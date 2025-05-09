/*
 * sunroof_control.h
 *
 *  Created on: Apr 30, 2025
 *      Author: USER
 */

#ifndef INC_SUNROOF_CONTROL_H_
#define INC_SUNROOF_CONTROL_H_

#include <stdio.h>
#include <stdint.h>
#define SUNROOF_CLOSED    0
#define SUNROOF_TILT      1
#define SUNROOF_OPEN      2
#define SUNROOF_STOP      3
#define KEEP_STATE	      10


// 기준값 정의 (상황 따라 조정 가능)
//#define TEMP_AVG          		 24.0f
//#define HUMIDITY_HIGH      		 70.0f
//#define LIGHT_THRESHOLD    		 800
//#define VELOCITY_THRESHOLD       60.0f
//#define AIR_WORST_THRESHOLD      1.4f    // 종합 AQI 기준 매우 나쁨
//#define AIR_BAD_THRESHOLD  		 1.2f    // 나쁨
//#define AIR_SOSO_THRESHOLD       1.0f    // 보통
//#define AIR_GOOD_THRESHOLD 		 0.8f    // 좋음

 typedef struct
 {
       float temp_threshold;
       float humi_threshold;
       float light_threshold;
       float velocity_threshold;
       float air_worst_threshold;
       float air_bad_threshold;
       float air_soso_threshold;
       float air_good_threshold;
       float aqi_in_threshold;
 }SensingThreshold_t;

typedef struct {
    float benzene_in;
    float co_in;
    float co2_in;
    float smoke_in;

    float benzene_out;
    float co_out;
    float co2_out;
    float smoke_out;
    float pm25_out;

    int light;
    int rain_detected;
    float velocity;

    uint8_t current_state;
} SunroofInput_t;

extern SunroofInput_t sunroof_input;
extern SensingThreshold_t thershold_input;
extern uint8_t in_out_mode;
extern float current_in_di;
extern float current_out_di;
extern char buf[128];
// 정규화 함수
float Normalize(float value, float threshold);

// 공기질 가중 합 지수 계산 함수

float Calculate_WeightedAirQualityIndex_In(float benzene, float co, float co2, float smoke);
float Calculate_WeightedAirQualityIndex_Out(float benzene, float co, float co2, float smoke, float pm25);

uint8_t Smart_Sunroof_Control(SunroofInput_t input);
uint8_t User_Sunroof_Control(uint8_t command, uint8_t current_state);

#endif /* INC_SUNROOF_CONTROL_H_ */
