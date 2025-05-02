/*
 * sensor.h
 *
 *  Created on: May 1, 2025
 *      Author: USER
 */

#ifndef INC_SENSOR_H_
#define INC_SENSOR_H_


#include "main.h"
#include "adc.h"
#include "tim.h"
#include "usart.h"
#include "gpio.h"


#define RAIN_TH 3200

extern volatile uint16_t in_illum;
extern volatile uint16_t out_illum;
extern volatile uint16_t rain_sense;
extern volatile uint8_t rain_state;

void read_illum(void);
void read_rain(void);

#endif /* INC_SENSOR_H_ */
