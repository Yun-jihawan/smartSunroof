import time
from send_data import sendData
from receive_data import receiveData
from serial_setup import initialize_serial

ser = initialize_serial()

if __name__ == "__main__":
    
    data = [10,20,30,40,50,60]
    count =0
    while(True):
        if(ser.in_waiting > 0):
            recData = receiveData(ser)
            print(f"Received Data: {recData}")
        else:
            if(count %1000 == 0):
               sendData(ser, data)
               count = 0
            count += 1