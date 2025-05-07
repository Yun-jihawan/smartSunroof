#ifndef INC_CAR_DEVICE_CONTROL_H_
#define INC_CAR_DEVICE_CONTROL_H_

#include "gpio.h"

#include <stdint.h>

uint8_t smart_device_command(dht11_data_t *dht, uint8_t temp_user);
void    user_device_command(uint8_t season, uint8_t operate, uint8_t temp_user);

#endif /* INC_CAR_DEVICE_CONTROL_H_ */
