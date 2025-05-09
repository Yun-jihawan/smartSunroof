#include "uart_cgw_rpi.h"

#include "usart.h"

void send_device_state(sunroof_t *sunroof)
{
    sensor_data_t  *data  = &sunroof->data;
    system_state_t *state = &sunroof->state;

    uint8_t buffer[8];

    // 전송 포맷
    // [ID, 선루프 상태, 투명도, 에어컨, 자동/수동모드, 속도, 0x00, 0x00]
    buffer[0] = (uint8_t)THRES_IDENTIFIER;
    buffer[1] = (uint8_t)state->roof;
    buffer[2] = (uint8_t)state->transparency;
    buffer[3] = (uint8_t)state->airconditioner;
    buffer[4] = (uint8_t)state->mode;
    buffer[5] = (uint8_t)data->velocity;
    buffer[6] = (uint8_t)0x00;
    buffer[7] = (uint8_t)0x00;

    HAL_UART_Transmit(&huart4, buffer, sizeof(buffer), HAL_MAX_DELAY);
}

void send_sensor_data(dht11_data_t *dht11, air_dust_level_t *air_dust_state)
{
    uint8_t buffer[8];

    // 전송 포맷
    // [ID, 내부 온도, 내부 습도, 외부 온도, 외부 습도,
    //  내부 공기질, 외부 공기질, 미세먼지]
    buffer[0] = (uint8_t)CONTROL_IDENTIFIER;
    buffer[1] = (uint8_t)dht11[IN].temp;
    buffer[2] = (uint8_t)dht11[IN].rh;
    buffer[3] = (uint8_t)dht11[OUT].temp;
    buffer[4] = (uint8_t)dht11[OUT].rh;
    buffer[5] = (uint8_t)air_dust_state->internal_air_quality_state;
    buffer[6] = (uint8_t)air_dust_state->external_air_quality_state;
    buffer[7] = (uint8_t)air_dust_state->dust_state;

    HAL_UART_Transmit(&huart4, buffer, sizeof(buffer), HAL_MAX_DELAY);
}
