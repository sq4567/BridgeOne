using System.Diagnostics;
using System.Text;
using System.Text.Json;
using BridgeOne.Protocol;

namespace BridgeOne.Services;

/// <summary>
/// 핸드셰이크 서비스.
/// ESP32-S3와의 Authentication + State Sync 2단계 핸드셰이크를 수행합니다.
///
/// Phase 1: Authentication - 에코백 방식 인증
/// Phase 2: State Sync - 기능 협상 및 Keep-alive 주기 합의
/// </summary>
public sealed class HandshakeService
{
    /// <summary>인증 타임아웃 (1초)</summary>
    private static readonly TimeSpan AuthTimeout = TimeSpan.FromSeconds(1);

    /// <summary>State Sync 타임아웃 (1초)</summary>
    private static readonly TimeSpan SyncTimeout = TimeSpan.FromSeconds(1);

    /// <summary>핸드셰이크 최대 재시도 횟수</summary>
    private const int MaxRetries = 3;

    /// <summary>서버가 지원하는 전체 기능 목록</summary>
    private static readonly string[] ServerFeatures =
        ["wheel", "drag", "right_click", "multi_cursor", "macro", "extended_keyboard"];

    /// <summary>기본 Keep-alive 주기 (ms)</summary>
    private const int DefaultKeepaliveMs = 500;

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

    /// <summary>
    /// State Sync 단계를 수행합니다.
    ///
    /// 1. 서버가 지원하는 전체 기능 목록과 Keep-alive 주기를 전송
    /// 2. CMD_STATE_SYNC (0x03) 프레임 전송
    /// 3. 1초 타임아웃으로 CMD_STATE_SYNC_ACK (0x04) 대기
    /// 4. ESP32-S3가 수락한 기능 목록 저장
    /// </summary>
    /// <param name="cancellationToken">외부 취소 토큰</param>
    /// <returns>State Sync 결과</returns>
    public async Task<SyncResult> StateSyncAsync(CancellationToken cancellationToken = default)
    {
        // 1. STATE_SYNC JSON 조립 및 전송
        var syncJson = JsonSerializer.Serialize(new
        {
            command = "STATE_SYNC",
            features = ServerFeatures,
            keepalive_ms = DefaultKeepaliveMs
        });

        Debug.WriteLine($"[HandshakeService] STATE_SYNC 전송: {syncJson}");

        try
        {
            await _protocol.SendFrameAsync(
                (byte)VendorCdcCommand.StateSync,
                syncJson,
                cancellationToken);
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"[HandshakeService] STATE_SYNC 전송 실패: {ex.Message}");
            return SyncResult.Failed("STATE_SYNC 전송 실패");
        }

        // 2. STATE_SYNC_ACK 대기 (1초 타임아웃)
        using var timeoutCts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        timeoutCts.CancelAfter(SyncTimeout);

        try
        {
            while (await _protocol.FrameReader.WaitToReadAsync(timeoutCts.Token))
            {
                while (_protocol.FrameReader.TryRead(out var frame))
                {
                    // STATE_SYNC_ACK가 아닌 프레임은 무시
                    if (frame.Command != (byte)VendorCdcCommand.StateSyncAck)
                        continue;

                    // JSON 파싱
                    var payloadStr = Encoding.UTF8.GetString(frame.Payload);
                    Debug.WriteLine($"[HandshakeService] STATE_SYNC_ACK received: {payloadStr}");

                    JsonDocument? doc;
                    try
                    {
                        doc = JsonDocument.Parse(payloadStr);
                    }
                    catch (JsonException ex)
                    {
                        Debug.WriteLine(
                            $"[HandshakeService] STATE_SYNC_ACK JSON 파싱 실패: {ex.Message}");
                        return SyncResult.Failed("STATE_SYNC_ACK JSON 파싱 실패");
                    }

                    using (doc)
                    {
                        var root = doc.RootElement;

                        // accepted_features 배열 추출
                        var acceptedFeatures = new List<string>();
                        if (root.TryGetProperty("accepted_features", out var featuresProp) &&
                            featuresProp.ValueKind == JsonValueKind.Array)
                        {
                            foreach (var item in featuresProp.EnumerateArray())
                            {
                                var name = item.GetString();
                                if (name != null)
                                    acceptedFeatures.Add(name);
                            }
                        }

                        // mode 필드 추출
                        var mode = root.TryGetProperty("mode", out var modeProp)
                            ? modeProp.GetString() ?? "standard"
                            : "standard";

                        Debug.WriteLine(
                            $"[HandshakeService] State Sync 성공: accepted=[{string.Join(", ", acceptedFeatures)}], mode={mode}");

                        return SyncResult.Succeeded(acceptedFeatures.ToArray(), mode);
                    }
                }
            }
        }
        catch (OperationCanceledException) when (timeoutCts.IsCancellationRequested
                                                   && !cancellationToken.IsCancellationRequested)
        {
            Debug.WriteLine("[HandshakeService] STATE_SYNC_ACK 타임아웃 (1초)");
            return SyncResult.Failed("STATE_SYNC_ACK 타임아웃");
        }

