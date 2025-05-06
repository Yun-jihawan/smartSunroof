import tkinter as tk
from tkinter import ttk
import json
import os
from freeset_gui import create_freeset_frame
from myEnum import SunroofState, AirConditionState, GeneralState
from uart import sendData

JSON_FILE = "setting.json"

# global variable
is_in_ac = False
is_out_ac = False
freeset_selected = 1

# init setting value
sunroof_value = 0
transparency_value = 0
air_condition_value = 0
smart_mode_value = 1

# sensor not detected -> -1
freeset = 1
temp_in = -1
temp_out = -1
humi_in = -1
humi_out = -1
air_quality = -1
dust_density = -1

def get_default_data():
    return {
        "SUN": sunroof_value,
        "TP": transparency_value,
        "AC": air_condition_value,
        "SM": smart_mode_value,
        "FS": freeset,
        "TI": temp_in,
        "TO": temp_out,
        "HI": humi_in,
        "HO": humi_out,
        "AQ": air_quality,
        "DD": dust_density
    }

if not os.path.exists(JSON_FILE):
    with open(JSON_FILE, "w", encoding="utf-8") as f:
        json.dump(get_default_data(), f, indent=4)

def load_setting():
    with open(JSON_FILE, "r", encoding="utf-8") as f:
        # global variable
        is_in_ac = False
        is_out_ac = False
        freeset_selected = 1

        # init setting value
        sunroof_value = 0
        transparency_value = 0
        air_condition_value = 0
        smart_mode_value = 1
        
        return json.load(f)


def save_setting():
    data = {
        "SUN": sunroof_value,
        "TP": transparency_value,
        "AC": air_condition_value,
        "SM": smart_mode_value,
        "FS": freeset,
        "TI": temp_in,
        "TO": temp_out,
        "HI": humi_in,
        "HO": humi_out,
        "AQ": air_quality,
        "DD": dust_density
    }

    with open(JSON_FILE, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=4)
    print(f'changed data: {data}')

def toggle_sunroof_open():
    global sunroof_value
    if sunroof_value != 2:
        sunroof_value = (sunroof_value + 1) % 3

    save_setting()
    update_sunroof_ui()
    
def toggle_sunroof_close():
    global sunroof_value
    sunroof_value = 0
    
    save_setting()
    update_sunroof_ui()

def update_sunroof_ui():
    label_sunroof.config(text=SunroofState(sunroof_value).name)

    # OPEN BUTTON DISABLED when already opened
    if sunroof_value == 2:
        sunroof_btn_open.config(state=tk.DISABLED)
    else:
        sunroof_btn_open.config(state=tk.NORMAL)
    
    # CLOSE BUTTON DISABLED when already closed
    if sunroof_value == 0:
        sunroof_btn_close.config(state=tk.DISABLED)
    else:
        sunroof_btn_close.config(state=tk.NORMAL)

def is_in_ac_on(state):
    return (state.value >> 1) & 1 == 1

def is_out_ac_on(state):
    return state.value & 1 == 1

def toggle_in_ac():
    global air_condition_value
    air_condition_value = AirConditionState(air_condition_value ^ 0b10).value
    btn_in_ac.config(text="A/C ON")
    update_ac_ui()

def toggle_out_ac():
    global air_condition_value
    air_condition_value = AirConditionState(air_condition_value ^ 0b01).value
    update_ac_ui()

def update_ac_ui():
    global air_condition_value
    btn_in_ac.config(text=f'A/C {"ON" if is_in_ac_on(AirConditionState(air_condition_value)) else "OFF"}')
    btn_out_ac.config(text=f'O/A {"ON" if is_out_ac_on(AirConditionState(air_condition_value)) else "OFF"}')
    save_setting()

def toggle_freeset(selected_button):
    btn_fr1.config(state="normal")
    btn_fr2.config(state="normal")
    btn_fr3.config(state="normal")

    if selected_button == 1:
        btn_fr1.config(state="disabled")
        freeset_selected = 1
    elif selected_button == 2:
        btn_fr2.config(state="disabled")
        freeset_selected = 2
    elif selected_button == 3:
        btn_fr3.config(state="disabled")
        freeset_selected = 3

def toggle_btn_smart_mode():
    global smart_mode_value
    smart_mode_value = (smart_mode_value + 1) % 2

    if smart_mode_value == 0:
        btn_smart_mode.config(text="OFF", relief="raised")
    else:
        btn_smart_mode.config(text="ON", relief="sunken")
    
    save_setting()


def on_scale_release(event):
    save_setting()

def update_setting_tp(value):
    global transparency_value
    transparency_value = int(float(value))
    label_tp.config(text=f"{transparency_value}")
    # save_setting()

def get_color(state):
    pass

def go_to_settings(selected_button):
    frame_home.pack_forget()
    global frame_settings
    frame_settings = create_freeset_frame(root, go_home)
    frame_settings.pack()

