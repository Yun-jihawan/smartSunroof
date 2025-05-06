import serial

def initialize_serial(port='/dev/ttyAMA3', baudrate=115200, timeout=1):
    try:
        ser = serial.Serial(
            port=port,
            baudrate=baudrate,
            parity=serial.PARITY_NONE,
            stopbits=serial.STOPBITS_ONE,
            bytesize=serial.EIGHTBITS,
            timeout=timeout
        )
        return ser
    except serial.SerialException as e:
        print(f"Failed to initialize serial connection: {e}")
        return None