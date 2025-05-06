import time
import struct

def sendData(ser, data):
    send = struct.pack('BBBBBB', *data)  # 바이트로 변환
    ser.write(send)   # 전송
    ser.flush()
    print(f"Sent Packet: {data}")
    time.sleep(0.01)  # 잠깐 대기