#ifndef INC_SUNROOF_CONTROL_H_
#define INC_SUNROOF_CONTROL_H_

#include "PM25_GP2Y1023AU0F.h"
#include "mq135.h"

#include <stdint.h>
#include <stdio.h>

#define SUNROOF_CLOSED 0
#define SUNROOF_TILT   1
#define SUNROOF_OPEN   2
#define SUNROOF_STOP   3
#define KEEP_STATE     10

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

typedef struct
{
    mq135_data_t aq[2];
    float        pm;

    int   light;
    int   rain_detected;
    float velocity;

    uint8_t current_state;
} SunroofInput_t;

extern SunroofInput_t     sunroof_input;
extern SensingThreshold_t thershold_input;
extern uint8_t            in_out_mode;
extern float              current_in_di;
extern float              current_out_di;

uint8_t Smart_Sunroof_Control(SunroofInput_t   *input,
                              air_dust_level_t *air_dust_level);
uint8_t User_Sunroof_Control(uint8_t command, uint8_t current_state);

#endif /* INC_SUNROOF_CONTROL_H_ */
