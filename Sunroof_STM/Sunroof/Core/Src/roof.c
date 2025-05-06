/*
 * roof.c
 *
 *  Created on: May 1, 2025
 *      Author: USER
 */

#include "roof.h"


void HAL_GPIO_EXTI_Callback(uint16_t GPIO_Pin) {
	uint8_t ROOF_A_state = 0;
	uint8_t ROOF_B_state = 0;
	uint8_t TILTING_A_state = 0;
	uint8_t TILTING_B_state = 0;

	//Roof Motor
	if(GPIO_Pin == ROOF_ENC_A_Pin) {
		ROOF_A_state = HAL_GPIO_ReadPin(ROOF_ENC_A_GPIO_Port, ROOF_ENC_A_Pin);
		ROOF_B_state = HAL_GPIO_ReadPin(ROOF_ENC_B_GPIO_Port, ROOF_ENC_B_Pin);
		roof_encoder += ((ROOF_A_state == ROOF_B_state) ? 1 : -1);
	}
	if(GPIO_Pin == ROOF_ENC_B_Pin) {
		ROOF_A_state = HAL_GPIO_ReadPin(ROOF_ENC_A_GPIO_Port, ROOF_ENC_A_Pin);
		ROOF_B_state = HAL_GPIO_ReadPin(ROOF_ENC_B_GPIO_Port, ROOF_ENC_B_Pin);
		roof_encoder += ((ROOF_A_state == ROOF_B_state) ? -1 : 1);
	}

	//Tilting Motor
	if(GPIO_Pin == TILTING_ENC_A_Pin) {
		TILTING_A_state = HAL_GPIO_ReadPin(TILTING_ENC_A_GPIO_Port, TILTING_ENC_A_Pin);
		TILTING_B_state = HAL_GPIO_ReadPin(TILTING_ENC_B_GPIO_Port, TILTING_ENC_B_Pin);
		tilting_encoder += ((TILTING_A_state == TILTING_B_state) ? 1 : -1);
	}
	if(GPIO_Pin == TILTING_ENC_B_Pin) {
		TILTING_A_state = HAL_GPIO_ReadPin(TILTING_ENC_A_GPIO_Port, TILTING_ENC_A_Pin);
		TILTING_B_state = HAL_GPIO_ReadPin(TILTING_ENC_B_GPIO_Port, TILTING_ENC_B_Pin);
		tilting_encoder += ((TILTING_A_state == TILTING_B_state) ? -1 : 1);
	}
}

void Sunroof_Set(uint8_t mode) {
	switch(mode) {
	case OPEN:
		if(roof_encoder <= ROOF_OPEN_MAX) {
			__HAL_TIM_SET_COMPARE(&htim2, TIM_CHANNEL_2, MOVE_SPEED);
			HAL_GPIO_WritePin(ROOF_BRAKE_GPIO_Port, ROOF_BRAKE_Pin, 0);
			HAL_GPIO_WritePin(ROOF_DIR_GPIO_Port, ROOF_DIR_Pin, CW);
		}
		else {
			__HAL_TIM_SET_COMPARE(&htim2, TIM_CHANNEL_2, 0);
			HAL_GPIO_WritePin(ROOF_BRAKE_GPIO_Port, ROOF_BRAKE_Pin, 1);
		}
		break;
	case TILTING:
		if(roof_encoder <= ROOF_TILTING_MAX) {
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
		if(roof_encoder >= ROOF_CLOSE) {
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
