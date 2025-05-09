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
#include "PM25_GP2Y1023AU0F.h"
#include "dht11.h"
#include "event_groups.h"
#include "mq135.h"
#include "uart_cgw_rpi.h"
/* USER CODE END Includes */

/* Private typedef -----------------------------------------------------------*/
/* USER CODE BEGIN PTD */
typedef struct
{
  dht11_data_t dht[2];
  mq135_data_t aq[2];
  pm25_data_t  pm;

  EventGroupHandle_t xSensorEventGroup;
} sensor_data_t;
/* USER CODE END PTD */

/* Private define ------------------------------------------------------------*/
/* USER CODE BEGIN PD */
#define DATA_READY_EVENT (1 << 0)
/* USER CODE END PD */

/* Private macro -------------------------------------------------------------*/
/* USER CODE BEGIN PM */

/* USER CODE END PM */

/* Private variables ---------------------------------------------------------*/
/* USER CODE BEGIN Variables */

/* USER CODE END Variables */
/* Definitions for SensorReadTask */
osThreadId_t SensorReadTaskHandle;
const osThreadAttr_t SensorReadTask_attributes = {
  .name = "SensorReadTask",
  .stack_size = 128 * 4,
  .priority = (osPriority_t) osPriorityNormal,
};
/* Definitions for DataHandlerTask */
osThreadId_t DataHandlerTaskHandle;
const osThreadAttr_t DataHandlerTask_attributes = {
  .name = "DataHandlerTask",
  .stack_size = 128 * 4,
  .priority = (osPriority_t) osPriorityNormal,
};

/* Private function prototypes -----------------------------------------------*/
/* USER CODE BEGIN FunctionPrototypes */
osThreadId_t SensorTxTaskHandle;
const osThreadAttr_t SensorTxTask_attributes = {
  .name = "SensorTxTask",
  .stack_size = 128 * 4,
  .priority = (osPriority_t) osPriorityNormal,
};
osThreadId_t DeviceStateTxTaskHandle;
const osThreadAttr_t DeviceStateTxTask_attributes = {
  .name = "DeviceStateTxTask",
  .stack_size = 128 * 4,
  .priority = (osPriority_t) osPriorityAboveNormal,
};

void StartSensorTxTask(void *argument);
void StartDeviceStateTxTask(void *argument);
/* USER CODE END FunctionPrototypes */

void StartSensorReadTask(void *argument);
void StartDataHandlerTask(void *argument);

void MX_FREERTOS_Init(void); /* (MISRA C 2004 rule 8.1) */

/* Hook prototypes */
void configureTimerForRunTimeStats(void);
unsigned long getRunTimeCounterValue(void);
void vApplicationStackOverflowHook(xTaskHandle xTask, signed char *pcTaskName);

/* USER CODE BEGIN 1 */
/* Functions needed when configGENERATE_RUN_TIME_STATS is on */
__weak void configureTimerForRunTimeStats(void)
{

}

__weak unsigned long getRunTimeCounterValue(void)
{
return 0;
}
/* USER CODE END 1 */

/* USER CODE BEGIN 4 */
void vApplicationStackOverflowHook(xTaskHandle xTask, signed char *pcTaskName)
{
   /* Run time stack overflow checking is performed if
   configCHECK_FOR_STACK_OVERFLOW is defined to 1 or 2. This hook function is
   called if a stack overflow is detected. */
}
/* USER CODE END 4 */

/**
  * @brief  FreeRTOS initialization
  * @param  None
  * @retval None
  */
void MX_FREERTOS_Init(void) {
  /* USER CODE BEGIN Init */
  static sensor_data_t data;

  data.xSensorEventGroup = xEventGroupCreate();
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
  /* creation of SensorReadTask */
  SensorReadTaskHandle = osThreadNew(StartSensorReadTask, (void *)&data, &SensorReadTask_attributes);

  /* creation of DataHandlerTask */
  DataHandlerTaskHandle = osThreadNew(StartDataHandlerTask, (void *)&data, &DataHandlerTask_attributes);

  /* USER CODE BEGIN RTOS_THREADS */
  /* add threads, ... */
  /* creation of SensorTxTask */
  SensorTxTaskHandle = osThreadNew(StartSensorTxTask, (void *)&data, &SensorTxTask_attributes);

  /* creation of DeviceStateTxTask */
  DeviceStateTxTaskHandle = osThreadNew(StartDeviceStateTxTask, (void *)&data, &DeviceStateTxTask_attributes);

  /* USER CODE END RTOS_THREADS */

  /* USER CODE BEGIN RTOS_EVENTS */
  /* add events, ... */
  /* USER CODE END RTOS_EVENTS */

}

