using System.Security.Cryptography;

namespace BridgeOne.Services;

/// <summary>
/// 에코백 방식 인증 검증기.
/// challenge를 그대로 돌려보내는 단순 방식입니다.
///
/// BridgeOne은 USB 물리 연결 기반이므로 원격 공격 벡터가 없어
/// 에코백 방식으로 충분합니다. 추후 보안 강화 시 HmacVerifier로 교체 가능합니다.
/// </summary>
public sealed class EchoBackVerifier : IAuthVerifier
{
    /// <summary>
    /// 16바이트(32자) 랜덤 hex 문자열을 생성합니다.
    /// </summary>
    public string GenerateChallenge()
    {
        var bytes = RandomNumberGenerator.GetBytes(16);
        return Convert.ToHexString(bytes).ToLowerInvariant();
    }

    /// <summary>
    /// 에코백 검증: response가 challenge와 동일한지 확인합니다.
    /// </summary>
    public bool VerifyResponse(string challenge, string response)
    {
        return string.Equals(challenge, response, StringComparison.Ordinal);
    }
}
