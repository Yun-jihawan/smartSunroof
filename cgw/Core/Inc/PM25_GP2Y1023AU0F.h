#ifndef __SHARP_DUST_SENSOR_H
#define __SHARP_DUST_SENSOR_H

#include "stm32l073xx.h"

typedef float pm25_data_t;
typedef struct
{
    GPIO_TypeDef *port;
    uint16_t      pin;
    float         concentration;
} sharp_dust_sensor_t;

void PM_Init(sharp_dust_sensor_t *sensor);
void PM_Read(sharp_dust_sensor_t *sensor, pm25_data_t *pm);

#endif
