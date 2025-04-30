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
/* Definitions for aqReaderTask */
osThreadId_t aqReaderTaskHandle;
const osThreadAttr_t aqReaderTask_attributes = {
  .name = "aqReaderTask",
  .stack_size = 128 * 4,
  .priority = (osPriority_t) osPriorityNormal,
};
/* Definitions for pmReaderTask */
osThreadId_t pmReaderTaskHandle;
const osThreadAttr_t pmReaderTask_attributes = {
  .name = "pmReaderTask",
  .stack_size = 128 * 4,
  .priority = (osPriority_t) osPriorityLow,
};
/* Definitions for dhtReaderTask */
osThreadId_t dhtReaderTaskHandle;
const osThreadAttr_t dhtReaderTask_attributes = {
  .name = "dhtReaderTask",
  .stack_size = 128 * 4,
  .priority = (osPriority_t) osPriorityLow,
};

/* Private function prototypes -----------------------------------------------*/
/* USER CODE BEGIN FunctionPrototypes */

/* USER CODE END FunctionPrototypes */

void StartAqReaderTask(void *argument);
void StartPmReaderTask(void *argument);
void StartDhtReaderTask(void *argument);

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
  /* creation of aqReaderTask */
  aqReaderTaskHandle = osThreadNew(StartAqReaderTask, NULL, &aqReaderTask_attributes);

  /* creation of pmReaderTask */
  pmReaderTaskHandle = osThreadNew(StartPmReaderTask, NULL, &pmReaderTask_attributes);

  /* creation of dhtReaderTask */
  dhtReaderTaskHandle = osThreadNew(StartDhtReaderTask, NULL, &dhtReaderTask_attributes);

  /* USER CODE BEGIN RTOS_THREADS */
  /* add threads, ... */
  /* USER CODE END RTOS_THREADS */

  /* USER CODE BEGIN RTOS_EVENTS */
  /* add events, ... */
  /* USER CODE END RTOS_EVENTS */

}

/* USER CODE BEGIN Header_StartAqReaderTask */
/**
  * @brief  Function implementing the aqReaderTask thread.
  * @param  argument: Not used
  * @retval None
  */
/* USER CODE END Header_StartAqReaderTask */
void StartAqReaderTask(void *argument)
{
  /* USER CODE BEGIN StartAqReaderTask */
  /* Infinite loop */
  for(;;)
  {
    osDelay(1);
  }
  /* USER CODE END StartAqReaderTask */
}

/* USER CODE BEGIN Header_StartPmReaderTask */
/**
* @brief Function implementing the pmReaderTask thread.
* @param argument: Not used
* @retval None
*/
/* USER CODE END Header_StartPmReaderTask */
void StartPmReaderTask(void *argument)
{
  /* USER CODE BEGIN StartPmReaderTask */
  /* Infinite loop */
  for(;;)
  {
    osDelay(1);
  }
  /* USER CODE END StartPmReaderTask */
}

/* USER CODE BEGIN Header_StartDhtReaderTask */
/**
* @brief Function implementing the dhtReaderTask thread.
* @param argument: Not used
* @retval None
*/
/* USER CODE END Header_StartDhtReaderTask */
void StartDhtReaderTask(void *argument)
{
  /* USER CODE BEGIN StartDhtReaderTask */
  /* Infinite loop */
  for(;;)
  {
    osDelay(1);
  }
  /* USER CODE END StartDhtReaderTask */
}

/* Private application code --------------------------------------------------*/
/* USER CODE BEGIN Application */

/* USER CODE END Application */

