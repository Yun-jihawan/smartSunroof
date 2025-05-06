import tkinter as tk

from tkinter import ttk
from PIL import Image, ImageTk

from logic.buttonClicked import ButtonClicked
from logic.file_path import CONTROL_ICON_FILES, WEATHER_ICON_FILES, ICON_ERROR

from uart.serial_setup import initialize_serial
from uart.receive_data import receiveData

class HomeFrame(ttk.Frame):
    def __init__(self, master, go_to_setting_callback):
        super().__init__(master, padding=10)
        self.go_to_setting = go_to_setting_callback

        self.weather = {
            "TI": -1,
            "TO": -1,
            "HI": -1,
            "HO": -1,
            "AQI": 0,
            "AQO": 0,
            "DD": -1
        }
        self.ser = initialize_serial()

        self.selected_preset = 1
        self.velocity = 0
        self.buttonClicked = ButtonClicked(self)
        self.create_widgets()
        self.update_weather_ui()

    def load_icon(self, path, size=(45, 45)):
        try:
            image = Image.open(path).resize(size)
        except FileNotFoundError as fe:
            print(f'FILE NOT FOUND ERROR: {fe}')
            image = Image.open(ICON_ERROR).resize(size)
        
        return ImageTk.PhotoImage(image)

    def create_widgets(self):
        # control icon
        self.icon_on = self.load_icon(CONTROL_ICON_FILES["ICON_ON"])
        self.icon_off = self.load_icon(CONTROL_ICON_FILES["ICON_OFF"])
        self.icon_set = self.load_icon(CONTROL_ICON_FILES["ICON_SETTING_BTN"])

        # weather icon
        self.icon_ti = self.load_icon(WEATHER_ICON_FILES["ICON_TEMP_HUMI_IN"])
        self.icon_to = self.load_icon(WEATHER_ICON_FILES["ICON_TEMP_HUMI_OUT"])
        self.icon_aq = [self.load_icon(WEATHER_ICON_FILES[f"ICON_AQ_{i}"]) for i in range(4)]
        self.icon_pm = self.load_icon(WEATHER_ICON_FILES["ICON_PM"])
        self.icon_sp = self.load_icon(WEATHER_ICON_FILES["ICON_SP"])
        
        # control frame
        control_frame = ttk.LabelFrame(self, text="CONTROL", width=380, height=380)
        control_frame.pack_propagate(False)
        control_frame.pack(side="left", expand=True, fill="both", padx=5, pady=5)

        # sunroof motor on/off
        sunroof_frame = ttk.LabelFrame(control_frame, text="SUN ROOF", width=200, height=80)
        sunroof_frame.pack_propagate(False)
        sunroof_frame.pack()
        
        self.sunroof_btn_open = tk.Button(sunroof_frame, text="OPEN", command=lambda: self.buttonClicked.toggle_sunroof_open())
        self.sunroof_btn_open.pack(side="left", padx=5)

        self.sunroof_btn_close = tk.Button(sunroof_frame, text="CLOSE", state="disabled", command=lambda: self.buttonClicked.toggle_sunroof_close())
        self.sunroof_btn_close.pack(side="left", padx=5)

        self.label_sunroof = ttk.Label(sunroof_frame, text="CLOSE")
        self.label_sunroof.pack(side="left", padx=10)

        # Transparency control
        tp_frame = ttk.LabelFrame(control_frame, text="SUNROOF TRANSPARENCY", width=200, height=80)
        tp_frame.pack_propagate(False)
        tp_frame.pack()

        self.init_tp = self.buttonClicked.setting_data["TP"]
        tp_scale = tk.Scale(tp_frame, from_=0, to=100, variable=tk.IntVar(value=self.init_tp), orient="horizontal", 
                             command=self.buttonClicked.update_setting_tp,
                             length=120, sliderlength=30, width=20)
        tp_scale.pack(side="left", fill="x", expand=True, padx=5)
        tp_scale.bind("<ButtonRelease-1>", self.buttonClicked.on_scale_relase)
        
        self.tp_label = ttk.Label(tp_frame, text=self.init_tp)
        self.tp_label.pack(side="right", padx=10)

        # A/C control on/off
        ac_frame = ttk.LabelFrame(control_frame, text="A/C", width=200, height=80)
        ac_frame.pack_propagate(False)
        ac_frame.pack()

        ac_button_frame = tk.Frame(ac_frame)
        ac_button_frame.pack(expand=True)

        self.btn_ac_in = tk.Button(ac_button_frame, text="A/C OFF", command=lambda: self.buttonClicked.toggle_in_ac())
        self.btn_ac_in.pack(side="left", padx=5)
        self.btn_ac_out = tk.Button(ac_button_frame, text="OUT OFF", command=lambda: self.buttonClicked.toggle_out_ac())
        self.btn_ac_out.pack(side="left", padx=5)

        # smart mode control
        sm_frame = ttk.LabelFrame(control_frame, text="SMART MODE", padding=10)
        sm_frame.pack()

        self.sm_btn = tk.Button(sm_frame, image=self.icon_on, command=lambda: self.buttonClicked.toggle_btn_smart_mode(), borderwidth=0)
        self.sm_btn.pack()

        # preset_frame
        preset_frame = ttk.LabelFrame(control_frame, text="PRESET")
        preset_frame.pack()
        self.ps_btn1 = tk.Button(preset_frame, text="1", command=lambda: self.buttonClicked.toggle_btn_preset(1))
        self.ps_btn2 = tk.Button(preset_frame, text="2", command=lambda: self.buttonClicked.toggle_btn_preset(2))
        self.ps_btn3 = tk.Button(preset_frame, text="3", command=lambda: self.buttonClicked.toggle_btn_preset(3))
        ps_setting_btn = tk.Button(preset_frame, image=self.icon_set, command=self.go_to_setting)
        self.buttonClicked.init_btn_preset()
        
        self.ps_btn1.pack(side="left", padx=5)
        self.ps_btn2.pack(side="left", padx=5)
        self.ps_btn3.pack(side="left", padx=5)
        ps_setting_btn.pack(pady=10)
        
        # weather
        weather_frame = ttk.LabelFrame(self, text="WEATHER")
        weather_frame.pack(side="right", expand=True, fill="both", padx=5, pady=5)

        # TEMP_IN
        row1 = ttk.Frame(weather_frame)
        ttk.Label(row1, image=self.icon_ti).pack(side="left", padx=5)
        ttk.Label(row1, text=f'{int(self.weather["TI"])}°C / {int(self.weather["HI"])}%', font=("Arial", 12)).pack(side="left")
        row1.pack(pady=4)

        # TEMP_OUT
        row2 = ttk.Frame(weather_frame)
        ttk.Label(row2, image=self.icon_to).pack(side="left", padx=5)
        ttk.Label(row2, text=f'{int(self.weather["TO"])}°C / {int(self.weather["HO"])}%', font=("Arial", 12)).pack(side="left")
        row2.pack(pady=4)

        row3 = ttk.Frame(weather_frame)
        ttk.Label(row3, text="IN:").pack(side="left")
        ttk.Label(row3, image=self.icon_aq[self.weather['AQI']]).pack(side="left", padx=5)
        ttk.Label(row3, text="OUT:").pack(side="left")
        ttk.Label(row3, image=self.icon_aq[self.weather['AQO']]).pack(side="left", padx=5)
        row3.pack(pady=4)

        row4 = ttk.Frame(weather_frame)
        ttk.Label(row4, image=self.icon_pm, borderwidth=0).pack(side="left", padx=5)
        ttk.Label(row4, text=f"{self.weather['DD']} ppm", font=("Arial", 12)).pack(side="left")
        row4.pack(pady=4)

        row5 = ttk.Frame(weather_frame)
        ttk.Label(row5, image=self.icon_sp, borderwidth=0).pack(side="left", padx=5)
        ttk.Label(row5, text=f"{self.velocity} km/h", font=("Arial", 12)).pack(side="left")
        row5.pack(pady=4)
    
    def update_weather_ui(self):
        received_weather = receiveData(self.ser)
        if received_weather is not None:        
            self.weather["TI"] = received_weather[1]
            self.weather["HI"] = received_weather[2]
            self.weather["TO"] = received_weather[3]
            self.weather["HO"] = received_weather[4]
            self.weather["AQI"] = received_weather[5]
            self.weather["AQO"] = received_weather[6]
            self.weather["DD"] = received_weather[7]
            self.velocity = received_weather[8]

