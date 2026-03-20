namespace BridgeOne.Services;

/// <summary>
/// 인증 검증 인터페이스.
/// 핸드셰이크 Phase 1(Authentication)에서 challenge-response 방식의
/// 인증 로직을 추상화합니다.
///
/// 현재 구현: EchoBackVerifier (challenge == response)
/// 추후 구현: HmacVerifier (HMAC-SHA256 기반)
/// </summary>
public interface IAuthVerifier
{
    /// <summary>
    /// 랜덤 챌린지 문자열을 생성합니다.
    /// </summary>
    /// <returns>16바이트 랜덤 hex 문자열</returns>
    string GenerateChallenge();

    /// <summary>
    /// ESP32-S3의 응답이 챌린지에 대한 올바른 답인지 검증합니다.
    /// </summary>
    /// <param name="challenge">서버가 전송한 챌린지</param>
    /// <param name="response">ESP32-S3가 반환한 응답</param>
    /// <returns>true: 인증 성공, false: 인증 실패</returns>
    bool VerifyResponse(string challenge, string response);
}
