import struct
import json

def receiveData(ser):
    if ser.in_waiting > 0:
        data = ser.read(ser.in_waiting)
        
        if len(data) ==5:
            data = struct.unpack('BBBBB', data)
            mapped_data ={
                "DT": data[0],
                "SUNROOF" : data[1],
                "TRANSPANCY" : data[2],
                "AIRCON" : data[3],
                "AIRQUALITY" : data[4]
            }

            with open("receive_data_format.json", "w", encoding='utf-8') as f:
                json.dump(mapped_data, f, indent=4)

            return None

        elif len(data) == 8:
            data = struct.unpack('BBBBBBBB', data)
            return data
    else:
        return None