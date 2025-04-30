/*
 * dht11_driver.h
 *
 *  Created on: 2025. 2. 18.
 *      Author: jijon
 */

#ifndef DHT11_DRIVER_H_
#define DHT11_DRIVER_H_

//#include "Ifx_Types.h"
#include "IfxPort_PinMap.h"

#define DHT11 IfxPort_P10_4

void delay(uint32_t us);
void DHT11_Start (void);
uint8_t Check_Response (void);
uint8_t DHT11_Read (void);


#endif /* DHT11_DRIVER_H_ */
