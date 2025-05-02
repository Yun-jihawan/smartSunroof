 #ifndef INC_CAR_DEVICE_CONTROL_H_
 #define INC_CAR_DEVICE_CONTROL_H_
 
 #include <stdio.h>
 #include <stdint.h>
 #include "gpio.h"
 
 extern float current_in_di1;
 extern float current_out_di1;
 
 float calculate_discomfort_index(float temperature, float humidity);
 void operation_conditioner(uint8_t season, uint8_t operate, float temp);
 uint8_t smart_device_command(float temp_in, float temp_out, float temp_user, float humi_in, float humi_out);
 void user_device_command(uint8_t season, uint8_t operate, uint8_t temp_user);
 void fan_on(uint16_t GPIO_Pin);
 void fan_off(uint16_t GPIO_Pin);
 #endif /* INC_CAR_DEVICE_CONTROL_H_ */
 