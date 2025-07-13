#ifndef _UART_FROM_CGW_TO_RPI_
#define _UART_FROM_CGW_TO_RPI_

#include "main.h"

#define THRES_IDENTIFIER   0x55
#define CONTROL_IDENTIFIER 0xAA

#define RECEIVED_MASK 0x01

typedef struct
{
    uint8_t temp_threshold;
    uint8_t air_quality_threshold;
    uint8_t dust_threshold;
    uint8_t light_threshold;
} ThresholdData_from_rpi_t;

void send_device_state(sunroof_t *sunroof);
void send_sensor_data(dht11_data_t *dht11, air_dust_level_t *air_dust_state);

#endif /*_UART_FROM_CGW_TO_RPI_*/
