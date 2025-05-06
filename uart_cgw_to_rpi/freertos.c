/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * File Name          : freertos.c
  * Description        : Code for freertos applications
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

/* Includes ------------------------------------------------------------------*/
#include "FreeRTOS.h"
#include "task.h"
#include "main.h"
#include "cmsis_os.h"

/* Private includes ----------------------------------------------------------*/
/* USER CODE BEGIN Includes */
#include "usart.h"
#include <string.h>
#include "PM25_GP2Y1023AU0F.h"
#include "dht11.h"
#include "mq135.h"
#include "cgw_rpi_uart.h"
#include "main.h"

/* USER CODE END Includes */

/* Private typedef -----------------------------------------------------------*/
/* USER CODE BEGIN PTD */

/* USER CODE END PTD */

/* Private define ------------------------------------------------------------*/
/* USER CODE BEGIN PD */

/* USER CODE END PD */

/* Private macro -------------------------------------------------------------*/
/* USER CODE BEGIN PM */

/* USER CODE END PM */

/* Private variables ---------------------------------------------------------*/
/* USER CODE BEGIN Variables */

/* USER CODE END Variables */
osThreadId SendDeviceDataTHandle;
osThreadId SendSensorDataTHandle;

/* Private function prototypes -----------------------------------------------*/
/* USER CODE BEGIN FunctionPrototypes */

/* USER CODE END FunctionPrototypes */

void SendDeviceDataTask(void const * argument);
void SendSensorDataTask(void const * argument);

void MX_FREERTOS_Init(void); /* (MISRA C 2004 rule 8.1) */

/* GetIdleTaskMemory prototype (linked to static allocation support) */
void vApplicationGetIdleTaskMemory( StaticTask_t **ppxIdleTaskTCBBuffer, StackType_t **ppxIdleTaskStackBuffer, uint32_t *pulIdleTaskStackSize );

/* USER CODE BEGIN GET_IDLE_TASK_MEMORY */
static StaticTask_t xIdleTaskTCBBuffer;
static StackType_t xIdleStack[configMINIMAL_STACK_SIZE];

void vApplicationGetIdleTaskMemory( StaticTask_t **ppxIdleTaskTCBBuffer, StackType_t **ppxIdleTaskStackBuffer, uint32_t *pulIdleTaskStackSize )
{
  *ppxIdleTaskTCBBuffer = &xIdleTaskTCBBuffer;
  *ppxIdleTaskStackBuffer = &xIdleStack[0];
  *pulIdleTaskStackSize = configMINIMAL_STACK_SIZE;
  /* place for user code */
}
/* USER CODE END GET_IDLE_TASK_MEMORY */

/**
  * @brief  FreeRTOS initialization
  * @param  None
  * @retval None
  */
void MX_FREERTOS_Init(void) {
  /* USER CODE BEGIN Init */

  /* USER CODE END Init */

  /* USER CODE BEGIN RTOS_MUTEX */
  /* add mutexes, ... */
  /* USER CODE END RTOS_MUTEX */

  /* USER CODE BEGIN RTOS_SEMAPHORES */
  /* add semaphores, ... */
  /* USER CODE END RTOS_SEMAPHORES */

  /* USER CODE BEGIN RTOS_TIMERS */
  /* start timers, add new ones, ... */
  /* USER CODE END RTOS_TIMERS */

  /* USER CODE BEGIN RTOS_QUEUES */
  /* add queues, ... */
  /* USER CODE END RTOS_QUEUES */

  /* Create the thread(s) */
  /* definition and creation of SendDeviceDataT */
  osThreadDef(SendDeviceDataT, SendDeviceDataTask, osPriorityNormal, 0, 256);
  SendDeviceDataTHandle = osThreadCreate(osThread(SendDeviceDataT), NULL);

  /* definition and creation of SendSensorDataT */
  osThreadDef(SendSensorDataT, SendSensorDataTask, osPriorityHigh, 0, 128);
  SendSensorDataTHandle = osThreadCreate(osThread(SendSensorDataT), NULL);

  /* USER CODE BEGIN RTOS_THREADS */
  /* add threads, ... */
  /* USER CODE END RTOS_THREADS */

}

