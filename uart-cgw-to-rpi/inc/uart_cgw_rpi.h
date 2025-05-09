#ifndef _UART_FROM_CGW_TO_RPI_
#define _UART_FROM_CGW_TO_RPI_

#include "state.h"
#include "dht11.h"
#include "usart.h"

#define THRES_IDENTIFIER   0xFF
#define CONTROL_IDENTIFIER 0xFE

#define RECEIVED_MASK 0x01

typedef struct {
    uint8_t temp_threshold;
    uint8_t air_quality_threshold;
    uint8_t dust_threshold;
    uint8_t light_threshold;
} ThresholdData_from_rpi_t;

typedef struct {
    uint8_t roof;     // 0~3
    uint8_t transparency;
    uint8_t air_control;
    control_mode_t mode;
} ControlData_from_rpit;

extern ThresholdData_from_rpi_t thresholdData;
extern ControlData_from_rpit controlData;

void send_device_state(state_cgw_to_pi_t *stat);
void send_sensor_data(dht11_data_t *dht11, air_dust_level_t *air_dust_stat);
void send_cgw_to_rpi_init(void);

#endif /*_UART_FROM_CGW_TO_RPI_*/
