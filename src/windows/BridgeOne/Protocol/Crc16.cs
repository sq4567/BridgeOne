namespace BridgeOne.Protocol;

/// <summary>
/// CRC16-CCITT 체크섬 계산기.
/// ESP32-S3 측 vendor_cdc_crc16()과 동일한 알고리즘 (다항식 0x1021, 초기값 0x0000).
/// </summary>
public static class Crc16
{
    private const ushort Polynomial = 0x1021;
    private const ushort InitialValue = 0x0000;

    /// <summary>
    /// 지정된 바이트 범위에 대해 CRC16-CCITT를 계산합니다.
    /// </summary>
    public static ushort Calculate(byte[] data, int offset, int length)
    {
        ushort crc = InitialValue;

        for (int i = 0; i < length; i++)
        {
            crc ^= (ushort)(data[offset + i] << 8);
            for (int j = 0; j < 8; j++)
            {
                if ((crc & 0x8000) != 0)
                    crc = (ushort)((crc << 1) ^ Polynomial);
                else
                    crc <<= 1;
            }
        }

        return crc;
    }

    /// <summary>
    /// 전체 바이트 배열에 대해 CRC16-CCITT를 계산합니다.
    /// </summary>
    public static ushort Calculate(byte[] data)
        => Calculate(data, 0, data.Length);
}
