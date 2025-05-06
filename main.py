import tkinter as tk
from ui.frame_home import HomeFrame
from ui.frame_setting import SettingFrame
# from uart.serial_setup import initialize_serial

class DisplayApp(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("In-Vehicle Display")
        self.geometry("800x480")
        self.resizable(False, False)

        # self.ser = initialize_serial()
        self.current_frame = None
        self.show_home()

    def show_home(self):
        if self.current_frame:
            self.current_frame.destroy()
        self.current_frame = HomeFrame(self, self.show_setting)
        self.current_frame.pack(fill="both", expand=True)

    def show_setting(self):
        if self.current_frame:
            self.current_frame.destroy()
        self.current_frame = SettingFrame(self, self.show_home)
        self.current_frame.pack(fill="both", expand=True)

if __name__ == "__main__":
    app = DisplayApp()
    app.mainloop()