/* USER CODE BEGIN Header_StartSensorReadTask */
/**
  * @brief  Function implementing the SensorReadTask thread.
  * @param  argument: Not used
  * @retval None
  */

int __io_putchar(int ch)
{
//    HAL_UART_Transmit(&huart1, (uint8_t*)&ch, 1, HAL_MAX_DELAY);
    HAL_UART_Transmit(&huart2, (uint8_t*)&ch, 1, HAL_MAX_DELAY);
    return ch;
}
/* USER CODE END Header_StartSensorReadTask */
void StartSensorReadTask(void *argument)
{
  /* USER CODE BEGIN StartSensorReadTask */
  sensor_data_t *data = (sensor_data_t *)argument;

  dht11_sensor_t      dht_sensors[2];
  mq135_sensor_t      aq_sensors[2];
  sharp_dust_sensor_t dust_sensor;

  DHT_Init(dht_sensors);
  AQ_Init(aq_sensors);
  PM_Init(&dust_sensor);

  /* Infinite loop */
  for(;;)
  {
    DHT_Read(dht_sensors, data->dht);
    AQ_Read(aq_sensors, data->aq);
    PM_Read(&dust_sensor, &data->pm);

    xEventGroupSetBits(data->xSensorEventGroup, DATA_READY_EVENT);
    osDelay(5000);
  }
  /* USER CODE END StartSensorReadTask */
}

/* USER CODE BEGIN Header_StartDataHandlerTask */
/**
* @brief Function implementing the DataHandlerTask thread.
* @param argument: Not used
* @retval None
*/
/* USER CODE END Header_StartDataHandlerTask */
void StartDataHandlerTask(void *argument)
{
  /* USER CODE BEGIN StartDataHandlerTask */
  /* Infinite loop */
  for(;;)
  {
    osDelay(1);
  }
  /* USER CODE END StartDataHandlerTask */
}

/* Private application code --------------------------------------------------*/
/* USER CODE BEGIN Application */
// 1초마다 센서 데이터 전송
void StartSensorTxTask(void *argument) {
	sensor_data_t *data = (sensor_data_t *)argument;
  int i = 0;
  for (;;) {
#if 1
	// urat 테스트용 임의의 센서값
	air_dust_level_t mq135_pm25_data;
	data->dht[0].temp = (20+i)%40;
	data->dht[0].rh = (30+i)%100;
	data->dht[1].temp = 40;
	data->dht[1].rh = 50;
	mq135_pm25_data.internal_air_quality_state = AIR_QUALITY_MODERATE;
	mq135_pm25_data.external_air_quality_state = AIR_QUALITY_BAD;
	mq135_pm25_data.dust_state = DUST_VERY_BAD;
	i++;
#endif

    send_sensor_data(data->dht, &mq135_pm25_data);
    osDelay(1000); // 1초 대기
  }
}

// 3초마다 장치 상태 전송
void StartDeviceStateTxTask(void *argument) {
//  sunroof_t *data = (sunroof_t *)argument;

  int i = 0;
  for (;;) {
#if 1
		// urat 테스트용 임의의 상태값
	  	state_cgw_to_pi_t device_data;
		// 임의의 값
		device_data.roof = (1+i)%3;
		device_data.transparency = 50;
		device_data.airconditioner = IN_AIRCOND_ON;
		device_data.mode = 1;
		device_data.velocity = 60;
		i++;
#endif

	printf("mode: %d\r\n", controlData.mode);
	if(controlData.mode == AUTOMATIC) {
		send_device_state(&device_data);
	}
    osDelay(3000); // 3초 대기
  }
}
/* USER CODE END Application */

