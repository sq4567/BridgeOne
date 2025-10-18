#!/usr/bin/env python3
"""
Phase 1.2.2.3 명령 처리 시스템 검증 스크립트
ESP32-S3 보드에 JSON 명령을 전송하고 응답을 검증합니다.

작성일: 2025-01-18
작성자: BridgeOne Development Team
"""

import serial
import json
import struct
import time
import sys

# ============================================================================
# CRC16-CCITT 계산 (ESP32-S3와 동일한 알고리즘)
# ============================================================================

def calculate_crc16(data):
    """
    CRC16-CCITT 체크섬 계산
    ESP32-S3의 calculateCRC16() 함수와 동일한 알고리즘
    
    Args:
        data: 계산할 데이터 (bytes)
    
    Returns:
        uint16_t CRC16 체크섬
    """
    crc = 0xFFFF
    for byte in data:
        crc ^= (byte << 8)
        for _ in range(8):
            if crc & 0x8000:
                crc = (crc << 1) ^ 0x1021
            else:
                crc <<= 1
            crc &= 0xFFFF
    return crc

# ============================================================================
# JSON 메시지 프레임 생성
# ============================================================================

def create_json_frame(json_obj):
    """
    JSON 객체를 프레임으로 직렬화
    
    프레임 형식: [0xFF][길이 2바이트][JSON payload][CRC16 2바이트]
    
    Args:
        json_obj: 직렬화할 JSON 객체 (dict)
    
    Returns:
        bytes: 완성된 프레임
    """
    json_str = json.dumps(json_obj)
    json_bytes = json_str.encode('utf-8')
    
    # 헤더
    header = 0xFF
    length = len(json_bytes)
    
    # 프레임 데이터 (헤더 + 길이 + payload)
    frame_data = struct.pack('<BH', header, length) + json_bytes
    
    # CRC16 계산
    crc16 = calculate_crc16(frame_data)
    
    # 완성된 프레임
    frame = frame_data + struct.pack('<H', crc16)
    
    return frame

# ============================================================================
# JSON 메시지 프레임 파싱
# ============================================================================

def parse_json_frame(ser, timeout=2.0):
    """
    ESP32-S3로부터 JSON 응답 프레임 수신 및 파싱
    
    Args:
        ser: Serial 포트 객체
        timeout: 타임아웃 시간 (초)
    
    Returns:
        dict: 파싱된 JSON 객체 또는 None
    """
    start_time = time.time()
    
    # 헤더 대기 (0xFF)
    while True:
        if time.time() - start_time > timeout:
            print("❌ 타임아웃: 헤더를 찾을 수 없습니다.")
            return None
        
        byte = ser.read(1)
        if not byte:
            continue
        
        if byte[0] == 0xFF:
            print(f"✓ 헤더 수신: 0x{byte[0]:02X}")
            break
    
    # 길이 읽기 (2바이트, Little-Endian)
    length_bytes = ser.read(2)
    if len(length_bytes) != 2:
        print("❌ 길이 필드 읽기 실패")
        return None
    
    length = struct.unpack('<H', length_bytes)[0]
    print(f"✓ Payload 길이: {length} bytes")
    
    # Payload 읽기
    payload = ser.read(length)
    if len(payload) != length:
        print(f"❌ Payload 읽기 실패 ({len(payload)}/{length} bytes)")
        return None
    
    print(f"✓ Payload 수신: {length} bytes")
    
    # CRC16 읽기
    crc_bytes = ser.read(2)
    if len(crc_bytes) != 2:
        print("❌ CRC16 읽기 실패")
        return None
    
    received_crc = struct.unpack('<H', crc_bytes)[0]
    print(f"✓ CRC16 수신: 0x{received_crc:04X}")
    
    # CRC16 검증
    frame_data = struct.pack('<BH', 0xFF, length) + payload
    calculated_crc = calculate_crc16(frame_data)
    
    # 디버그: Raw 데이터 출력
    print(f"[DEBUG] Header: 0xFF")
    print(f"[DEBUG] Length: {length} (0x{length:04X})")
    print(f"[DEBUG] Length bytes: {length_bytes.hex()}")
    print(f"[DEBUG] Payload (first 20 bytes): {payload[:20].hex()}")
    print(f"[DEBUG] Frame data for CRC (first 25 bytes): {frame_data[:25].hex()}")
    print(f"[DEBUG] Calculated CRC: 0x{calculated_crc:04X}")
    print(f"[DEBUG] Received CRC: 0x{received_crc:04X}")
    
    if received_crc != calculated_crc:
        print(f"❌ CRC16 불일치! (수신: 0x{received_crc:04X}, 계산: 0x{calculated_crc:04X})")
        print(f"[DEBUG] 전체 프레임 (hex): {(struct.pack('<BH', 0xFF, length) + payload + crc_bytes).hex()}")
        return None
    
    print(f"✓ CRC16 검증 통과")
    
    # JSON 파싱
    try:
        json_obj = json.loads(payload.decode('utf-8'))
        print(f"✓ JSON 내용: {json.dumps(json_obj, indent=2)}")
        return json_obj
    except Exception as e:
        print(f"❌ JSON 파싱 실패: {e}")
        print(f"   Raw payload: {payload.decode('utf-8', errors='replace')}")
        return None

