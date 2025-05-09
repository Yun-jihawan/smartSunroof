import serial
import threading
import time

from manager.setting_manager import SettingManager
from manager.weather_manager import WeatherManager

class UartReceiver(threading.Thread):
    def __init__(self, port='/dev/ttyAMA3', baudrate=115200):
        super().__init__()
        self.ser = serial.Serial(port, baudrate, timeout=1)
        self.running = True
        self.buffer = bytearray()
        self.setting_manager = SettingManager()
        self.weather_manager = WeatherManager()


    def run(self):
        while self.running:
            if self.ser.in_waiting:
                self.buffer.extend(self.ser.read(self.ser.in_waiting))
                self._parse_buffer()
                time.sleep(0.01)

    def _parse_buffer(self):
        while len(self.buffer) >= 8:
            packet = self.buffer[:8]
            self.buffer = self.buffer[8:]
            
            packet_type = packet[0]

            if packet_type == 0xFF: # setting data
                setting_data = packet[1:6]
                #print(f'[SET DATA]: {setting_data}')
                self._handle_setting(setting_data)
            elif packet_type == 0xFE:
                weather_data = packet[1:8]
                #print(f'[WEA DATA] : {weather_data}')
                self._handle_weather(weather_data)
            else:
                print(f"PACKET ERROR : {packet_type}")
            
    def _handle_weather(self, packet):
        try:
            parsed = list(packet)
            self.weather_manager.update_by_uart(parsed)
        except Exception as e:
            print(f"[UART] Weather Parse Error: {e}")

    def _handle_setting(self, packet):
        try:
            parsed = list(packet)
            self.setting_manager.update_by_uart(parsed)
        except Exception as e:
            print(f"[UART] Setting Parse Error: {e}")

    def stop(self):
        self.running = False
        if self.ser.is_open:
            self.ser.close()
