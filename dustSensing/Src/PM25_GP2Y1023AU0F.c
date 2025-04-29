#include "PM25_GP2Y1023AU0F.h"

// 초기화 함수: 센서의 핀과 포트를 설정합니다.
void sharp_dust_sensor_init(sharp_dust_sensor_t *sensor, GPIO_TypeDef *port, uint16_t pin)
{
    sensor->port = port;
    sensor->pin = pin;
    sensor->concentration = 0.0f;

    // PA0 핀을 입력 모드로 설정
    GPIO_InitTypeDef GPIO_InitStruct = {0};
    GPIO_InitStruct.Pin = pin;
    GPIO_InitStruct.Mode = GPIO_MODE_INPUT;
    GPIO_InitStruct.Pull = GPIO_NOPULL;
    HAL_GPIO_Init(port, &GPIO_InitStruct);
}

// 센서 스캔 함수
void sharp_dust_sensor_scan(sharp_dust_sensor_t *sensor)
{
    int j = 0;

    // 신호가 LOW일 때 카운트
    while (HAL_GPIO_ReadPin(sensor->port, sensor->pin) != GPIO_PIN_RESET);
    while (HAL_GPIO_ReadPin(sensor->port, sensor->pin) != GPIO_PIN_RESET);

    // 신호가 LOW일 때의 시간 계산
    while (HAL_GPIO_ReadPin(sensor->port, sensor->pin) == GPIO_PIN_RESET)
    {
        j++;
    }

    // 신호가 HIGH일 때
    while (HAL_GPIO_ReadPin(sensor->port, sensor->pin) != GPIO_PIN_RESET);

    float factor1 = 189.926;
    float calculated_ms = ((float)j) / factor1;

    float factor2 = 1.155;
    calculated_ms = calculated_ms * factor2;

    float offset = 0.194;
    calculated_ms = calculated_ms + offset;

    // ADC 값 계산
    float AD_value = (calculated_ms / 7.0) * 5000.0 * 1.102 * 1.059;

    // AD_value가 1000 미만이면 1000으로 설정
    if (AD_value < 1000.0)
    {
        AD_value = 1000.0;
    }

    // 미세먼지 농도 계산 (ug/m^3)
    float dust = (AD_value * 1.10 - 1000.0) * 0.075 * 6.200;

    // 농도가 100 이상이면 10으로 나눔
    if (dust > 100.0)
    {
        dust = dust / 10.0;
    }

    // 농도 값 저장
    sensor->concentration = dust;
}

// 농도 값을 반환하는 함수
float sharp_dust_sensor_get_concentration(sharp_dust_sensor_t *sensor)
{
    return sensor->concentration;
}
