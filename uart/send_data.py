import time
import struct

def sendData(ser, data):
    send = struct.pack('BBBBBB', *data)  # 바이트로 변환
    ser.write(send)   # 전송
    ser.flush()
    print(f"Sent Packet: {data}")
    time.sleep(0.01)  # 잠깐 대기


# received
# [0 -> SUN, TP, AC, AQ] -> 주기(5sec)
# [1 -> TI, HI, TO, HO, AQI, AQO, PM, SP] -> 주기(?sec)

# send -> 둘다 비주기
# [0 -> SUN, TP, AC, SM, PS]
# [1 -> PSID, PS_TEMP, PS_AQ, PS_DD, PS_BR]