#ifndef __SHARP_DUST_SENSOR_H
#define __SHARP_DUST_SENSOR_H

#include "stm32l0xx_hal.h"

#include <stdio.h>

typedef struct
{
    GPIO_TypeDef *port;
    uint16_t      pin;
    float         concentration;
} sharp_dust_sensor_t;

void  sharp_dust_sensor_init(sharp_dust_sensor_t *sensor,
                             GPIO_TypeDef        *port,
                             uint16_t             pin);
void  sharp_dust_sensor_scan(sharp_dust_sensor_t *sensor);
float sharp_dust_sensor_get_concentration(sharp_dust_sensor_t *sensor);

#endif
