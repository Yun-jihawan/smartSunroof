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

#define MOVE_SPEED 70
#define CLOSE 0
#define TILTING 1
#define OPEN 2
#define STOP 3

#define CW 1
#define ACW 0

#define ROOF_OPEN_MAX 6000
#define ROOF_TILTING_MAX 3000
#define ROOF_CLOSE 0
#define Margin 300


extern volatile int32_t encoder;


void HAL_GPIO_EXTI_Callback(uint16_t GPIO_Pin);
void Sunroof_Set(uint8_t mode);

#endif /* INC_ROOF_H_ */