        return SyncResult.Failed("STATE_SYNC_ACK를 수신하지 못함");
    }

    /// <summary>
    /// 전체 핸드셰이크(Auth + State Sync)를 수행합니다.
    /// 실패 시 최대 3회 재시도하며 지수 백오프(1초 → 2초 → 4초)를 적용합니다.
    /// </summary>
    /// <param name="cancellationToken">외부 취소 토큰</param>
    /// <returns>핸드셰이크 결과</returns>
    public async Task<HandshakeResult> PerformHandshakeAsync(CancellationToken cancellationToken = default)
    {
        for (int attempt = 1; attempt <= MaxRetries; attempt++)
        {
            Debug.WriteLine($"[HandshakeService] 핸드셰이크 시도 {attempt}/{MaxRetries}");

            // Phase 1: Authentication
            var authResult = await AuthenticateAsync(cancellationToken);
            if (!authResult.Success)
            {
                Debug.WriteLine(
                    $"[HandshakeService] 인증 실패 (시도 {attempt}): {authResult.ErrorMessage}");

                if (attempt < MaxRetries)
                {
                    var delay = TimeSpan.FromSeconds(Math.Pow(2, attempt - 1)); // 1s, 2s, 4s
                    Debug.WriteLine($"[HandshakeService] {delay.TotalSeconds}초 후 재시도...");
                    await Task.Delay(delay, cancellationToken);
                    continue;
                }

                return HandshakeResult.Failed(HandshakeFailReason.AuthFailed, authResult.ErrorMessage);
            }

            // Phase 2: State Sync
            var syncResult = await StateSyncAsync(cancellationToken);
            if (!syncResult.Success)
            {
                Debug.WriteLine(
                    $"[HandshakeService] State Sync 실패 (시도 {attempt}): {syncResult.ErrorMessage}");

                if (attempt < MaxRetries)
                {
                    var delay = TimeSpan.FromSeconds(Math.Pow(2, attempt - 1));
                    Debug.WriteLine($"[HandshakeService] {delay.TotalSeconds}초 후 재시도...");
                    await Task.Delay(delay, cancellationToken);
                    continue;
                }

                return HandshakeResult.Failed(HandshakeFailReason.SyncFailed, syncResult.ErrorMessage);
            }

            // 핸드셰이크 성공
            Debug.WriteLine(
                $"[HandshakeService] 핸드셰이크 완료 (시도 {attempt}): " +
                $"device={DeviceName}, features=[{string.Join(", ", syncResult.AcceptedFeatures)}]");

            return HandshakeResult.Connected(
                syncResult.AcceptedFeatures,
                syncResult.Mode,
                DeviceName,
                FirmwareVersion);
        }

        return HandshakeResult.Failed(HandshakeFailReason.MaxRetriesExceeded,
            $"최대 재시도 횟수({MaxRetries}) 초과");
    }
}

/// <summary>
/// 인증 결과를 나타내는 클래스.
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

/// <summary>
/// State Sync 결과를 나타내는 클래스.
/// </summary>
public sealed class SyncResult
{
    public bool Success { get; }
    public string? ErrorMessage { get; }
    public string[] AcceptedFeatures { get; }
    public string Mode { get; }

    private SyncResult(bool success, string? errorMessage, string[] acceptedFeatures, string mode)
    {
        Success = success;
        ErrorMessage = errorMessage;
        AcceptedFeatures = acceptedFeatures;
        Mode = mode;
    }

    public static SyncResult Succeeded(string[] acceptedFeatures, string mode)
        => new(true, null, acceptedFeatures, mode);

    public static SyncResult Failed(string errorMessage)
        => new(false, errorMessage, Array.Empty<string>(), "");
}

/// <summary>
/// 핸드셰이크 실패 사유.
/// </summary>
public enum HandshakeFailReason
{
    AuthFailed,
    SyncFailed,
    MaxRetriesExceeded,
}

/// <summary>
/// 전체 핸드셰이크(Auth + State Sync) 결과를 나타내는 클래스.
/// </summary>
public sealed class HandshakeResult
{
    public bool Success { get; }
    public HandshakeFailReason? FailReason { get; }
    public string? ErrorMessage { get; }
    public string[] AcceptedFeatures { get; }
    public string Mode { get; }
    public string? DeviceName { get; }
    public string? FirmwareVersion { get; }

    private HandshakeResult(
        bool success,
        HandshakeFailReason? failReason,
        string? errorMessage,
        string[] acceptedFeatures,
        string mode,
        string? deviceName,
        string? fwVersion)
    {
        Success = success;
        FailReason = failReason;
        ErrorMessage = errorMessage;
        AcceptedFeatures = acceptedFeatures;
        Mode = mode;
        DeviceName = deviceName;
        FirmwareVersion = fwVersion;
    }

    public static HandshakeResult Connected(
        string[] acceptedFeatures, string mode,
        string? deviceName, string? fwVersion)
        => new(true, null, null, acceptedFeatures, mode, deviceName, fwVersion);

    public static HandshakeResult Failed(HandshakeFailReason reason, string? errorMessage)
        => new(false, reason, errorMessage, Array.Empty<string>(), "", null, null);
}
