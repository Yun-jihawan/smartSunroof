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
#include "uart_cgw_rpi.h"
#include "usart.h"
#include "wh148.h"
/* USER CODE END Includes */

/* Private typedef -----------------------------------------------------------*/
/* USER CODE BEGIN PTD */

/* USER CODE END PTD */

/* Private define ------------------------------------------------------------*/
/* USER CODE BEGIN PD */
#define CGW_DATA_READY_EVENT (1 << 0)
#define SUN_DATA_READY_EVENT (1 << 1)
#define USER_DATA_READY_EVENT (1 << 2)
#define SENSOR_DATA_READY_EVENT (CGW_DATA_READY_EVENT | SUN_DATA_READY_EVENT)
#define ANY_DATA_READY_EVENT (SENSOR_DATA_READY_EVENT | USER_DATA_READY_EVENT)
/* USER CODE END PD */

/* Private macro -------------------------------------------------------------*/
/* USER CODE BEGIN PM */

/* USER CODE END PM */

/* Private variables ---------------------------------------------------------*/
/* USER CODE BEGIN Variables */
// CGW-SUN
static uint8_t rx_buf[4];

// CGW-RPI
static uint8_t rx_data[5];
// static ThresholdData_from_rpi_t thresholdData;

static sunroof_t sunroof;
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

static void Receive_Sensor_Data_SUN_to_CGW(uint8_t *rx_buffer)
{
  sensor_data_t *data = &sunroof.data;
  system_state_t *state = &sunroof.state;
  uint32_t payload = (rx_buffer[0] << 24) | (rx_buffer[1] << 16) | (rx_buffer[2] << 8) | rx_buffer[3];

  // data->illum[IN] = (payload >> 20) & 0x0FFF;
  // data->illum[OUT] = (payload >> 8) & 0x0FFF;
  data->illum = (payload >> 8) & 0x0FFF;
  data->rain = (payload >> 7) & 0x01;
  state->roof = (payload >> 5) & 0x03;
  // state->transparency = (payload) & 0x1F;

#if (DEBUG_LEVEL > 0)
  char msg[64];
  sprintf(msg,
          "In: %u, Out: %u, Rain: %u \r\n",
          in_illum,
          out_illum,
          rain_flag);
  printf("%s\r\n", msg);
#endif
}

static void Send_Sunroof_Command_CGW_to_SUN(system_state_t *state)
{
  static uint8_t tx_data_uart1[2];

  tx_data_uart1[0] = state->roof + '0';
  tx_data_uart1[1] = state->transparency / 5;
  HAL_UART_Transmit(&huart1, tx_data_uart1, sizeof(tx_data_uart1), HAL_MAX_DELAY);
#if (DEBUG_LEVEL > 0)
  printf("Send command %s \r\n", tx_data);
#endif
}

static void Send_Sunroof_CGW_to_ESP()
{
  static uint8_t tx_data_lpuart1[7];

  dht11_data_t *dht11 = sunroof.data.dht;
  system_state_t *state = &sunroof.state;
  air_dust_level_t *air_dust_state = &sunroof.air_dust_level;

  tx_data_lpuart1[0] = 0xAA; // 패킷 시작 식별자
  tx_data_lpuart1[1] = state->mode
                      | (state->roof << 1)
                      | (state->airconditioner << 3);
  tx_data_lpuart1[2] = dht11[IN].temp;
  tx_data_lpuart1[3] = dht11[IN].rh;
  tx_data_lpuart1[4] = dht11[OUT].temp;
  tx_data_lpuart1[5] = dht11[OUT].rh;
  tx_data_lpuart1[6] = air_dust_state->internal_air_quality_state
                      | (air_dust_state->external_air_quality_state << 2)
                      | (air_dust_state->dust_state << 4);

  HAL_UART_Transmit(&hlpuart1, tx_data_lpuart1, sizeof(tx_data_lpuart1), HAL_MAX_DELAY);
}
/* USER CODE END 4 */

/**
  * @brief  FreeRTOS initialization
  * @param  None
  * @retval None
  */
