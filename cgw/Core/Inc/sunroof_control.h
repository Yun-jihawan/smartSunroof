#ifndef INC_SUNROOF_CONTROL_H_
#define INC_SUNROOF_CONTROL_H_

#include "main.h"

#include <stdint.h>
#include <stdio.h>

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
} SensingThreshold_t;

extern SensingThreshold_t threshold_input;
extern uint8_t            in_out_mode;
extern float              current_in_di;
extern float              current_out_di;

uint8_t Smart_Sunroof_Control(sunroof_t *sunroof);
uint8_t User_Sunroof_Control(uint8_t command, uint8_t current_state);

#endif /* INC_SUNROOF_CONTROL_H_ */
