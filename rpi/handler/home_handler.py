import tkinter as tk

from config.enums import SunroofState, AirConditionState

class ButtonHandler:
    def __init__(self, frame, setting_manager, uart_sender):
        self.frame = frame
        self.setting_manager = setting_manager
        self.uart_sender = uart_sender

    def toggle_sunroof_open(self):
        sun_state = self.setting_manager.get("SUN")
        if sun_state != 2:
            sun_state = (sun_state + 1) % 3
        self.apply_sunroof_state(sun_state)

    def toggle_sunroof_close(self):
        self.apply_sunroof_state(SunroofState.CLOSE.value)

    def apply_sunroof_state(self, state: int):
        self._update_sunroof_ui(state)
        self._update_setting("SUN", state)

    def _update_sunroof_ui(self, state: int):
        if state == SunroofState.CLOSE.value:
            self.frame.sunroof_btn_open.config(state="normal")
            self.frame.sunroof_btn_close.config(state="disabled")
            self.frame.sunroof_label.config(text="CLOSE")
        elif state == SunroofState.TILT.value:
            self.frame.sunroof_btn_open.config(state="normal")
            self.frame.sunroof_btn_close.config(state="normal")
            self.frame.sunroof_label.config(text="TILT")
        elif state == SunroofState.OPEN.value:
            self.frame.sunroof_btn_open.config(state="disabled")
            self.frame.sunroof_btn_close.config(state="normal")
            self.frame.sunroof_label.config(text="OPEN")

    def _update_setting(self, key, state: int):
        self.setting_manager.set(key, state)
        print(f'[BTN {key}]: {state}')
        if key != "PS":
            self.uart_sender.send_setting()

    def _update_tp_ui(self, value):
        int_val = int(value)
        self.frame.tp_label.config(text=int_val)
        self.frame.tp_scale.config(variable=tk.IntVar(value=int_val))

    def on_scale_release(self, event):
        widget = event.widget
        final_value = widget.get()
        self._update_setting("TP", final_value)
        
    def toggle_in_ac(self):
        in_ac_state = self.setting_manager.get("AC") ^ 0b10
        self.apply_ac_setting(in_ac_state)

    def toggle_out_ac(self):
        out_ac_state = self.setting_manager.get("AC") ^ 0b01
        self.apply_ac_setting(out_ac_state)

    def apply_ac_setting(self, state: int):
        self._update_ac_ui(state)
        self._update_setting("AC", state)

    def _is_in_ac_on(self, state: int):
        return (state >> 1) & 1 == 1
    
    def _is_out_ac_on(self, state: int):
        return state & 1 == 1
    
    def _update_ac_ui(self, state: int):
        if self._is_in_ac_on(state): 
            self.frame.btn_ac_in.config(fg="green")
        else:
            self.frame.btn_ac_in.config(fg="red")
        
        if self._is_out_ac_on(state):
            self.frame.btn_ac_out.config(fg="green")
        else:
            self.frame.btn_ac_out.config(fg="red")
    
    def toggle_smart_mode(self):
        sm_state = self.setting_manager.get("SM")
        sm_state = (sm_state + 1)%2
        self.apply_sm_setting(sm_state)

    def apply_sm_setting(self, state: int):
        self._update_sm_ui(state)
        self._update_setting("SM", state)

    def _update_sm_ui(self, state: int):
        if state == 0:
            self.frame.btn_sm.config(fg="red")
        else:
            self.frame.btn_sm.config(fg="green")

    def toggle_btn_preset(self, value=None):
        if value == None:
            value = self.setting_manager.get("PS")

        self.frame.btn_ps1.config(state="normal")
        self.frame.btn_ps2.config(state="normal")
        self.frame.btn_ps3.config(state="normal")

        if value == 1:
            self.frame.btn_ps1.config(state="disabled")
        elif value == 2:
            self.frame.btn_ps2.config(state="disabled")
        elif value == 3:
            self.frame.btn_ps3.config(state="disabled")

        self._update_setting("PS", value)
        self.uart_sender.send_preset(value)