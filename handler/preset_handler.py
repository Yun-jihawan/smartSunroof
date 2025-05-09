from uart.uart_sender import UartSender

class PresetHandler:
    def __init__(self, frame, preset_manager, setting_manager, uart_sender):
        self.frame = frame
        self.preset_manager = preset_manager
        self.setting_manager = setting_manager
        self.uart_sender = uart_sender
    
    def init_preset(self, ps_id: int):
        self._update_preset_ui(ps_id)

    def apply_preset(self, ps_id: int):
        self._update_preset_setting(ps_id)
        self._update_preset_ui(ps_id)
        self.uart_sender.send_preset(ps_id)
        self.frame.go_back()

    def _update_preset_ui(self, ps_id: int):
        self.setting_manager.set("PS", ps_id)
        entry = self.preset_manager.get(ps_id)
        #print(entry)
        self.frame.temp_var.set(entry["PS_TEMP"])
        self.frame.aq_slider.set(entry["PS_AQL"])
        self.frame.dust_slider.set(entry["PS_DDL"])
        self.frame.bright_slider.set(entry["PS_BRL"])
        
    def _update_preset_setting(self, ps_id: int):
        entry = self.preset_manager.get(ps_id)
        temp = round(self.frame.temp_var.get(), 1)
        if 16 <= temp <= 30:
            entry["PS_TEMP"] = int(temp)
            entry["PS_AQL"] = int(self.frame.aq_slider.get())
            entry["PS_DDL"] = int(self.frame.dust_slider.get())
            entry["PS_BRL"] = int(self.frame.bright_slider.get())
            self.preset_manager.set(ps_id, entry)
            self.setting_manager.set("PS", ps_id)

    def increase_temp(self):
        temp = self.frame.temp_var.get()
        if temp + 1 <= 30:
            self.frame.temp_var.set(temp + 1)

    def decrease_temp(self):
        temp = self.frame.temp_var.get()
        if temp - 1 >= 16:
            self.frame.temp_var.set(temp - 1)