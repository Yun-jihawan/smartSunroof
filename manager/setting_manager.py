import json
import os
from config.file_path import SETTING_FILE

class SettingManager:
    def __init__(self):
        self.filepath = SETTING_FILE
        self.setting_data = self._load_settings()
        self.velocity = 0
        
    def _load_settings(self):
        if not os.path.exists(self.filepath):
            return self.default_settings()
        
        with open(self.filepath, 'r', encoding='utf-8') as f:
            return json.load(f)

    def save_settings(self):
        with open(self.filepath, 'w', encoding='utf-8') as f:
            json.dump(self.setting_data, f, indent=4)

    def default_settings(self):
        return {
            "SUN": 0,
            "TP": 0,
            "AC": 0,
            "SM": 0,
            "PS": 1
        }
    
    def get(self, key, default=None):
        try:
            return int(self.setting_data[key])
        except:
            return default
        
    def set(self, key, value):
        try:
            for k in self.setting_data:
                if k == key:
                    self.setting_data[k] = value
                    self.save_settings()
        except:
            print('[SM] key value error')

    def update_by_uart(self, packet):
        print(f'[SM - PACK] : {packet}')
        self.set("SUN", packet[0])
        self.set("TP", packet[1])
        self.set("AC", packet[2])
        self.set("SM", packet[3])
        self.velocity = packet[4]
        print(f'[SM - JSON] : {self.setting_data}')