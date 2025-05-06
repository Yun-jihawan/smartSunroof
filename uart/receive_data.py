import struct
import json
from logic.file_path import SETTING_FILE

def receiveData(ser):
    if ser.in_waiting > 0:
        data = ser.read(ser.in_waiting)
        
        if len(data) == 5:
            with open(SETTING_FILE, "r", encoding='utf-8') as SETTING:
                setting_data = json.load(SETTING)

            data = struct.unpack('BBBB', data)
            mapped_data ={
                "SUN" : data[1],
                "TP" : data[2],
                "AC" : data[3],
                "SM" : setting_data["SM"],
                "PS" : setting_data["PS"]
            }

            with open(SETTING_FILE, "w", encoding='utf-8') as f:
                json.dump(mapped_data, f, indent=4)

            return None

        elif len(data) == 9:
            data = struct.unpack('BBBBBBBBB', data)
            return data
    else:
        return None
    
# received
# [0 -> SUN, TP, AC, AQ] -> 주기(5sec)
# [1 -> TI, HI, TO, HO, AQI, AQO, PM, SP] -> 주기(?sec)

