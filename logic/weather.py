class WeatherBoard:
    def __init__(self, app):
        self.app = app
        self.weather = {
            "temp_in": -1,
            "temp_out": -1,
            "humi_in": -1,
            "humi_out": -1,
            "air_quality_in": -1,
            "air_quality_out": -1,
            "dust_density": -1
        }

    def update_weather_ui(self, received_weather):
        if received_weather[0] != 0 or len(received_weather) < 8:
            print("wrong data")
            return
        
        self.weather["temp_in"] = received_weather[1]
        self.weather["humi_in"] = received_weather[2]
        self.weather["temp_out"] = received_weather[3]
        self.weather["humi_out"] = received_weather[4]
        self.weather["air_quality_in"] = received_weather[5]
        self.weather["air_quality_out"] = received_weather[6]
        self.weather["dust_density"] = received_weather[7]

        for key, value in self.weahter.items():
            self.weather[key].config(text=str(value))

        print(f"WEATHER UPDATE:{self.weather}")
