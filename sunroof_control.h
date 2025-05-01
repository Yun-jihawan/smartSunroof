 #ifndef INC_SUNROOF_CONTROL_H_
 #define INC_SUNROOF_CONTROL_H_
 
 #include <stdio.h>
 
 #define SUNROOF_CLOSED 0
 #define SUNROOF_TILT   1
 #define SUNROOF_OPEN   2
 
 // 기준값 정의
 #define TEMP_AVG          		     24.0f
 #define HUMIDITY_HIGH      		 70.0f
 #define LIGHT_THRESHOLD    		 800
 #define VELOCITY_THRESHOLD          60.0f
 #define AIR_BAD_THRESHOLD  		 1.2f    // 종합 AQI 기준
 #define AIR_SOSO_THRESHOLD          1.0f    // 종합 AQI 기준
 #define AIR_GOOD_THRESHOLD 		 0.8f    // 양호 기준
 

 // 정규화 함수
 float Normalize(float value, float threshold);
 
 // 공기질 가중 합 지수 계산 함수
 
 float Calculate_WeightedAirQualityIndex_In(float benzene, float co, float co2, float smoke);
 float Calculate_WeightedAirQualityIndex_Out(float benzene, float co, float co2, float smoke, float pm25);
 
 int Send_Sunroof_Command_CGW_to_SUN(float temp_in, float temp_out, float humi_in, float humi_out,
         float benzene_in, float co_in, float co2_in, float smoke_in,
         float benzene_out, float co_out, float co2_out, float smoke_out, float pm25_out,
         int light, int rain_detected, float velocity, int current_state);
 
 
 #endif /* INC_SUNROOF_CONTROL_H_ */
 