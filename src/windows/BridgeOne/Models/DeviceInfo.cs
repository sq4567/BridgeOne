namespace BridgeOne.Models;

/// <summary>
/// ESP32-S3 BridgeOne 동글의 USB 장치 정보
/// </summary>
public sealed class DeviceInfo
{
    /// <summary>USB Vendor ID (Espressif: 0x303A)</summary>
    public ushort Vid { get; init; }

    /// <summary>USB Product ID (BridgeOne: 0x4001)</summary>
    public ushort Pid { get; init; }

    /// <summary>CDC 가상 시리얼 포트 이름 (예: "COM3")</summary>
    public string ComPort { get; init; } = string.Empty;

    /// <summary>USB 장치 설명 (예: "BridgeOne USB Bridge")</summary>
    public string Description { get; init; } = string.Empty;

    /// <summary>USB 시리얼 번호</summary>
    public string SerialNumber { get; init; } = string.Empty;

    /// <summary>WMI PnP Device ID (핫플러그 식별용)</summary>
    public string PnpDeviceId { get; init; } = string.Empty;

    public override string ToString() =>
        $"BridgeOne [{ComPort}] (VID=0x{Vid:X4}, PID=0x{Pid:X4})";
}
