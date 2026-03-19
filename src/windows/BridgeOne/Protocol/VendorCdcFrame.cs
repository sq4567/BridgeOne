using System.Buffers.Binary;

namespace BridgeOne.Protocol;

/// <summary>
/// Vendor CDC 명령 코드. ESP32-S3 vendor_cdc_cmd_t와 동일.
/// </summary>
public enum VendorCdcCommand : byte
{
    AuthChallenge = 0x01,
    AuthResponse  = 0x02,
    StateSync     = 0x03,
    StateSyncAck  = 0x04,
    Ping          = 0x10,
    Pong          = 0x11,
    ModeNotify    = 0x20,
    Error         = 0xFE,
}

/// <summary>
/// Vendor CDC 바이너리 프레임.
/// 구조: [0xFF] [command 1B] [length_LE 2B] [payload 0~448B] [crc16_LE 2B]
/// CRC16은 payload만 대상으로 계산합니다.
/// </summary>
public sealed class VendorCdcFrame
{
    /// <summary>프레임 헤더 마커</summary>
    public const byte Header = 0xFF;

    /// <summary>헤더(1) + 명령(1) + 길이(2) + CRC(2)</summary>
    public const int OverheadSize = 6;

    /// <summary>최대 페이로드 크기 (ESP32-S3과 동일)</summary>
    public const int MaxPayloadSize = 448;

    /// <summary>최대 프레임 크기</summary>
    public const int MaxFrameSize = OverheadSize + MaxPayloadSize;

    /// <summary>명령 코드</summary>
    public byte Command { get; }

    /// <summary>페이로드 데이터 (빈 배열일 수 있음)</summary>
    public byte[] Payload { get; }

    /// <summary>CRC 검증 통과 여부 (Parse로 생성된 경우에만 의미 있음)</summary>
    public bool IsValid { get; }

    public VendorCdcFrame(byte command, byte[] payload, bool isValid = true)
    {
        Command = command;
        Payload = payload;
        IsValid = isValid;
    }

    /// <summary>
    /// 프레임을 바이트 배열로 직렬화합니다.
    /// CRC16은 payload만 대상으로 계산하여 Little-Endian으로 부착합니다.
    /// </summary>
    public byte[] ToBytes()
    {
        int frameSize = OverheadSize + Payload.Length;
        var buffer = new byte[frameSize];

        buffer[0] = Header;
        buffer[1] = Command;
        BinaryPrimitives.WriteUInt16LittleEndian(buffer.AsSpan(2), (ushort)Payload.Length);

        if (Payload.Length > 0)
            Payload.CopyTo(buffer.AsSpan(4));

        ushort crc = Crc16.Calculate(Payload);
        BinaryPrimitives.WriteUInt16LittleEndian(buffer.AsSpan(4 + Payload.Length), crc);

        return buffer;
    }

    /// <summary>
    /// 완전한 프레임 바이트 배열에서 VendorCdcFrame을 파싱합니다.
    /// 헤더(0xFF)부터 CRC까지 포함된 전체 프레임이어야 합니다.
    /// </summary>
    /// <returns>파싱된 프레임 (CRC 오류 시 IsValid=false), 구조 오류 시 null</returns>
    public static VendorCdcFrame? Parse(byte[] data, int offset = 0)
    {
        int available = data.Length - offset;

        // 최소 크기 검증
        if (available < OverheadSize)
            return null;

        // 헤더 검증
        if (data[offset] != Header)
            return null;

        byte command = data[offset + 1];
        ushort payloadLen = BinaryPrimitives.ReadUInt16LittleEndian(data.AsSpan(offset + 2));

        // 페이로드 길이 검증
        if (payloadLen > MaxPayloadSize)
            return null;

        int expectedSize = OverheadSize + payloadLen;
        if (available < expectedSize)
            return null;

        // 페이로드 추출
        var payload = new byte[payloadLen];
        if (payloadLen > 0)
            Array.Copy(data, offset + 4, payload, 0, payloadLen);

        // CRC 검증
        ushort receivedCrc = BinaryPrimitives.ReadUInt16LittleEndian(
            data.AsSpan(offset + 4 + payloadLen));
        ushort calculatedCrc = Crc16.Calculate(payload);
        bool isValid = receivedCrc == calculatedCrc;

        return new VendorCdcFrame(command, payload, isValid);
    }

    /// <summary>
    /// 프레임의 전체 바이트 크기를 반환합니다.
    /// </summary>
    public int FrameSize => OverheadSize + Payload.Length;
}
