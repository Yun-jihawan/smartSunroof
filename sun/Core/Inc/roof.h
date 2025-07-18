/*
 * roof.h
 *
 *  Created on: May 1, 2025
 *      Author: USER
 */

#ifndef INC_ROOF_H_
#define INC_ROOF_H_


#include "main.h"
#include "adc.h"
#include "tim.h"
#include "usart.h"
#include "gpio.h"

#define SLIDE_SPEED 25
#define TILT_SPEED 27

#define CLOSE 0
#define TILTING 1
#define OPEN 2
#define STOP 3

#define CW 1
#define ACW 0

#define ROOF_OPEN_MAX 50
#define ROOF_TILTING_MAX 5
#define ROOF_CLOSE 0


extern volatile int32_t roof_encoder;
extern volatile int32_t tilting_encoder;


void HAL_GPIO_EXTI_Callback(uint16_t GPIO_Pin);
void Sunroof_Set(uint8_t mode);

#endif /* INC_ROOF_H_ */
