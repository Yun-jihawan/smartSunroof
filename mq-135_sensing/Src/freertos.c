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
#include <stdio.h>
#include "mq135.h"
#include "adc.h"
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

extern MQ135_HandleTypeDef hmq_in;
extern MQ135_HandleTypeDef hmq_out;
extern UART_HandleTypeDef huart1;
extern double benzene_ppm_in;
extern double co_ppm_in;
extern double co2_ppm_in;
extern double smoke_ppm_in;

extern double benzene_ppm_out;
extern double co_ppm_out;
extern double co2_ppm_out;
extern double smoke_ppm_out;

/* USER CODE END Variables */
osThreadId defaultTaskHandle;
osThreadId SensorTaskHandle;

/* Private function prototypes -----------------------------------------------*/
/* USER CODE BEGIN FunctionPrototypes */

/* USER CODE END FunctionPrototypes */

void StartDefaultTask(void const * argument);
void StartSensorTask(void const * argument);

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
	printf("MQ-135 센서 초기화 완료\r\n");
	  MQ135_Init(&hmq_in, &hadc, ADC_CHANNEL_0, MQ135_DEFAULT_AVERAGE, MQ135_DEFAULT_ADC_BITS, MQ135_DEFAULT_VREF);
	  MQ135_SetRL(&hmq_in, 10.0f); // 로드 저항 값 설정 (보통 10k옴)
	  MQ135_SetR0(&hmq_in, 9.83f); // 깨끗한 공기에서의 초기 R0 값 (직접 보정해서 측정하는 게 가장 정확)

	  MQ135_Init(&hmq_out, &hadc, ADC_CHANNEL_1, MQ135_DEFAULT_AVERAGE, MQ135_DEFAULT_ADC_BITS, MQ135_DEFAULT_VREF);
	  MQ135_SetRL(&hmq_out, 10.0f); // 로드 저항 값 설정 (보통 10k옴)
	  MQ135_SetR0(&hmq_out, 9.83f); // 깨끗한 공기에서의 초기 R0 값 (직접 보정해서 측정하는 게 가장 정확)
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
  /* definition and creation of defaultTask */
  osThreadDef(defaultTask, StartDefaultTask, osPriorityNormal, 0, 128);
  defaultTaskHandle = osThreadCreate(osThread(defaultTask), NULL);

  /* definition and creation of SensorTask */
  osThreadDef(SensorTask, StartSensorTask, osPriorityIdle, 0, 512);
  SensorTaskHandle = osThreadCreate(osThread(SensorTask), NULL);

  /* USER CODE BEGIN RTOS_THREADS */
  /* add threads, ... */
  /* USER CODE END RTOS_THREADS */

}

/* USER CODE BEGIN Header_StartDefaultTask */
/**
  * @brief  Function implementing the defaultTask thread.
  * @param  argument: Not used
  * @retval None
  */
/* USER CODE END Header_StartDefaultTask */

void StartDefaultTask(void const * argument)
{
  /* USER CODE BEGIN StartDefaultTask */
  /* Infinite loop */
  for(;;)
  {
    osDelay(1);
  }
  /* USER CODE END StartDefaultTask */
}

/* USER CODE BEGIN Header_StartSensorTask */
/**
* @brief Function implementing the SensorTask thread.
* @param argument: Not used
* @retval None
*/
/* USER CODE END Header_StartSensorTask */
void StartSensorTask(void const * argument)
{
  /* USER CODE BEGIN StartSensorTask */
  /* Infinite loop */
  for(;;)
  {
	  	 benzene_ppm_in = MQ135_GetPPM(&hmq_in);
	  	 co_ppm_in = MQ135_GetCO_PPM(&hmq_in);
	  	 co2_ppm_in = MQ135_GetCO2_PPM(&hmq_in);
	  	 smoke_ppm_in = MQ135_GetSmoke_PPM(&hmq_in);

	  	 benzene_ppm_out = MQ135_GetPPM(&hmq_out);
	  	 co_ppm_out = MQ135_GetCO_PPM(&hmq_out);
	  	 co2_ppm_out = MQ135_GetCO2_PPM(&hmq_out);
	  	 smoke_ppm_out = MQ135_GetSmoke_PPM(&hmq_out);


	  	 //TeraTerm으로 uart통신해서 출력하기
	  	 printf("\r\n=== Indoor Sensor ===\r\n");
	  	 printf("Benzene : %.2f ppm \r\n", benzene_ppm_in);
	  	 printf("CO : %.2f ppm\r\n", co_ppm_in);
	  	 printf("CO2 : %.2f ppm\r\n", co2_ppm_in);
	  	 printf("Smoke : %.2f ppm\r\n", smoke_ppm_in);

	  	 printf("\r\n=== Outdoor Sensor ===\r\n");
	  	 printf("Benzene : %.2f ppm \r\n", benzene_ppm_out);
	  	 printf("CO : %.2f ppm\r\n", co_ppm_out);
	  	 printf("CO2 : %.2f ppm\r\n", co2_ppm_out);
	  	 printf("Smoke : %.2f ppm\r\n", smoke_ppm_out);


	  	 //보드끼리 uart 통신
//	  	char sendBuf[256];
//	  	    snprintf(sendBuf, sizeof(sendBuf),
//	  	             "IN: %.2f, %.2f, %.2f, %.2f; OUT: %.2f, %.2f, %.2f, %.2f\r\n",
//	  	             benzene_ppm_in, co_ppm_in, co2_ppm_in, smoke_ppm_in,
//	  	             benzene_ppm_out, co_ppm_out, co2_ppm_out, smoke_ppm_out);
//
//	  	    // 3. UART로 송신
//	  	    HAL_UART_Transmit(&huart1, sendBuf, strlen(sendBuf), HAL_MAX_DELAY);
//
//	  	    // 4. 로컬 출력 (기존처럼 printf로 Tera Term에도 보여줌)
//	  	    printf("%s", sendBuf);

    osDelay(5000);
  }
  /* USER CODE END StartSensorTask */
}

/* Private application code --------------------------------------------------*/
/* USER CODE BEGIN Application */

/* USER CODE END Application */

