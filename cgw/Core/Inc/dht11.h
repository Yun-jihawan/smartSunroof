#ifndef DHT11_H_
#define DHT11_H_

#include "stm32l073xx.h"

typedef struct
{
    GPIO_TypeDef *port;
    uint16_t      pin;
    uint8_t       Rh_byte1;
    uint8_t       Rh_byte2;
    uint8_t       Temp_byte1;
    uint8_t       Temp_byte2;
    uint8_t       SUM;
} dht11_sensor_t;

typedef struct
{
    uint8_t rh;
    uint8_t temp;
} dht11_data_t;

void DHT_Init(dht11_sensor_t *sensors);
void DHT_Read(dht11_sensor_t *sensors, dht11_data_t *data);

#endif /* DHT11_H_ */
