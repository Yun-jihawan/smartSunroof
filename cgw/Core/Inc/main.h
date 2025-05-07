/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * @file           : main.h
  * @brief          : Header for main.c file.
  *                   This file contains the common defines of the application.
  ******************************************************************************
  * @attention
  *
  * Copyright (c) 2025 STMicroelectronics.
  * All rights reserved.
  *
  * This software is licensed under terms that can be found in the LICENSE file
  * in the root directory of this software component.
  * If no LICENSE file comes with this software, it is provided AS-IS.
  *
  ******************************************************************************
  */
/* USER CODE END Header */

/* Define to prevent recursive inclusion -------------------------------------*/
#ifndef __MAIN_H
#define __MAIN_H

#ifdef __cplusplus
extern "C" {
#endif

/* Includes ------------------------------------------------------------------*/
#include "stm32l0xx_hal.h"

/* Private includes ----------------------------------------------------------*/
/* USER CODE BEGIN Includes */
#include "PM25_GP2Y1023AU0F.h"
#include "dht11.h"
#include "event_groups.h"
#include "mq135.h"
#include "state.h"
/* USER CODE END Includes */

/* Exported types ------------------------------------------------------------*/
/* USER CODE BEGIN ET */
typedef struct
{
  dht11_data_t dht[2];
  mq135_data_t aq[2];
  pm25_data_t  pm;

  uint16_t     illum;
  uint8_t      rain;
  uint8_t      velocity;
} sensor_data_t;

typedef struct
{
  sensor_data_t data;
  system_state_t state;
  air_dust_level_t air_dust_level;
  EventGroupHandle_t xSensorEventGroup;
} sunroof_t;
/* USER CODE END ET */

/* Exported constants --------------------------------------------------------*/
/* USER CODE BEGIN EC */

/* USER CODE END EC */

/* Exported macro ------------------------------------------------------------*/
/* USER CODE BEGIN EM */

/* USER CODE END EM */

/* Exported functions prototypes ---------------------------------------------*/
void Error_Handler(void);

/* USER CODE BEGIN EFP */

/* USER CODE END EFP */

/* Private defines -----------------------------------------------------------*/
#define B1_Pin GPIO_PIN_13
#define B1_GPIO_Port GPIOC
#define MCO_Pin GPIO_PIN_0
#define MCO_GPIO_Port GPIOH
#define DHT11_EXTERNAL_Pin GPIO_PIN_1
#define DHT11_EXTERNAL_GPIO_Port GPIOC
#define AQ_INTERNAL_Pin GPIO_PIN_0
#define AQ_INTERNAL_GPIO_Port GPIOA
#define AQ_EXTERNAL_Pin GPIO_PIN_1
#define AQ_EXTERNAL_GPIO_Port GPIOA
#define PM2_5_Pin GPIO_PIN_4
#define PM2_5_GPIO_Port GPIOA
#define LD2_Pin GPIO_PIN_5
#define LD2_GPIO_Port GPIOA
#define DHT11_INTERNAL_Pin GPIO_PIN_0
#define DHT11_INTERNAL_GPIO_Port GPIOB
#define TMS_Pin GPIO_PIN_13
#define TMS_GPIO_Port GPIOA
#define TCK_Pin GPIO_PIN_14
#define TCK_GPIO_Port GPIOA

/* USER CODE BEGIN Private defines */

/* USER CODE END Private defines */

#ifdef __cplusplus
}
#endif

#endif /* __MAIN_H */
