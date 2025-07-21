import json
import serial
import time

from config.file_path import SETTING_FILE, PRESET_FILE

class UartSender:
    def __init__(self, port='/dev/ttyAMA3', baudrate=115200):
        self.ser = serial.Serial(port, baudrate, timeout=1)

    def _load_json(self, filepath):
        with open(filepath, 'r', encoding='utf-8') as f:
            return json.load(f)
        
    def send_setting(self):
        data = self._load_json(SETTING_FILE)
        payload = bytearray([0xFF])
        payload += bytes([
            data.get("SUN", 0),
            data.get("TP", 0),
            data.get("AC", 0),
            data.get("SM", 0)
        ])
        # payload += b'\x00' * (8 - len(payload))
        self._send(payload)

    def send_preset(self, ps_id=1):
        data = self._load_json(PRESET_FILE)
        preset = next((item for item in data if item["id"] == ps_id))
        
        payload = bytearray([0xFE])
        payload += bytes([
            preset.get("PS_TEMP", 16),
            preset.get("PS_AQL", 0),
            preset.get("PS_DDL", 0),
            preset.get("PS_BRL", 0)
        ])
        self._send(payload)

    def _send(self, payload):
        if self.ser.is_open:
            self.ser.write(payload)
            self.ser.flush()
            print(f"[UART-SEND] : {payload}")
            time.sleep(0.01)
        else:
            print(f"[UART-SEND] : Port is not Connected")

    def close(self):
        if self.ser.is_open:
            self.ser.close()