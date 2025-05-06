import tkinter as tk
from tkinter import ttk
import json

from logic.file_path import SETTING_FILE, PRESET_FILE
from uart.serial_setup import initialize_serial
from uart.send_data import sendData

class SettingFrame(ttk.Frame):
    def __init__(self, master, go_back_callback):
        super().__init__(master, padding=10)
        self.go_back = go_back_callback

        self.SETTING_FILE = SETTING_FILE
        self.PRESET_FILE = PRESET_FILE

        self.setting_data = self.load_json(self.SETTING_FILE)
        self.preset_data = self.load_json(self.PRESET_FILE)
        self.ser = initialize_serial()

        self.init_var()
        self.create_widgets()
        self.apply_var()
        
    def init_var(self):
        self.temp_var = tk.DoubleVar()
        self.aq_var = tk.IntVar()
        self.dust_var = tk.IntVar()
        self.bright_var = tk.IntVar()

    def apply_var(self):
        preset_id = self.setting_data["PS"]

        for entry in self.preset_data:
            if entry["id"] == preset_id:
                self.temp_var.set(entry["PS_TEMP"])
                self.aq_var.set(entry["PS_AQL"])
                self.dust_var.set(entry["PS_DDL"])
                self.bright_var.set(entry["PS_BRL"])
                break
        
    def get_default_data():
        return [
        {"id": 1, "PS_TEMP": 16.0, "PS_AQL": 0, "PS_DDL": 0, "PS_BRL": 0},
        {"id": 2, "PS_TEMP": 16.0, "PS_AQL": 0, "PS_DDL": 0, "PS_BRL": 0},
        {"id": 3, "PS_TEMP": 16.0, "PS_AQL": 0, "PS_DDL": 0, "PS_BRL": 0}
        ]

    def load_json(self, FILE_PATH):
        with open(FILE_PATH, "r", encoding="utf-8") as f:
            return json.load(f)
            
    def save_json(self, FILE_PATH, data):
        with open(FILE_PATH, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=4)
        
    def create_widgets(self):
        ttk.Label(self, text="PRESET SETTING", font=("Arial", 16)).pack(pady=10)
        preset_frame = ttk.Frame(self)
        preset_frame.pack()

        # preset 선택
        ps_id = self.setting_data["PS"]
        self.id_var = tk.IntVar(value=ps_id)
        id_frame = ttk.Frame(preset_frame)
        id_frame.pack(pady=5)
        for i in range(1, 4):
            ttk.Radiobutton(id_frame, text=i, variable=self.id_var, value=i, command=self.update_entries_from_id).pack(side="left", padx=10)

        # TEMPERATURE SETTING
        temp_frame = ttk.Frame(preset_frame)
        temp_frame.pack(pady=5)
        
        ttk.Button(temp_frame, text="▲", command=self.increase_temp).pack(side="left", padx=5)
        ttk.Label(temp_frame, textvariable=self.temp_var, width=5, relief="sunken", anchor="center").pack(side="left", padx=5)
        ttk.Button(temp_frame, text="▼", command=self.decrease_temp).pack(side="left", padx=5)

        # AQL
        ttk.Label(preset_frame, text="AIR QUALITY LEVEL").pack(pady=(5,0))
        self.aq_slider = tk.Scale(preset_frame, from_=0, to=3, variable=self.aq_var, resolution=1, orient="horizontal", tickinterval=1, length=300)
        self.aq_slider.pack()

        # DDL
        ttk.Label(preset_frame, text="DUST DENSITY LEVEL").pack(pady=(5,0))
        self.dust_slider = tk.Scale(preset_frame, from_=0, to=3, variable=self.dust_var, resolution=1, orient="horizontal", tickinterval=1, length=300)
        self.dust_slider.pack()
        
        # BRL
        ttk.Label(preset_frame, text="BRIGHTNESS LEVEL").pack(pady=(5,0))
        self.bright_slider = tk.Scale(preset_frame, from_=0, to=3, variable=self.bright_var, resolution=1, orient="horizontal", tickinterval=1, length=300)
        self.bright_slider.pack()

        # 저장 및 취소 버튼
        button_frame = ttk.Frame(self)
        button_frame.pack(pady=10)
        ttk.Button(button_frame, text="SAVE", command=self.save_current_settings).pack(side="left", padx=5)
        ttk.Button(button_frame, text="CANCEL", command=self.go_back).pack(side="left", padx=5)

    def update_entries_from_id(self):
        selected_id = self.id_var.get()
        self.setting_data["PS"] = selected_id
        for entry in self.preset_data:
            if entry["id"] == selected_id:
                self.temp_var.set(entry["PS_TEMP"])
                self.aq_slider.set(entry["PS_AQL"])
                self.dust_slider.set(entry["PS_DDL"])
                self.bright_slider.set(entry["PS_BRL"])
                break

    def save_current_settings(self):
        selected_id = self.id_var.get()
        for entry in self.preset_data:
            if entry["id"] == selected_id:
                temp = round(self.temp_var.get(), 1)
                if 16.0 <= temp <= 30.0:
                    entry["PS_TEMP"] = int(temp)
                    entry["PS_AQL"] = int(self.aq_slider.get())
                    entry["PS_DDL"] = int(self.dust_slider.get())
                    entry["PS_BRL"] = int(self.bright_slider.get())

                    self.setting_data["PS"] = selected_id
                    self.save_json(self.PRESET_FILE, self.preset_data)
                    self.save_json(self.SETTING_FILE, self.setting_data)

                    print(f'PRESET SAVED: {entry}')
                    print(f'SEND DATA : {self.setting_data}')
                    ser_preset = [1, entry["id"], entry["PS_TEMP"], entry["PS_AQL"], entry["PS_DDL"], entry["PS_BRL"]]
                    sendData(self.ser, ser_preset)

                    self.go_back()

    def increase_temp(self):
        temp = self.temp_var.get()
        if temp + 0.5 <= 30.0:
            self.temp_var.set(round(temp + 0.5, 1))

    def decrease_temp(self):
        temp = self.temp_var.get()
        if temp - 0.5 >= 16.0:
            self.temp_var.set(round(temp - 0.5, 1))