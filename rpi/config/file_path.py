import os

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# FOLDER PATH
JSON_DIR = os.path.join(BASE_DIR, "resources", "json")
ICONS_DIR = os.path.join(BASE_DIR, "resources", "icons")

# JSON FILE PATH
SETTING_FILE = os.path.join(JSON_DIR, "setting.json")
PRESET_FILE = os.path.join(JSON_DIR, "preset.json")
WEATHER_FILE = os.path.join(JSON_DIR, "weather.json")

# ICONS FILE PATH
WEATHER_ICON_FILES = {
    "ICON_TEMP_HUMI_IN": os.path.join(ICONS_DIR, "humi_in.png"),
    "ICON_TEMP_HUMI_OUT": os.path.join(ICONS_DIR, "humi_out.png"),
    "ICON_AQ_0": os.path.join(ICONS_DIR, "aq_0.png"),
    "ICON_AQ_1": os.path.join(ICONS_DIR, "aq_1.png"),
    "ICON_AQ_2": os.path.join(ICONS_DIR, "aq_2.png"),
    "ICON_AQ_3": os.path.join(ICONS_DIR, "aq_3.png"),
    "ICON_AQ_BASIC": os.path.join(ICONS_DIR, "aq_basic.png"),
    "ICON_PM": os.path.join(ICONS_DIR, "pm.png"),
    "ICON_SP": os.path.join(ICONS_DIR, "speed.png")
}

CONTROL_ICON_FILES = {
    "ICON_CONTROL_BASIC": os.path.join(ICONS_DIR, "control_basic.png"),
    "ICON_SUN_OPEN": os.path.join(ICONS_DIR, "sun_open.png"),
    "ICON_SUN_CLOSE": os.path.join(ICONS_DIR, "sun_close.png"),
    "ICON_AC_IN": os.path.join(ICONS_DIR, "ac_in.png"),
    "ICON_AC_OUT": os.path.join(ICONS_DIR, "ac_out.png"),
    "ICON_SETTING_BTN": os.path.join(ICONS_DIR, "set_btn.png"),
    "ICON_ON": os.path.join(ICONS_DIR, "toggle_on.png"),
    "ICON_OFF": os.path.join(ICONS_DIR, "toggle_off.png")

}

ICON_ERROR = os.path.join(ICONS_DIR, "error.png")
