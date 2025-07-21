import tkinter as tk
from ui.frame_home import HomeFrame
from ui.frame_preset import SettingPreset
from uart.uart_receiver import UartReceiver
from uart.uart_sender import UartSender
# from uart.serial_setup import initialize_serial

class DisplayApp(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("In-Vehicle Display")
        self.geometry("800x480")
        self.resizable(False, False)

        self.uart_receiver = UartReceiver()
        self.uart_sender = UartSender()

        self.uart_receiver.start()

        self.current_frame = None
        self.show_home()

    def show_home(self):
        if self.current_frame:
            self.current_frame.destroy()
        self.current_frame = HomeFrame(self, self.show_setting, self.uart_receiver, self.uart_sender)
        self.current_frame.pack(fill="both", expand=True)

    def show_setting(self):
        if self.current_frame:
            self.current_frame.destroy()
        self.current_frame = SettingPreset(self, self.show_home, self.uart_sender)
        self.current_frame.pack(fill="both", expand=True)


if __name__ == "__main__":
    app = DisplayApp()
    #app.protocol("WM_DELETE_WINDOW", app.on_closing)
    app.mainloop()