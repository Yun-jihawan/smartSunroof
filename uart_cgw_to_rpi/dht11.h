#ifndef DHT11_H_
#define DHT11_H_

#include "stm32l073xx.h"

typedef struct
{
    GPIO_TypeDef *port;
    uint16_t pin;
    uint8_t Rh_byte1;
    uint8_t Rh_byte2;
    uint8_t Temp_byte1;
    uint8_t Temp_byte2;
    uint8_t SUM;
} dht11_sensor_t;

typedef struct
{
    uint8_t internal_rh;
    uint8_t internal_temp;
    uint8_t external_rh;
    uint8_t external_temp;
} dht11_data_t;

#endif /* DHT11_H_ */
