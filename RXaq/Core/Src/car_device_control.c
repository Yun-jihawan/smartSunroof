/*
 * car_device_control.c
 *
 *  Created on: May 1, 2025
 *      Author: USER
 */

#include "car_device_control.h"


float current_in_di1;
float current_out_di1;
char buf1[128];

float calculate_discomfort_index(float temperature, float humidity) {
    return (0.81f * temperature + 0.01f * humidity * (0.99f * temperature - 14.3f) + 46.3f);
}
void fan_on(uint8_t in_out_mode)
{
	if (in_out_mode == 0)
	{
		// 내기모드
	}
	else
	{
		// 외기모드
	}
    HAL_GPIO_WritePin(GPIOA, GPIO_PIN_1, GPIO_PIN_RESET); // IN LOW → 릴레이 작동 → 팬 ON

}
void fan_off(uint16_t in_out_mode)
{
	if (in_out_mode == 0)
		{
			// 내기모드
		}
		else
		{
			// 외기모드
		}
    HAL_GPIO_WritePin(GPIOA, GPIO_PIN_1, GPIO_PIN_SET); // IN HIGH → 릴레이 끊김 → 팬 OFF
}
void conditioner_on(uint16_t GPIO_Pin)
{
	HAL_GPIO_WritePin(GPIOA, GPIO_Pin, GPIO_PIN_SET);
    //HAL_GPIO_WritePin(GPIOA, GPIO_PIN_1, GPIO_PIN_RESET); // IN LOW → 릴레이 작동 → 팬 ON
    //printf("ON \r\n");
}

void conditioner_off(uint16_t GPIO_Pin)
{
	HAL_GPIO_WritePin(GPIOA, GPIO_Pin, GPIO_PIN_RESET);
    //HAL_GPIO_WritePin(GPIOA, GPIO_PIN_1, GPIO_PIN_SET); // IN HIGH → 릴레이 끊김 → 팬 OFF
}
void operation_conditioner(uint8_t season, uint8_t operate, float temp)
{
	if (season == 0)
	{
		// 에어컨 on/off 구현, 파란불 LED 에어컨
		if (operate == 1)
		{
			conditioner_on(GPIO_PIN_6);
			snprintf(buf1, sizeof(buf1), "air conditioner ON");
		}
		else
		{
			conditioner_off(GPIO_PIN_6);
			snprintf(buf1, sizeof(buf1), "air conditioner OFF");
		}

	}
	else
	{
		// 히터 on/off 구현, 빨간색 LED 히터
		if (operate == 1)
		{
			conditioner_on(GPIO_PIN_7);
			snprintf(buf1, sizeof(buf1), "heater ON");
		}
		else
		{
			conditioner_off(GPIO_PIN_7);
			snprintf(buf1, sizeof(buf1), "heater OFF");
		}

	}
}
uint8_t smart_device_command(float temp_in, float temp_out, float temp_user, float humi_in, float humi_out)
{
	//float relative_humi = ((21.7f - 0.81f*temp_user) / (0.0099f*temp_user - 0.143f)); //유저가 설정한 온도로 구한 상대습도
	current_in_di1 = calculate_discomfort_index(temp_in, humi_in); // 현재 내부 불쾌지수
	current_out_di1 = calculate_discomfort_index(temp_out, humi_out); // 현재 외부 불쾌지수

	uint8_t season = 0; // 0 == 여름, 1 == 겨울

	if (temp_out < 10.0f)
	{
		season = 1; // 외부 온도가 10도보나 낮으면 히터를 사용하는 계절로 인식

		// 차량 내부 온도가 사용자가 설정한 온도보다 낮으면 히터를 사용자가 원하는 온도로 on
		if (temp_in < temp_user)
		{
			if (HAL_GPIO_ReadPin(GPIOA, GPIO_PIN_7) == GPIO_PIN_SET) // 이미 히터가 켜져있으면 아무작업 x
			{
				printf("heater is running\r\n");
			}
			else // 온도가 낮은데 히터가 안켜져 있으면 히터 키기
			{
				operation_conditioner(season, 1, temp_user);
				return season;
			}

		}
		else
		{
			snprintf(buf1, sizeof(buf1), "car temp is good");
		}

	}

	//season이 여름으로 결정됐으므로 여름은 불쾌지수로 판단
	else if (current_in_di1 > 68)
	{
		if (HAL_GPIO_ReadPin(GPIOA, GPIO_PIN_6) == GPIO_PIN_SET)
		{
			printf("air conditioner is running\r\n");
		}
		else
		{
			operation_conditioner(season, 1, temp_user);
			return season;
		}

	}
	else
	{
		snprintf(buf1, sizeof(buf1), "car DI is good");
	}

    return KEEP_STATE;
}

void user_device_command(uint8_t season, uint8_t operate, uint8_t temp_user)
{
	operation_conditioner(season, operate, temp_user);
}
