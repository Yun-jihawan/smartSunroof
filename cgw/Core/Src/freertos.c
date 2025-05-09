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
#include "car_device_control.h"
#include "event_groups.h"
#include "sunroof_control.h"
#include "transparency.h"
#include "usart.h"
/* USER CODE END Includes */

/* Private typedef -----------------------------------------------------------*/
/* USER CODE BEGIN PTD */

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
static uint8_t rx_sensor_sun = 0;
static EventGroupHandle_t xSensorEventGroup;
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
  static sunroof_t sunroof;

  xSensorEventGroup = xEventGroupCreate();
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
  SensorReadTaskHandle = osThreadNew(StartSensorReadTask, (void *)&sunroof, &SensorReadTask_attributes);

  /* creation of DataHandlerTask */
  DataHandlerTaskHandle = osThreadNew(StartDataHandlerTask, (void *)&sunroof, &DataHandlerTask_attributes);

  /* USER CODE BEGIN RTOS_THREADS */
  /* add threads, ... */
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
/* USER CODE END Header_StartSensorReadTask */
void StartSensorReadTask(void *argument)
{
  /* USER CODE BEGIN StartSensorReadTask */
  sunroof_t *sunroof = (sunroof_t *)argument;
  sensor_data_t *data = &sunroof->data;

  dht11_sensor_t      dht_sensors[2];
  mq135_sensor_t      aq_sensors[2];
  sharp_dust_sensor_t dust_sensor;

  DHT_Init(dht_sensors);
  AQ_Init(aq_sensors);
  PM_Init(&dust_sensor);

  /* Infinite loop */
  for(;;)
  {
    __disable_irq();
    DHT_Read(dht_sensors, data->dht);
    AQ_Read(aq_sensors, data->aq);
    PM_Read(&dust_sensor, &data->pm);
    __enable_irq();

    xEventGroupSetBits(xSensorEventGroup, DATA_READY_EVENT);
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
  sunroof_t *sunroof = (sunroof_t *)argument;
  sensor_data_t *data = &sunroof->data;
  system_state_t *state = &sunroof->state;

  /* Infinite loop */
  for(;;)
  {
    // 이벤트 대기
    xEventGroupWaitBits(xSensorEventGroup,
      DATA_READY_EVENT,
      pdTRUE, // 이벤트 비트 자동 클리어
      pdTRUE, // 모든 비트 필요
      portMAX_DELAY);

    if (state->mode == MODE_MANUAL)
    {
      // user_device_command();
      // User_Sunroof_Control();
    }
    else if (state->mode == MODE_SMART)
    {
      // smart_device_command();
      Smart_Sunroof_Control(sunroof);
    }
    calculate_transparency(state->mode, data->illum, 0);
  }
  /* USER CODE END StartDataHandlerTask */
}

/* Private application code --------------------------------------------------*/
/* USER CODE BEGIN Application */
void HAL_UART_RxCpltCallback(UART_HandleTypeDef *huart)
{
    if (huart->Instance == USART1)
    {
        Receive_Sensor_Data_SUN_to_CGW(rx_buf);
       	rx_sensor_sun = 1;  // 센서 값 수신 완료
       	HAL_UART_Receive_DMA(&huart1, rx_buf, sizeof(rx_buf));
    }
}

void HAL_UART_TxCpltCallback(UART_HandleTypeDef *huart)
{

}
/* USER CODE END Application */