/* USER CODE BEGIN Header_SendDeviceDataTask */
/**
  * @brief  Function implementing the SendDeviceDataT thread.
  * @param  argument: Not used
  * @retval None
  */
/* USER CODE END Header_SendDeviceDataTask */
int __io_putchar(int ch)
{
//    HAL_UART_Transmit(&huart1, (uint8_t*)&ch, 1, HAL_MAX_DELAY);
    HAL_UART_Transmit(&huart2, (uint8_t*)&ch, 1, HAL_MAX_DELAY);
    return ch;
}

void send_sensor_data(dht11_data_t *dht11, air_dust_level_t *air_dust_stat) {
    uint8_t header = 0x00; // 시작 식별자
    uint8_t buffer[1 + sizeof(dht11_data_t) + sizeof(air_dust_level_t)];

    buffer[0] = header;

    memcpy(buffer + 1, dht11, sizeof(dht11_data_t));
    memcpy(buffer + 1 + sizeof(dht11_data_t), air_dust_stat, sizeof(air_dust_level_t));

    HAL_UART_Transmit(&huart1, buffer, sizeof(buffer), HAL_MAX_DELAY);
}

void send_device_state(state_cgw_to_pi_t *state) {
    uint8_t header = 0x01; // 식별자 1
    uint8_t buffer[1 + sizeof(state_cgw_to_pi_t)];

    buffer[0] = header;

    memcpy(buffer + 1, state, sizeof(state_cgw_to_pi_t));

    HAL_UART_Transmit(&huart1, buffer, sizeof(buffer), HAL_MAX_DELAY);
}

// Data Frame = {1, 선루프, 투명도, 에어컨, 환기, 속도}
void SendDeviceDataTask(void const * argument)
{
  /* USER CODE BEGIN SendDeviceDataTask */
  /* Infinite loop */
	state_cgw_to_pi_t device_data;
  for(;;)
  {
	// 임의의 값
	device_data.sunroof_stat = 1;
	device_data.transparency = 0;
	device_data.airconditioner = 0;
	device_data.ventilation_mode = 1;
	device_data.velocity = 60;
	send_device_state(&device_data);
    osDelay(5000);
  }
  /* USER CODE END SendDeviceDataTask */
}

/* USER CODE BEGIN Header_SendSensorDataTask */
/**
* @brief Function implementing the SendSensorDataT thread.
* @param argument: Not used
* @retval None
*/
/* USER CODE END Header_SendSensorDataTask */
// Data Frame = {0, 내부 온도, 내부 습도, 외부 온도, 외부 습도, 내부 공기질, 외부 공기질, 미세먼지}
void SendSensorDataTask(void const * argument)
{
  /* USER CODE BEGIN SendSensorDataTask */
  /* Infinite loop */
  dht11_data_t dht11_data;
  air_dust_level_t mq135_pm25_data;
  for(;;)
  {
	dht11_data.internal_temp = 20;
	dht11_data.internal_rh = 30;
	dht11_data.external_temp = 40;
	dht11_data.external_rh = 50;
	mq135_pm25_data.internal_air_quality_stat = AIR_QUALITY_MODERATE;
	mq135_pm25_data.external_air_quality_stat = AIR_QUALITY_BAD;
	mq135_pm25_data.dust_stat = DUST_VERY_BAD;
	send_sensor_data(&dht11_data, &mq135_pm25_data);
	osDelay(3000);
  }
  /* USER CODE END SendSensorDataTask */
}

/* Private application code --------------------------------------------------*/
/* USER CODE BEGIN Application */

/* USER CODE END Application */