def go_home():
    if frame_settings:
        frame_settings.pack_forget()
    frame_home.pack()

# root setting
root = tk.Tk()
root.title("DISPLAY INTERFACE")
root.geometry("800x480")
root.resizable(False, False)

# frame_home
frame_home = tk.Frame(root)
frame_home.pack()

frame_settings = None

# sunroof motor on/off
frame_sunroof = ttk.LabelFrame(frame_home, text="SUN ROOF", padding=10)
frame_sunroof.grid(row=0, column=0, padx=10, pady=10)

sunroof_btn_open = tk.Button(frame_sunroof, text="OPEN", command=lambda: toggle_sunroof_open())
sunroof_btn_open.pack(side="left", padx=5)

sunroof_btn_close = tk.Button(frame_sunroof, text="CLOSE", state="disabled", command=lambda: toggle_sunroof_close())
sunroof_btn_close.pack(side="left", padx=5)

label_sunroof = ttk.Label(frame_sunroof, text="CLOSE")
label_sunroof.pack(side="left", padx=10)

# weather board
frame_weather_board = ttk.LabelFrame(frame_home, text="WEATHER", padding=10)
frame_weather_board.grid(row=0, column=1, padx=10, pady=10)

label_temp_in = ttk.Label(frame_weather_board, text=f'내부 온도 : {temp_in}', font=("Arial", 16))
label_temp_in.grid(row=0, column=0, padx=5, pady=5)

label_temp_out = ttk.Label(frame_weather_board, text=f'외부 온도 : {temp_out}', font=("Arial", 16))
label_temp_out.grid(row=0, column=1, padx=5, pady=5)

label_humi_in = ttk.Label(frame_weather_board, text=f'내부 습도 : {humi_in}%', font=("Arial", 16))
label_humi_in.grid(row=1, column=0, padx=5, pady=5)

label_humi_out = ttk.Label(frame_weather_board, text=f'외부 습도 : {humi_out}%', font=("Arial", 16))
label_humi_out.grid(row=1, column=1, padx=5, pady=5)

label_air = tk.Label(frame_weather_board, text=f'대기 : {air_quality}', font=("Arial", 16, "bold"), fg=get_color(air_quality))
label_air.grid(row=2, column=0, padx=5, pady=5)

label_dust = tk.Label(frame_weather_board, text=f'미세먼지 : {dust_density}', font=("Arial", 16, "bold"), fg=get_color(dust_density))
label_dust.grid(row=2, column=1, padx=5, pady=5)

# transparency setting
frame_tp = ttk.LabelFrame(frame_home, text="SUNROOF TRANSPARENCY", padding=10)
frame_tp.grid(row=1, column=0, padx=10, pady=10)

scale_tp = ttk.Scale(frame_tp, from_=0, to=100, variable=transparency_value, orient="horizontal", command=update_setting_tp)
scale_tp.pack(side="left", fill="x", expand=True, padx=5)

# scale_tp.set(transparency_value)
scale_tp.bind("<ButtonRelease-1>", on_scale_release)

label_tp = ttk.Label(frame_tp, text=transparency_value)
label_tp.pack(side="right", padx=10)

# air conditioning setting
frame_ac = ttk.LabelFrame(frame_home, text="AIR CIRCULATION", padding=10)
frame_ac.grid(row=2, column=0, padx=10, pady=10)

btn_in_ac = tk.Button(frame_ac, text="A/C OFF", width=10, command=lambda: toggle_in_ac())
btn_in_ac.pack(side="left", padx=5)

btn_out_ac = tk.Button(frame_ac, text="O/A OFF", width=10, command=lambda: toggle_out_ac())
btn_out_ac.pack(side="left", padx=5)

# freeset setting
frame_freeset = ttk.LabelFrame(frame_home, text="FREE SET", padding=10)
frame_freeset.grid(row=2, column=1, padx=10, pady=10)

btn_fr1 = tk.Button(frame_freeset, text="1", command=lambda: toggle_freeset(1))
btn_fr2 = tk.Button(frame_freeset, text="2", command=lambda: toggle_freeset(2))
btn_fr3 = tk.Button(frame_freeset, text="3", command=lambda: toggle_freeset(3))
btn_fr_setting = tk.Button(frame_freeset, text="SET", command=lambda: go_to_settings(freeset_selected))

btn_fr1.pack(side="left", padx=5)
btn_fr2.pack(side="left", padx=5)
btn_fr3.pack(side="left", padx=5)
btn_fr_setting.pack(side="left", padx=5)

frame_smart_mode = ttk.LabelFrame(frame_home, text="SMART MODE", padding=10)
frame_smart_mode.grid(row=3, column=0, padx=10, pady=10)

btn_smart_mode = tk.Button(frame_smart_mode, text="ON", relief="sunken", command=lambda: toggle_btn_smart_mode())
btn_smart_mode.pack()

print(load_setting())
# update_ac_ui()
root.mainloop()