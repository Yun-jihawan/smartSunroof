import json
import os
from config.file_path import WEATHER_FILE

class WeatherManager:
    def __init__(self):
       self.filepath = WEATHER_FILE
       self.weather_data = self._load_weather()
       
    def _load_weather(self):
        if not os.path.exists(self.filepath):
            return self.default_weather()

        with open(self.filepath, 'r', encoding='utf-8') as f:
            return json.load(f)

    def save_weather(self):
        with open(self.filepath, 'w', encoding='utf-8') as f:
            json.dump(self.weather_data, f, indent=4)

    def default_weather(self):
        return {
            "TI": -1,
            "HI": -1,
            "TO": -1,
            "HO": -1,
            "AQI": -1,
            "AQO": -1,
            "DD": -1
        }
    
    def get(self, key, default=-1):
        try:
            return int(self.weather_data[key])
        except:
            return default
        
    def set(self, key, value):
        try:
            for k in self.weather_data:
                if k == key:
                    self.weather_data[k] = value
                    self.save_weather()
        except:
            print('[WM] key value error')
    
    def update_by_uart(self, packet):
        # 예: b'\x01\x1a' → 온도: 1, 습도: 26
        try:
            print(f'[WM - PACK] : {packet}')
            self.set("TI", packet[0])
            self.set("HI", packet[1])
            self.set("TO", packet[2])
            self.set("HO", packet[3])
            self.set("AQI", packet[4])
            self.set("AQO", packet[5])
            self.set("DD", packet[6])
            print(f'[WM - JSON] : {self.weather_data}')

        except (IndexError, ValueError):
            print("Invalid weather data received")