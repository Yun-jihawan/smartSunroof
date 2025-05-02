/*
 * roof.c
 *
 *  Created on: May 1, 2025
 *      Author: USER
 */

#include "roof.h"


void HAL_GPIO_EXTI_Callback(uint16_t GPIO_Pin) {
	uint8_t A_state = 0;
	uint8_t B_state = 0;

	if(GPIO_Pin == ENC_A_Pin) {
		A_state = HAL_GPIO_ReadPin(ENC_A_GPIO_Port, ENC_A_Pin);
		B_state = HAL_GPIO_ReadPin(ENC_B_GPIO_Port, ENC_B_Pin);
		encoder += ((A_state == B_state) ? 1 : -1);
	}
	if(GPIO_Pin == ENC_B_Pin) {
		A_state = HAL_GPIO_ReadPin(ENC_A_GPIO_Port, ENC_A_Pin);
		B_state = HAL_GPIO_ReadPin(ENC_B_GPIO_Port, ENC_B_Pin);
		encoder += ((A_state == B_state) ? -1 : 1);
	}
}

void Sunroof_Set(uint8_t mode) {
	switch(mode) {
	case OPEN:
		if(encoder <= ROOF_MAX) {
			__HAL_TIM_SET_COMPARE(&htim2, TIM_CHANNEL_2, MOVE_SPEED);
			HAL_GPIO_WritePin(ROOF_BRAKE_GPIO_Port, ROOF_BRAKE_Pin, 0);
			HAL_GPIO_WritePin(ROOF_DIR_GPIO_Port, ROOF_DIR_Pin, CW);
		}
		else {
			__HAL_TIM_SET_COMPARE(&htim2, TIM_CHANNEL_2, 0);
			HAL_GPIO_WritePin(ROOF_BRAKE_GPIO_Port, ROOF_BRAKE_Pin, 1);
		}
		break;
	case CLOSE:
		if(encoder >= 0) {
			__HAL_TIM_SET_COMPARE(&htim2, TIM_CHANNEL_2, MOVE_SPEED);
			HAL_GPIO_WritePin(ROOF_BRAKE_GPIO_Port, ROOF_BRAKE_Pin, 0);
			HAL_GPIO_WritePin(ROOF_DIR_GPIO_Port, ROOF_DIR_Pin, ACW);
		}
		else {
			__HAL_TIM_SET_COMPARE(&htim2, TIM_CHANNEL_2, 0);
			HAL_GPIO_WritePin(ROOF_BRAKE_GPIO_Port, ROOF_BRAKE_Pin, 1);
		}
		break;
	case STOP:
	default:
	    __HAL_TIM_SET_COMPARE(&htim2, TIM_CHANNEL_2, 0);
	    HAL_GPIO_WritePin(ROOF_BRAKE_GPIO_Port, ROOF_BRAKE_Pin, 1);
		break;
	}
}