void MX_FREERTOS_Init(void) {
  /* USER CODE BEGIN Init */
  sunroof.state.mode = MODE_SMART;
  HAL_UART_Receive_DMA(&huart1, rx_buf, sizeof(rx_buf));
  HAL_UART_Receive_DMA(&huart4, rx_data, sizeof(rx_data));
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
  SensorReadTaskHandle = osThreadNew(StartSensorReadTask, NULL, &SensorReadTask_attributes);

  /* creation of DataHandlerTask */
  DataHandlerTaskHandle = osThreadNew(StartDataHandlerTask, NULL, &DataHandlerTask_attributes);

  /* USER CODE BEGIN RTOS_THREADS */
  /* add threads, ... */
  /* USER CODE END RTOS_THREADS */

  /* USER CODE BEGIN RTOS_EVENTS */
  /* add events, ... */
  xSensorEventGroup = xEventGroupCreate();
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
  const uint8_t tx_request[2] = {0xFF, 0xFF};

  sensor_data_t *data = &sunroof.data;

  dht11_sensor_t dht_sensors[2];
  mq135_sensor_t aq_sensors[2];
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
    POT_Read(&data->velocity);
    __enable_irq();

    xEventGroupSetBits(xSensorEventGroup, CGW_DATA_READY_EVENT);

    // SUN에 나한테 데이터 보내라고 요청
    HAL_UART_Transmit(&huart1, tx_request, sizeof(tx_request), HAL_MAX_DELAY);

    osDelay(10000);
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
  sensor_data_t *data = &sunroof.data;
  system_state_t *state = &sunroof.state;
  air_dust_level_t *air_dust_level = &sunroof.air_dust_level;

  EventBits_t uxBits;

  /* Infinite loop */
  for(;;)
  {
    // 이벤트 대기
    uxBits = xEventGroupWaitBits(xSensorEventGroup,
                                 ANY_DATA_READY_EVENT,
                                 pdFALSE, // xClearOnExit: 비트 자동 클리어 설정
                                 pdFALSE, // xWaitForAllBits: 모든 비트 필요 여부
                                 portMAX_DELAY);
    if (uxBits == CGW_DATA_READY_EVENT || uxBits == SUN_DATA_READY_EVENT)
    {
      continue;
    }

    if (uxBits & SENSOR_DATA_READY_EVENT)
    {
      osDelay(100);
      send_sensor_data(data->dht, air_dust_level);

      if (state->mode == MODE_SMART)
      {
        state->roof = Smart_Sunroof_Control(&sunroof);
        state->transparency = calculate_transparency(data->illum);
        state->airconditioner = smart_device_command(data->dht, threshold_input.temp_threshold);

        osDelay(100);
        send_device_state(&sunroof);
      }
    }

    if (uxBits & USER_DATA_READY_EVENT)
    {
      // User_Sunroof_Control();
      // user_device_command();
    }

    osDelay(100);
    Send_Sunroof_Command_CGW_to_SUN(state);
    osDelay(100);
    Send_Sunroof_CGW_to_ESP();
    osDelay(100);
    xEventGroupClearBits(xSensorEventGroup, ANY_DATA_READY_EVENT);
  }
  /* USER CODE END StartDataHandlerTask */
}

/* Private application code --------------------------------------------------*/
/* USER CODE BEGIN Application */
void HAL_UART_RxCpltCallback(UART_HandleTypeDef *huart)
{
  system_state_t *state = &sunroof.state;

  if (huart->Instance == USART1)
  {
    Receive_Sensor_Data_SUN_to_CGW(rx_buf);
    xEventGroupSetBits(xSensorEventGroup, SUN_DATA_READY_EVENT);

    HAL_UART_Receive_DMA(&huart1, rx_buf, sizeof(rx_buf));
  }
  else if (huart->Instance == USART4)
  {
#if (DEBUG_LEVEL > 0)
    uint8_t debug_msg[5];
    HAL_GPIO_TogglePin(GPIOA, GPIO_PIN_5);
    sprintf((char *)debug_msg,
            "%d %d %d %d %d\r\n",
            rx_data[0],
            rx_data[1],
            rx_data[2],
            rx_data[3],
            rx_data[4]);
    printf("identifier: %d\r\n", rx_data[0]);
#endif
    int identifier = rx_data[0];

    switch (identifier)
    {
    case THRES_IDENTIFIER:
      threshold_input.temp_threshold = rx_data[1];
      // thresholdData.air_quality_threshold = rx_data[2];
      // thresholdData.dust_threshold = rx_data[3];
      threshold_input.light_threshold = rx_data[4];
      break;
    case CONTROL_IDENTIFIER:
      state->roof = rx_data[1];
      state->transparency = rx_data[2];
      state->airconditioner = rx_data[3];
      state->mode = RECEIVED_MASK & rx_data[4]; // 사용자 모드이면 안보내기
#if (DEBUG_LEVEL > 0)
      printf("received : %d\r\n", controlData.mode);
#endif
      break;
    default:
#if (DEBUG_LEVEL > 0)
      // 에러 처리: 잘못된 식별자
      const char *err_msg = "ERR: Invalid identifier\r\n";
      HAL_UART_Transmit(&huart2,
                        (uint8_t *)err_msg,
                        strlen(err_msg),
                        100);
      // 예시: 에러 LED 점등 (e.g., GPIOA Pin 5)
      HAL_GPIO_WritePin(GPIOA, GPIO_PIN_5, GPIO_PIN_SET);
#endif
      // 필요 시: 에러 카운트 증가, 무시, 로그 저장 등
      break;
    }

#if (DEBUG_LEVEL > 0)
    // 받은 데이터를 다시 전송 (Echo)
    HAL_UART_Transmit(&huart2, debug_msg, strlen((char *)debug_msg), 100);
#endif
    xEventGroupSetBits(xSensorEventGroup, USER_DATA_READY_EVENT);

    // 다시 수신 대기 (인터럽트 재등록)
    HAL_UART_Receive_DMA(&huart4, rx_data, sizeof(rx_data));
  }
}

void HAL_UART_TxCpltCallback(UART_HandleTypeDef *huart)
{

}
/* USER CODE END Application */

