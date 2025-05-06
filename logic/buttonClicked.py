import tkinter as tk
import json
#from state_enum import SunroofState, AirConditionState
from enum import Enum
from logic.file_path import SETTING_FILE
from uart.serial_setup import initialize_serial
from uart.send_data import sendData

class SunroofState(Enum):
    CLOSE = 0
    TILTING = 1
    OPEN = 2

class AirConditionState(Enum):
    IN_OFF_OUT_OFF = 0
    IN_OFF_OUT_ON = 1
    IN_ON_OUT_OFF = 2
    IN_ON_OUT_ON = 3

class GeneralState(Enum):
    OFF = 0
    ON = 1

class SensitivityState(Enum):
    OFF = 0
    LOW = 1
    MID = 2
    HIGH = 3

class DataType(Enum):
    USER_SETTING = 0 # freeset button clicked -> sendData(DataType(FREESET))
    FREESET = 1 # user seetting modified -> sendData(DataType(USER_SETTING))


class ButtonClicked:
    def __init__(self, app):
        self.app = app
        self.setting_data = self.load_setting()
        self.ser = initialize_serial()

    def load_setting(self):
        try:
            with open(SETTING_FILE, "r", encoding="utf-8") as SETTING:
                return json.load(SETTING)
        except:
            return {"SUN": 0,
                    "TP": 0,
                    "AC": 0,
                    "SM": 1,
                    "PS": 1}

    def save_setting(self):
        data = {
            "SUN": self.setting_data["SUN"],
            "TP": self.setting_data["TP"],
            "AC": self.setting_data["AC"],
            "SM": self.setting_data["SM"],
            "PS": self.setting_data["PS"]
        }
        with open(SETTING_FILE, "w", encoding="utf-8") as SETTING:
            json.dump(data, SETTING, indent=4)
        
        print(f"SEND DATA : {data}")
        ser_data = [0, data["SUN"], data["TP"], data["AC"], data["SM"], data["PS"]]
        sendData(self.ser, ser_data)

    # sun_roof motor control
    def toggle_sunroof_open(self):
        if self.setting_data["SUN"] != 2:
            self.setting_data["SUN"] = (self.setting_data["SUN"] + 1) % 3
        self.save_setting()
        self.update_sunroof_ui()

    def toggle_sunroof_close(self):
        self.setting_data["SUN"] = 0
        self.save_setting()
        self.update_sunroof_ui()

    def update_sunroof_ui(self):
        self.app.label_sunroof.config(text=SunroofState(self.setting_data["SUN"]).name)

        # OPEN BUTTON DISABLED when already opened
        if self.setting_data["SUN"] == 2:
            self.app.sunroof_btn_open.config(state=tk.DISABLED)
        else:
            self.app.sunroof_btn_open.config(state=tk.NORMAL)
        
        # CLOSE BUTTON DISABLED when already closed
        if self.setting_data["SUN"] == 0:
            self.app.sunroof_btn_close.config(state=tk.DISABLED)
        else:
            self.app.sunroof_btn_close.config(state=tk.NORMAL)

    # Transparency control
    def update_setting_tp(self, value):
        self.setting_data["TP"] = int(float(value))
        self.app.tp_label.config(text=self.setting_data["TP"])

    # A/C control
    def toggle_in_ac(self):
        self.setting_data["AC"] = AirConditionState(self.setting_data["AC"] ^ 0b10).value
        self.update_ac_ui()

    def toggle_out_ac(self):
        self.setting_data["AC"] = AirConditionState(self.setting_data["AC"] ^ 0b01).value
        self.update_ac_ui()

    def is_in_ac_on(self, state):
        return (state.value >> 1) & 1 == 1

    def is_out_ac_on(self, state):
        return state.value & 1 == 1

    def update_ac_ui(self):
        self.app.btn_ac_in.config(text=f'A/C {"ON " if self.is_in_ac_on(AirConditionState(self.setting_data["AC"])) else "OFF"}')
        self.app.btn_ac_out.config(text=f'O/A {"ON " if self.is_out_ac_on(AirConditionState(self.setting_data["AC"])) else "OFF"}')
        self.save_setting()

    def init_btn_preset(self):
        self.app.ps_btn1.config(state="normal")
        self.app.ps_btn2.config(state="normal")
        self.app.ps_btn3.config(state="normal")
        PS_ID = self.setting_data["PS"]

        if PS_ID == 1:
            self.app.ps_btn1.config(state="disabled")
            self.setting_data["PS"] = 1
        elif PS_ID == 2:
            self.app.ps_btn2.config(state="disabled")
            self.setting_data["PS"] = 2
        elif PS_ID == 3:
            self.app.ps_btn3.config(state="disabled")
            self.setting_data["PS"] = 3
    
    # preset control
    def toggle_btn_preset(self, selected_button):
        self.app.preset_id = selected_button
        self.app.ps_btn1.config(state="normal")
        self.app.ps_btn2.config(state="normal")
        self.app.ps_btn3.config(state="normal")

        if selected_button == 1:
            self.app.ps_btn1.config(state="disabled")
            self.setting_data["PS"] = 1
        elif selected_button == 2:
            self.app.ps_btn2.config(state="disabled")
            self.setting_data["PS"] = 2
        elif selected_button == 3:
            self.app.ps_btn3.config(state="disabled")
            self.setting_data["PS"] = 3
    
        self.save_setting()

    def toggle_btn_smart_mode(self):
        self.setting_data["SM"] = (self.setting_data["SM"] + 1) % 2

        if self.setting_data["SM"] == 0:
            self.app.sm_btn.config(image=self.app.icon_off, borderwidth=0)
        else:
            self.app.sm_btn.config(image=self.app.icon_on, borderwidth=0)
        
        self.save_setting()
    
    def on_scale_relase(self, event):
        self.save_setting()

    def update_setting_tp(self, value):
        self.setting_data["TP"] = int(float(value))
        self.app.tp_label.config(text=f"{self.setting_data['TP']}")

    