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

/* USER CODE END Includes */

/* Exported types ------------------------------------------------------------*/
/* USER CODE BEGIN ET */

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
#define B1_EXTI_IRQn EXTI4_15_IRQn
#define MCO_Pin GPIO_PIN_0
#define MCO_GPIO_Port GPIOH
#define ROOF_ENC_A_Pin GPIO_PIN_0
#define ROOF_ENC_A_GPIO_Port GPIOC
#define ROOF_ENC_A_EXTI_IRQn EXTI0_1_IRQn
#define ROOF_ENC_B_Pin GPIO_PIN_1
#define ROOF_ENC_B_GPIO_Port GPIOC
#define ROOF_ENC_B_EXTI_IRQn EXTI0_1_IRQn
#define OUT_ILLUMI_Pin GPIO_PIN_0
#define OUT_ILLUMI_GPIO_Port GPIOA
#define IN_ILLUMI_Pin GPIO_PIN_1
#define IN_ILLUMI_GPIO_Port GPIOA
#define USART_TX_Pin GPIO_PIN_2
#define USART_TX_GPIO_Port GPIOA
#define USART_RX_Pin GPIO_PIN_3
#define USART_RX_GPIO_Port GPIOA
#define TILTING_DIR_Pin GPIO_PIN_5
#define TILTING_DIR_GPIO_Port GPIOA
#define ROOF_DIR_Pin GPIO_PIN_6
#define ROOF_DIR_GPIO_Port GPIOA
#define RAIN_Pin GPIO_PIN_5
#define RAIN_GPIO_Port GPIOC
#define TILTING_ENC_A_Pin GPIO_PIN_10
#define TILTING_ENC_A_GPIO_Port GPIOB
#define TILTING_ENC_A_EXTI_IRQn EXTI4_15_IRQn
#define TILTING_ENC_B_Pin GPIO_PIN_11
#define TILTING_ENC_B_GPIO_Port GPIOB
#define TILTING_ENC_B_EXTI_IRQn EXTI4_15_IRQn
#define ROOF_BRAKE_Pin GPIO_PIN_7
#define ROOF_BRAKE_GPIO_Port GPIOC
#define TILTING_BRAKE_Pin GPIO_PIN_9
#define TILTING_BRAKE_GPIO_Port GPIOA
#define TMS_Pin GPIO_PIN_13
#define TMS_GPIO_Port GPIOA
#define TCK_Pin GPIO_PIN_14
#define TCK_GPIO_Port GPIOA

/* USER CODE BEGIN Private defines */
#define VDDA_VREFINT_CAL            ((uint32_t) 3000)
#define VREFINT_CAL       ((uint16_t*) ((uint32_t) 0x1FF80078))
/* USER CODE END Private defines */

#ifdef __cplusplus
}
#endif

#endif /* __MAIN_H */
