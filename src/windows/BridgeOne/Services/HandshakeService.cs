using System.Diagnostics;
using System.Text;
using System.Text.Json;
using BridgeOne.Protocol;

namespace BridgeOne.Services;

/// <summary>
/// 핸드셰이크 서비스.
/// ESP32-S3와의 Authentication + State Sync 2단계 핸드셰이크를 수행합니다.
///
/// Phase 3.3.2: Authentication (이 단계)
/// - 16바이트 랜덤 hex 챌린지 전송 → 에코백 응답 검증
/// - 디바이스 정보(device, fw_version) 저장
///
/// Phase 3.3.3: State Sync (추후 구현)
/// - StateSyncAsync 메서드 추가 예정
/// </summary>
public sealed class HandshakeService
{
    /// <summary>인증 타임아웃 (1초)</summary>
    private static readonly TimeSpan AuthTimeout = TimeSpan.FromSeconds(1);

    private readonly VendorCdcProtocol _protocol;
    private readonly IAuthVerifier _authVerifier;

    /// <summary>인증 성공 시 ESP32-S3가 보고한 디바이스 이름</summary>
    public string? DeviceName { get; private set; }

    /// <summary>인증 성공 시 ESP32-S3가 보고한 펌웨어 버전</summary>
    public string? FirmwareVersion { get; private set; }

    public HandshakeService(VendorCdcProtocol protocol, IAuthVerifier? authVerifier = null)
    {
        _protocol = protocol;
        _authVerifier = authVerifier ?? new EchoBackVerifier();
    }

    /// <summary>
    /// Authentication 단계를 수행합니다.
    ///
    /// 1. 랜덤 챌린지 생성
    /// 2. CMD_AUTH_CHALLENGE (0x01) 프레임 전송
    /// 3. 1초 타임아웃으로 CMD_AUTH_RESPONSE (0x02) 대기
    /// 4. 응답의 response 필드가 challenge와 일치하는지 검증
    /// 5. 디바이스 정보 저장
    /// </summary>
    /// <param name="cancellationToken">외부 취소 토큰</param>
    /// <returns>인증 결과</returns>
    public async Task<AuthResult> AuthenticateAsync(CancellationToken cancellationToken = default)
    {
        // 1. 챌린지 생성
        var challenge = _authVerifier.GenerateChallenge();
        Debug.WriteLine($"[HandshakeService] Challenge generated: {challenge}");

        // 2. AUTH_CHALLENGE JSON 조립 및 전송
        var challengeJson = JsonSerializer.Serialize(new
        {
            command = "AUTH_CHALLENGE",
            challenge,
            version = "1.0"
        });

        try
        {
            await _protocol.SendFrameAsync(
                (byte)VendorCdcCommand.AuthChallenge,
                challengeJson,
                cancellationToken);
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"[HandshakeService] AUTH_CHALLENGE 전송 실패: {ex.Message}");
            return AuthResult.Failed("AUTH_CHALLENGE 전송 실패");
        }

        // 3. AUTH_RESPONSE 대기 (1초 타임아웃)
        using var timeoutCts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        timeoutCts.CancelAfter(AuthTimeout);

        try
        {
            while (await _protocol.FrameReader.WaitToReadAsync(timeoutCts.Token))
            {
                while (_protocol.FrameReader.TryRead(out var frame))
                {
                    // AUTH_RESPONSE가 아닌 프레임은 무시
                    if (frame.Command != (byte)VendorCdcCommand.AuthResponse)
                        continue;

                    // JSON 파싱
                    var payloadStr = Encoding.UTF8.GetString(frame.Payload);
                    Debug.WriteLine($"[HandshakeService] AUTH_RESPONSE received: {payloadStr}");

                    JsonDocument? doc;
                    try
                    {
                        doc = JsonDocument.Parse(payloadStr);
                    }
                    catch (JsonException ex)
                    {
                        Debug.WriteLine(
                            $"[HandshakeService] AUTH_RESPONSE JSON 파싱 실패: {ex.Message}");
                        return AuthResult.Failed("AUTH_RESPONSE JSON 파싱 실패");
                    }

                    using (doc)
                    {
                        var root = doc.RootElement;

                        // response 필드 추출 및 검증
                        if (!root.TryGetProperty("response", out var responseProp))
                            return AuthResult.Failed("AUTH_RESPONSE에 'response' 필드 없음");

                        var response = responseProp.GetString();
                        if (response == null)
                            return AuthResult.Failed("AUTH_RESPONSE의 'response' 필드가 null");

                        if (!_authVerifier.VerifyResponse(challenge, response))
                        {
                            Debug.WriteLine(
                                $"[HandshakeService] 인증 실패: challenge={challenge}, response={response}");
                            return AuthResult.Failed("챌린지 응답 불일치");
                        }

                        // 디바이스 정보 저장
                        DeviceName = root.TryGetProperty("device", out var deviceProp)
                            ? deviceProp.GetString()
                            : null;
                        FirmwareVersion = root.TryGetProperty("fw_version", out var fwProp)
                            ? fwProp.GetString()
                            : null;

                        Debug.WriteLine(
                            $"[HandshakeService] 인증 성공: device={DeviceName}, fw={FirmwareVersion}");

                        return AuthResult.Succeeded(DeviceName, FirmwareVersion);
                    }
                }
            }
        }
        catch (OperationCanceledException) when (timeoutCts.IsCancellationRequested
                                                   && !cancellationToken.IsCancellationRequested)
        {
            Debug.WriteLine("[HandshakeService] AUTH_RESPONSE 타임아웃 (1초)");
            return AuthResult.Failed("AUTH_RESPONSE 타임아웃");
        }

        return AuthResult.Failed("AUTH_RESPONSE를 수신하지 못함");
    }
}

/// <summary>
/// 인증 결과를 나타내는 레코드.
/// </summary>
public sealed class AuthResult
{
    public bool Success { get; }
    public string? ErrorMessage { get; }
    public string? DeviceName { get; }
    public string? FirmwareVersion { get; }

    private AuthResult(bool success, string? errorMessage, string? deviceName, string? fwVersion)
    {
        Success = success;
        ErrorMessage = errorMessage;
        DeviceName = deviceName;
        FirmwareVersion = fwVersion;
    }

    public static AuthResult Succeeded(string? deviceName, string? fwVersion)
        => new(true, null, deviceName, fwVersion);

    public static AuthResult Failed(string errorMessage)
        => new(false, errorMessage, null, null);
}
