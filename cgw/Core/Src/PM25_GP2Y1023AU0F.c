#include "PM25_GP2Y1023AU0F.h"

#include "debug.h"
#include "main.h"
#include "tim.h"

static uint32_t pulseIn(GPIO_TypeDef *port,
                        uint16_t      pin,
                        GPIO_PinState state,
                        uint32_t      timeout_us)
{
    uint32_t start         = micros();
    uint32_t timeout_start = start;

    // 1. 기다림: 신호가 원하는 상태로 바뀔 때까지
    while (HAL_GPIO_ReadPin(port, pin) != state)
    {
        if ((micros() - timeout_start) > timeout_us)
            return 0; // timeout
    }

    // 2. 원하는 상태가 시작된 시간 기록
    start = micros();

    // 3. 신호가 원하는 상태를 유지하는 동안 대기
    while (HAL_GPIO_ReadPin(port, pin) == state)
    {
        if ((micros() - start) > timeout_us)
            return 0; // timeout
    }

    // 4. 펄스 지속 시간(us) 반환
    return micros() - start;
}

// 센서 스캔 함수
static void sharp_dust_sensor_scan(sharp_dust_sensor_t *sensor)
{
    uint32_t duration_us = pulseIn(sensor->port,
                                   sensor->pin,
                                   GPIO_PIN_RESET,
                                   100000); // 최대 100ms 대기
#if (DEBUG_LEVEL > 0)
    printf("\r\n=== PM2.5 Sensor ===\r\n");
    printf("duration_us: %lu\r\n", duration_us);
#endif
    float duration_ms = duration_us / 1000.0f;

    // 보정식 (원래 아두이노 코드 기반)
    float calculated_ms = duration_ms * 1.155f + 0.194f;

    float AD_value = (calculated_ms / 7.0f) * 5000.0f * 1.102f * 1.059f;
    if (AD_value < 1000.0f)
        AD_value = 1000.0f;

    float dust = (AD_value * 1.10f - 1000.0f) * 0.075f * 6.200f;
    if (dust > 100.0f)
        dust /= 10.0f;
    dust /= 10.0f;

    sensor->concentration = dust;
}

// 초기화 함수: 센서의 핀과 포트를 설정합니다.
void PM_Init(sharp_dust_sensor_t *sensor)
{
    sensor->port          = PM2_5_GPIO_Port;
    sensor->pin           = PM2_5_Pin;
    sensor->concentration = 0.0f;

    GPIO_InitTypeDef GPIO_InitStruct = {0};
    GPIO_InitStruct.Pin              = sensor->pin;
    GPIO_InitStruct.Mode             = GPIO_MODE_INPUT;
    GPIO_InitStruct.Pull             = GPIO_NOPULL;
    HAL_GPIO_Init(sensor->port, &GPIO_InitStruct);
}

void PM_Read(sharp_dust_sensor_t *sensor, pm25_data_t *pm)
{
    sharp_dust_sensor_scan(sensor);
    *pm = sensor->concentration;

#if (DEBUG_LEVEL > 0)
    // 농도 출력
    printf("Dust concentration: %.2f ugram/m^3\r\n", *pm);
#endif
}
