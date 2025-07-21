import tkinter as tk
from tkinter import ttk

from manager.setting_manager import SettingManager
from manager.preset_manager import PresetManager
from handler.preset_handler import PresetHandler

class SettingPreset(ttk.Frame):
    def __init__(self, master, go_back_callback, uart_sender):
        super().__init__(master, padding=10)
        self.go_back = go_back_callback

        self.setting_manager = SettingManager()
        self.preset_manager = PresetManager()

        self.buttonClicked = PresetHandler(self, self.preset_manager, self.setting_manager, uart_sender)
        self.init_var()
        self.create_widgets()
        self.apply_var()

    def init_var(self):
        self.temp_var = tk.IntVar()
        self.aq_var = tk.IntVar()
        self.dust_var = tk.IntVar()
        self.bright_var = tk.IntVar()

    def apply_var(self):
        ps_id = self.setting_manager.get("PS") # ps_id
        entry = self.preset_manager.get(ps_id)

        self.temp_var.set(entry["PS_TEMP"])
        self.aq_var.set(entry["PS_AQL"])
        self.dust_var.set(entry["PS_DDL"])
        self.bright_var.set(entry["PS_BRL"])

    def create_widgets(self):
        ttk.Label(self, text="PRESET SETTING", font=("Arial", 16)).pack(pady=10)
        preset_frame = ttk.Frame(self)
        preset_frame.pack()

        # preset select
        ps_id = self.setting_manager.get("PS")
        self.id_var = tk.IntVar(value=ps_id)
        id_frame = ttk.Frame(preset_frame)
        id_frame.pack(pady=5)
        for i in range(1, 4):
            ttk.Radiobutton(id_frame, text=i, variable=self.id_var, value=i, 
                            command=lambda: self.buttonClicked.init_preset(self.id_var.get())).pack(side="left", padx=10)
            
        # TEMPERATURE SETTING
        temp_frame = ttk.Frame(preset_frame)
        temp_frame.pack(pady=5)
        
        ttk.Button(temp_frame, text="▲", command=self.buttonClicked.increase_temp).pack(side="left", padx=5)
        ttk.Label(temp_frame, textvariable=self.temp_var, width=5, relief="sunken", anchor="center").pack(side="left", padx=5)
        ttk.Button(temp_frame, text="▼", command=self.buttonClicked.decrease_temp).pack(side="left", padx=5)
            
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
        ttk.Button(button_frame, text="SAVE", command=lambda: self.buttonClicked.apply_preset(self.id_var.get())).pack(side="left", padx=5)
        ttk.Button(button_frame, text="CANCEL", command=self.go_back).pack(side="left", padx=5)