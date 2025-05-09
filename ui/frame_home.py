import tkinter as tk

from tkinter import ttk
from PIL import Image, ImageTk

from config.file_path import WEATHER_ICON_FILES, ICON_ERROR
from manager.setting_manager import SettingManager
from manager.preset_manager import PresetManager
from manager.weather_manager import WeatherManager
from handler.home_handler import ButtonHandler

class HomeFrame(ttk.Frame):
    def __init__(self, master, go_to_setting_callback, uart_receiver, uart_sender):
        super().__init__(master, padding=10)
        self.go_to_setting = go_to_setting_callback

        self.weather_labels = {}

        self.setting_manager = SettingManager()
        self.preset_manager = PresetManager()
        self.weather_manager = WeatherManager()
        self.uart_sender = uart_sender
        self.uart_receiver = uart_receiver

        self.buttonClicked = ButtonHandler(self, self.setting_manager, uart_sender)
        self.create_widgets()
        self.update_ui()

    def load_icon(self, path, size=(45, 45)):
        try:
            image = Image.open(path).resize(size)
        except FileNotFoundError as fe:
            print(f'FILE NOT FOUND ERROR: {fe}')
            image = Image.open(ICON_ERROR).resize(size)
    
        return ImageTk.PhotoImage(image)
    
    def create_widgets(self):
        # weather icon
        self.icons = {
            "TI": self.load_icon(WEATHER_ICON_FILES["ICON_TEMP_HUMI_IN"]),
            "TO": self.load_icon(WEATHER_ICON_FILES["ICON_TEMP_HUMI_OUT"]),
            "AQ": [self.load_icon(WEATHER_ICON_FILES[f"ICON_AQ_{i}"]) for i in range(4)],
            "DD": self.load_icon(WEATHER_ICON_FILES["ICON_PM"]),
            "SP": self.load_icon(WEATHER_ICON_FILES["ICON_SP"]),
        }

        # control frame
        control_frame = ttk.LabelFrame(self, text="CONTROL", width=380, height=380)
        control_frame.pack_propagate(False)
        control_frame.pack(side="left", expand=True, fill="both", padx=5, pady=5)

        # sunroof motor frame
        sunroof_frame = ttk.LabelFrame(control_frame, text="SUN ROOF", width=300, height=80)
        sunroof_frame.pack_propagate(False)
        sunroof_frame.pack()

        self.sunroof_btn_open = tk.Button(sunroof_frame, text="OPEN", command=lambda: self.buttonClicked.toggle_sunroof_open())
        self.sunroof_btn_open.pack(side="left", padx=5)

        self.sunroof_btn_close = tk.Button(sunroof_frame, text="CLOSE", state="disable", command=lambda: self.buttonClicked.toggle_sunroof_close())
        self.sunroof_btn_close.pack(side="left", padx=5)

        self.sunroof_label = tk.Label(sunroof_frame, text="CLOSE")
        self.sunroof_label.pack(side="left", padx=10)
        # Transparency control
        tp_frame = ttk.LabelFrame(control_frame, text="SUNROOF TRANSPARENCY", width=300, height=80)
        tp_frame.pack_propagate(False)
        tp_frame.pack()

        init_tp = self.setting_manager.get("TP")
        self.tp_scale = tk.Scale(tp_frame, from_=0, to=100, variable=tk.IntVar(value=init_tp), orient="horizontal", 
                            command=self.buttonClicked._update_tp_ui,
                            length=120, sliderlength=30, width=20)
        self.tp_scale.pack(side="left", fill="x", expand=True, padx=5)
        self.tp_scale.bind("<ButtonRelease-1>", self.buttonClicked.on_scale_release)
        self.tp_label = ttk.Label(tp_frame, text=init_tp)
        self.tp_label.pack(side="right", padx=10)

        # A/C control on/off
        ac_frame = ttk.LabelFrame(control_frame, text="A/C", width=300, height=80)
        ac_frame.pack_propagate(False)
        ac_frame.pack()

        ac_button_frame = tk.Frame(ac_frame)
        ac_button_frame.pack(expand=True)
        
        init_ac = self.buttonClicked.setting_manager.get("AC")
        if (init_ac >> 1) & 1 == 1:
            in_color = "green"
        else:
            in_color = "red"

        if init_ac & 1 == 1:
            out_color = "green"
        else:
            out_color = "red"

        tk.Label(ac_button_frame, text="I/A").pack(side="left", padx=5)
        self.btn_ac_in = tk.Button(ac_button_frame, text="●", fg=in_color, borderwidth=0, command=self.buttonClicked.toggle_in_ac)
        self.btn_ac_in.pack(side="left", padx=5)
        
        tk.Label(ac_button_frame, text="O/A").pack(side="left", padx=5)
        self.btn_ac_out = tk.Button(ac_button_frame, text="●", fg=out_color, borderwidth=0, command=self.buttonClicked.toggle_out_ac)
        self.btn_ac_out.pack(side="left", padx=5)
        
        # smart mode control
        sm_frame = ttk.LabelFrame(control_frame, text="SMART MODE", padding=10, width=300, height=80)
        sm_frame.pack()

        init_sm = self.buttonClicked.setting_manager.get("SM")
        if init_sm == 0:
            sm_color = "red"
        else:
            sm_color = "green"
        self.btn_sm = tk.Button(sm_frame, text="●", fg=sm_color, borderwidth=0, command=self.buttonClicked.toggle_smart_mode)
        self.btn_sm.pack()

        # preset frame
        preset_frame = ttk.LabelFrame(control_frame, text="PRESET", width=300, height=80)
        preset_frame.pack()

        self.btn_ps1 = tk.Button(preset_frame, text="1", command=lambda: self.buttonClicked.toggle_btn_preset(value=1))
        self.btn_ps2 = tk.Button(preset_frame, text="2", command=lambda: self.buttonClicked.toggle_btn_preset(value=2))
        self.btn_ps3 = tk.Button(preset_frame, text="3", command=lambda: self.buttonClicked.toggle_btn_preset(value=3))
        setting_btn = tk.Button(preset_frame, text="SET", command=self.go_to_setting)
        
        self.buttonClicked.toggle_btn_preset()

        self.btn_ps1.pack(side="left", padx=5)
        self.btn_ps2.pack(side="left", padx=5)
        self.btn_ps3.pack(side="left", padx=5)
        setting_btn.pack(padx=5)

        # weather
        weather_frame = ttk.LabelFrame(self, text="WEATHER")
        weather_frame.pack(side="right", expand=True, fill="both", padx=5, pady=5)

        # TEMP_IN
        row1 = ttk.Frame(weather_frame)
        ttk.Label(row1, image=self.icons["TI"]).pack(side="left", padx=5)
        self.weather_labels["TI"] = ttk.Label(row1, text=f'--°C / --%', font=("Arial", 12))
        self.weather_labels["TI"].pack(side="left")
        row1.pack(pady=4)

        # TEMP_OUT
        row2 = ttk.Frame(weather_frame)
        ttk.Label(row2, image=self.icons["TO"]).pack(side="left", padx=5)
        self.weather_labels["TO"] = ttk.Label(row2, text=f'--°C / --%', font=("Arial", 12))
        self.weather_labels["TO"].pack(side="left")
        row2.pack(pady=4)

        row3 = ttk.Frame(weather_frame)
        ttk.Label(row3, text="IN:").pack(side="left")
        self.weather_labels["AQI"] = ttk.Label(row3, image=self.icons["AQ"][0])
        self.weather_labels["AQI"].pack(side="left", padx=5)
        ttk.Label(row3, text="OUT:").pack(side="left")
        self.weather_labels["AQO"] = ttk.Label(row3, image=self.icons["AQ"][0])
        self.weather_labels["AQO"].pack(side="left", padx=5)
        row3.pack(pady=4)

        row4 = ttk.Frame(weather_frame)
        ttk.Label(row4, image=self.icons["DD"], borderwidth=0).pack(side="left", padx=5)
        self.weather_labels["DD"] = ttk.Label(row4, text=f"-- ppm", font=("Arial", 12))
        self.weather_labels["DD"].pack(side="left")
        row4.pack(pady=4)

    def update_weather(self):
        try:
            wd = self.weather_manager._load_weather()
            self.weather_labels["TI"].config(text=f'{str(wd["TI"])}°C / {wd["HI"]}%')
            self.weather_labels["TO"].config(text=f'{wd["TO"]}°C / {wd["HO"]}%')
            self.weather_labels["AQI"].config(image=self.icons["AQ"][wd["AQI"]])
            self.weather_labels["AQO"].config(image=self.icons["AQ"][wd["AQO"]])
            self.weather_labels["DD"].config(image=self.icons["AQ"][wd["DD"]])
        except Exception as e:
            print(f'ERROR: {e}')

    def update_control(self):
        sd = self.setting_manager._load_settings()
        self.buttonClicked._update_sunroof_ui(sd["SUN"])
        self.buttonClicked._update_tp_ui(sd["TP"])
        self.buttonClicked._update_ac_ui(sd["AC"])
        self.buttonClicked._update_sm_ui(sd["SM"])

    def update_ui(self):
        self.update_weather()
        self.update_control()

        self.after(1000, self.update_ui)