# ============================================================================
# 테스트 함수들
# ============================================================================

def test_ping(ser, message_id):
    """CMD_PING (0x01) 테스트"""
    print("\n" + "="*60)
    print("📡 TEST 1: CMD_PING (0x01) - 연결 확인")
    print("="*60)
    
    request = {
        "cmd": 0x01,
        "id": message_id
    }
    
    print(f"\n📤 요청 전송: {json.dumps(request)}")
    frame = create_json_frame(request)
    ser.write(frame)
    print(f"✓ 프레임 전송 완료 ({len(frame)} bytes)")
    
    print("\n📥 응답 대기 중...")
    response = parse_json_frame(ser)
    
    if response:
        print(f"\n✅ 응답 수신 성공!")
        print(f"   - status: {response.get('status')}")
        print(f"   - cmd: {response.get('cmd')}")
        print(f"   - id: {response.get('id')}")
        print(f"   - timestamp: {response.get('timestamp')}")
        
        # 검증
        if (response.get('status') == 'ok' and 
            response.get('cmd') == 0x01 and 
            response.get('id') == message_id):
            print("\n✅ CMD_PING 테스트 통과!")
            return True
        else:
            print("\n❌ 응답 데이터 불일치")
            return False
    else:
        print("\n❌ 응답 수신 실패")
        return False

def test_get_status(ser, message_id):
    """CMD_GET_STATUS (0x02) 테스트"""
    print("\n" + "="*60)
    print("📊 TEST 2: CMD_GET_STATUS (0x02) - 시스템 상태 조회")
    print("="*60)
    
    request = {
        "cmd": 0x02,
        "id": message_id
    }
    
    print(f"\n📤 요청 전송: {json.dumps(request)}")
    frame = create_json_frame(request)
    ser.write(frame)
    print(f"✓ 프레임 전송 완료 ({len(frame)} bytes)")
    
    print("\n📥 응답 대기 중...")
    response = parse_json_frame(ser, timeout=3.0)
    
    if response:
        print(f"\n✅ 응답 수신 성공!")
        print(f"   - status: {response.get('status')}")
        print(f"   - cmd: {response.get('cmd')}")
        print(f"   - id: {response.get('id')}")
        
        data = response.get('data', {})
        print(f"\n📊 시스템 상태 데이터:")
        print(f"   - fps: {data.get('fps')} Hz")
        print(f"   - queueSize: {data.get('queueSize')}")
        print(f"   - uptime: {data.get('uptime')} ms")
        print(f"   - frameCount: {data.get('frameCount')}")
        print(f"   - lostFrames: {data.get('lostFrames')}")
        print(f"   - hidTxCount: {data.get('hidTxCount')}")
        print(f"   - freeHeap: {data.get('freeHeap')} bytes")
        
        # 검증
        if (response.get('status') == 'ok' and 
            response.get('cmd') == 0x02 and 
            response.get('id') == message_id and
            'data' in response):
            print("\n✅ CMD_GET_STATUS 테스트 통과!")
            return True
        else:
            print("\n❌ 응답 데이터 불일치")
            return False
    else:
        print("\n❌ 응답 수신 실패")
        return False

def test_set_config(ser, message_id):
    """CMD_SET_CONFIG (0x03) 테스트"""
    print("\n" + "="*60)
    print("⚙️  TEST 3: CMD_SET_CONFIG (0x03) - 설정 변경")
    print("="*60)
    
    request = {
        "cmd": 0x03,
        "id": message_id,
        "config": {
            "debugMode": True,
            "logLevel": 2
        }
    }
    
    print(f"\n📤 요청 전송: {json.dumps(request, indent=2)}")
    frame = create_json_frame(request)
    ser.write(frame)
    print(f"✓ 프레임 전송 완료 ({len(frame)} bytes)")
    
    print("\n📥 응답 대기 중...")
    response = parse_json_frame(ser, timeout=3.0)
    
    if response:
        print(f"\n✅ 응답 수신 성공!")
        print(f"   - status: {response.get('status')}")
        print(f"   - cmd: {response.get('cmd')}")
        print(f"   - id: {response.get('id')}")
        print(f"   - message: {response.get('message')}")
        
        config = response.get('config', {})
        print(f"\n⚙️  적용된 설정:")
        print(f"   - debugMode: {config.get('debugMode')}")
        print(f"   - logLevel: {config.get('logLevel')}")
        
        # 검증
        if (response.get('status') == 'ok' and 
            response.get('cmd') == 0x03 and 
            response.get('id') == message_id):
            print("\n✅ CMD_SET_CONFIG 테스트 통과!")
            return True
        else:
            print("\n❌ 응답 데이터 불일치")
            return False
    else:
        print("\n❌ 응답 수신 실패")
        return False

# ============================================================================
# 메인 테스트 실행
# ============================================================================

def main():
    """메인 테스트 실행"""
    print("\n" + "="*60)
    print("🚀 Phase 1.2.2.3 명령 처리 시스템 검증 시작")
    print("="*60)
    
    # COM 포트 설정 (명령줄 인자 또는 기본값)
    if len(sys.argv) > 1:
        COM_PORT = sys.argv[1]
    else:
        COM_PORT = "COM14"  # ⚠️ 실제 포트로 변경 필요!
    
    BAUD_RATE = 115200
    
    print(f"\n🔌 시리얼 포트 연결 중...")
    print(f"   - 포트: {COM_PORT}")
    print(f"   - Baud rate: {BAUD_RATE}")
    print(f"\n💡 다른 포트 사용: python test_command_system.py COM3")
    
    try:
        ser = serial.Serial(COM_PORT, BAUD_RATE, timeout=2)
        time.sleep(2)  # 보드 초기화 대기
        print(f"✓ 연결 성공!")
        
        # 기존 버퍼 클리어
        ser.reset_input_buffer()
        ser.reset_output_buffer()
        
        # 테스트 실행
        results = []
        message_id = 1000
        
        results.append(("CMD_PING", test_ping(ser, message_id)))
        message_id += 1
        time.sleep(0.5)
        
        results.append(("CMD_GET_STATUS", test_get_status(ser, message_id)))
        message_id += 1
        time.sleep(0.5)
        
        results.append(("CMD_SET_CONFIG", test_set_config(ser, message_id)))
        
        # 최종 결과
        print("\n" + "="*60)
        print("📊 최종 검증 결과")
        print("="*60)
        
        for test_name, result in results:
            status = "✅ 통과" if result else "❌ 실패"
            print(f"{status} - {test_name}")
        
        all_passed = all(result for _, result in results)
        
        print("\n" + "="*60)
        if all_passed:
            print("🎉 모든 테스트 통과! Phase 1.2.2.3 구현 완료 검증 성공!")
            print("\n📝 다음 단계:")
            print("   1. development-plan-checklist.md의 Python 테스트 항목 체크")
            print("   2. Phase 1.2.3: Windows 기본 장치 인식 구현 시작")
        else:
            print("⚠️  일부 테스트 실패. 로그를 확인하세요.")
            print("\n💡 문제 해결:")
            print("   1. ESP32-S3 보드가 연결되어 있는지 확인")
            print("   2. Serial Monitor가 닫혀 있는지 확인")
            print("   3. COM 포트 번호가 올바른지 확인")
        print("="*60 + "\n")
        
        ser.close()
        
        # 종료 코드 반환
        sys.exit(0 if all_passed else 1)
        
    except serial.SerialException as e:
        print(f"\n❌ 시리얼 포트 연결 실패: {e}")
        print(f"\n💡 확인 사항:")
        print(f"   1. ESP32-S3 보드가 USB로 연결되어 있는지 확인")
        print(f"   2. COM 포트 번호가 올바른지 확인 (장치 관리자)")
        print(f"   3. 다른 프로그램(Serial Monitor 등)이 포트를 사용 중이지 않은지 확인")
        print(f"   4. pyserial 설치: pip install pyserial")
        sys.exit(1)
    except KeyboardInterrupt:
        print("\n\n⚠️  사용자에 의해 중단되었습니다.")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ 예상치 못한 오류: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()

