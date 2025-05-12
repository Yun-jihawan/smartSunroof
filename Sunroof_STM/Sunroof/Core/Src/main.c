/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * @file           : main.c
  * @brief          : Main program body
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
#include "main.h"
#include "adc.h"
#include "dma.h"
#include "usart.h"
#include "tim.h"
#include "gpio.h"

/* Private includes ----------------------------------------------------------*/
/* USER CODE BEGIN Includes */
#include <string.h>
#include <stdio.h>
#include "sensor.h"
#include "roof.h"
/* USER CODE END Includes */

/* Private typedef -----------------------------------------------------------*/
/* USER CODE BEGIN PTD */

/* USER CODE END PTD */

/* Private define ------------------------------------------------------------*/
/* USER CODE BEGIN PD */
#define CLOSE 0
#define TILTING 1
#define OPEN 2
#define STOP 3

#define IN_ILLUM_SHIFT     20
#define OUT_ILLUM_SHIFT    8
#define RAIN_STATE_SHIFT   7
#define ROOF_STATE_SHIFT   5
#define FILM_OPACITY_SHIFT 0

#define IN_ILLUM_MASK      0x0FFF
#define OUT_ILLUM_MASK     0x0FFF
#define RAIN_STATE_MASK    0x01
#define ROOF_STATE_MASK    0x03
#define FILM_OPACITY_MASK  0x1F

/* USER CODE END PD */

/* Private macro -------------------------------------------------------------*/
/* USER CODE BEGIN PM */

/* USER CODE END PM */

/* Private variables ---------------------------------------------------------*/

/* USER CODE BEGIN PV */

// Sensor Data & UART Send Data
volatile int32_t roof_encoder = 0;
volatile int32_t tilting_encoder = 0;
volatile uint16_t in_illum = 0;
volatile uint16_t out_illum = 0;
volatile uint16_t rain_sense;
volatile uint8_t rain_state = 0;

// UART Receive Data
volatile uint8_t film_opacity = 0; 	// X 5 해서 사용
volatile uint8_t roof_state = STOP;

// UART Variables
uint8_t SPD_tx_payload;

uint8_t CGW_tx_buf[4];
uint32_t CGW_tx_payload;

uint8_t CGW_rx_buf[2];
uint16_t CGW_rx_payload;
uint8_t CGW_rx_ready = 0;

uint8_t receive_data = 0;

// Sensor Read Flag
uint8_t sensor_read = 0;

static uint8_t prev_roof_state = 0xFF;  // 처음에는 일치하지 않도록 임의의 값


/* USER CODE END PV */

/* Private function prototypes -----------------------------------------------*/
void SystemClock_Config(void);
static void MX_NVIC_Init(void);
/* USER CODE BEGIN PFP */
void Send_Data_CGW(void);
void Send_Data_SPD(void);
/* USER CODE END PFP */

/* Private user code ---------------------------------------------------------*/
/* USER CODE BEGIN 0 */

int __io_putchar(int ch) {
    HAL_UART_Transmit(&huart2, (uint8_t*)&ch, 1, HAL_MAX_DELAY);
    return ch;
}

// UART RX
void HAL_UART_RxCpltCallback(UART_HandleTypeDef *huart)
{
    if (huart->Instance == USART1) {
    	CGW_rx_ready = 1;
    	HAL_UART_Receive_DMA(&huart1, CGW_rx_buf, sizeof(CGW_rx_buf));
    }
}
/* USER CODE END 0 */

/**
  * @brief  The application entry point.
  * @retval int
  */
int main(void)
{

  /* USER CODE BEGIN 1 */

  /* USER CODE END 1 */

  /* MCU Configuration--------------------------------------------------------*/

  /* Reset of all peripherals, Initializes the Flash interface and the Systick. */
  HAL_Init();

  /* USER CODE BEGIN Init */

  /* USER CODE END Init */

  /* Configure the system clock */
  SystemClock_Config();

  /* USER CODE BEGIN SysInit */

  /* USER CODE END SysInit */

  /* Initialize all configured peripherals */
  MX_GPIO_Init();
  MX_DMA_Init();
  MX_ADC_Init();
  MX_TIM2_Init();
  MX_USART2_UART_Init();
  MX_TIM3_Init();
  MX_USART5_UART_Init();
  MX_USART1_UART_Init();
  MX_LPUART1_UART_Init();

  /* Initialize interrupts */
  MX_NVIC_Init();
  /* USER CODE BEGIN 2 */
  HAL_TIM_PWM_Start(&htim2, TIM_CHANNEL_2);
  HAL_TIM_PWM_Start(&htim3, TIM_CHANNEL_2);

  HAL_UART_Receive_DMA(&huart1, CGW_rx_buf, sizeof(CGW_rx_buf));

  // Initialize
  roof_encoder = 0;
  tilting_encoder = 0;
  roof_state = STOP;
  film_opacity = 0;
  CGW_rx_ready = 0;
  sensor_read = 0;

  printf("#BOOT\r\n");

  // DFPlayer Mini 초기화
  DFPlayerMini_InitPlayer(&hlpuart1);

//  roof_state = 1;

  /* USER CODE END 2 */

  /* Infinite loop */
  /* USER CODE BEGIN WHILE */
  while (1)
  {
	  // 센서 값 수신 및 UART 송신
	  if(sensor_read)
	  {
		  sensor_read = 0;

		  read_illum();
		  read_rain();
		  Send_Data_CGW();

		  printf("#SEND\r\n in : %d, out : %d, rain : %d, roof : %d, opacity : %d\r\n", \
				  (int)in_illum, (int)out_illum, (int)rain_state, (int)roof_state, (int)film_opacity);
	  }

	  if(CGW_rx_ready)
	  {
		  CGW_rx_ready = 0;

		  CGW_rx_payload = 0;
		  CGW_rx_payload |= ((uint16_t)CGW_rx_buf[0] << 8);
		  CGW_rx_payload |= ((uint16_t)CGW_rx_buf[1]);

		  if(CGW_rx_payload == 0xFFFF) {
			  sensor_read = 1;

			  printf("#REQUEST\r\n");
		  }
		  else {
			  roof_state = ((CGW_rx_payload >> 8) & 0x03);
			  film_opacity = ((CGW_rx_payload) & 0x1F);
			  Send_Data_SPD();

			  printf("#RECEIVE\r\n roof : %d, opacity : %d\r\n", (int)roof_state, (int)film_opacity);
		  }
	  }

	  if (roof_state != prev_roof_state) {
	      prev_roof_state = roof_state;  // 상태 변경 기록

	      switch (roof_state) {
	      	  case OPEN:
	      		  // 선루프 자동 열림 mp3 재생
	      		  DFPlayerMini_PlayFile(1);
	              break;
	          case TILTING:
				  // 선루프 자동 틸팅 mp3 재생
				  DFPlayerMini_PlayFile(2);
	        	  break;
	          case CLOSE:
	        	  // 선루프 자동 닫힘 mp3 재생
				  DFPlayerMini_PlayFile(3);
	              break;
			  case STOP:
	          default:
	              break;
	      }
	  }

	  Sunroof_Set(roof_state);

    /* USER CODE END WHILE */

    /* USER CODE BEGIN 3 */
  }
  /* USER CODE END 3 */
}

/**
  * @brief System Clock Configuration
  * @retval None
  */
void SystemClock_Config(void)
{
  RCC_OscInitTypeDef RCC_OscInitStruct = {0};
  RCC_ClkInitTypeDef RCC_ClkInitStruct = {0};
  RCC_PeriphCLKInitTypeDef PeriphClkInit = {0};

  /** Configure the main internal regulator output voltage
  */
  __HAL_PWR_VOLTAGESCALING_CONFIG(PWR_REGULATOR_VOLTAGE_SCALE1);

  /** Initializes the RCC Oscillators according to the specified parameters
  * in the RCC_OscInitTypeDef structure.
  */
  RCC_OscInitStruct.OscillatorType = RCC_OSCILLATORTYPE_HSE;
  RCC_OscInitStruct.HSEState = RCC_HSE_BYPASS;
  RCC_OscInitStruct.PLL.PLLState = RCC_PLL_NONE;
  if (HAL_RCC_OscConfig(&RCC_OscInitStruct) != HAL_OK)
  {
    Error_Handler();
  }

  /** Initializes the CPU, AHB and APB buses clocks
  */
  RCC_ClkInitStruct.ClockType = RCC_CLOCKTYPE_HCLK|RCC_CLOCKTYPE_SYSCLK
                              |RCC_CLOCKTYPE_PCLK1|RCC_CLOCKTYPE_PCLK2;
  RCC_ClkInitStruct.SYSCLKSource = RCC_SYSCLKSOURCE_HSE;
  RCC_ClkInitStruct.AHBCLKDivider = RCC_SYSCLK_DIV1;
  RCC_ClkInitStruct.APB1CLKDivider = RCC_HCLK_DIV1;
  RCC_ClkInitStruct.APB2CLKDivider = RCC_HCLK_DIV1;

  if (HAL_RCC_ClockConfig(&RCC_ClkInitStruct, FLASH_LATENCY_0) != HAL_OK)
  {
    Error_Handler();
  }
  PeriphClkInit.PeriphClockSelection = RCC_PERIPHCLK_USART1|RCC_PERIPHCLK_USART2
                              |RCC_PERIPHCLK_LPUART1;
  PeriphClkInit.Usart1ClockSelection = RCC_USART1CLKSOURCE_PCLK2;
  PeriphClkInit.Usart2ClockSelection = RCC_USART2CLKSOURCE_PCLK1;
  PeriphClkInit.Lpuart1ClockSelection = RCC_LPUART1CLKSOURCE_PCLK1;
  if (HAL_RCCEx_PeriphCLKConfig(&PeriphClkInit) != HAL_OK)
  {
    Error_Handler();
  }
}

/**
  * @brief NVIC Configuration.
  * @retval None
  */
static void MX_NVIC_Init(void)
{
  /* EXTI0_1_IRQn interrupt configuration */
  HAL_NVIC_SetPriority(EXTI0_1_IRQn, 0, 0);
  HAL_NVIC_EnableIRQ(EXTI0_1_IRQn);
}

/* USER CODE BEGIN 4 */
void Send_Data_CGW(void) {
	//UART Send
	CGW_tx_payload = 0;
	CGW_tx_payload |= ((uint32_t)(in_illum & IN_ILLUM_MASK)) << IN_ILLUM_SHIFT;
	CGW_tx_payload |= ((uint32_t)(out_illum & OUT_ILLUM_MASK)) << OUT_ILLUM_SHIFT;
	CGW_tx_payload |= ((uint32_t)(rain_state & RAIN_STATE_MASK)) << RAIN_STATE_SHIFT;
	CGW_tx_payload |= ((uint32_t)(roof_state & ROOF_STATE_MASK)) << ROOF_STATE_SHIFT;
	CGW_tx_payload |= ((uint32_t)(film_opacity & FILM_OPACITY_MASK)) << FILM_OPACITY_SHIFT;

	CGW_tx_buf[0] = (CGW_tx_payload >> 24) & 0xFF;
	CGW_tx_buf[1] = (CGW_tx_payload >> 16) & 0xFF;
	CGW_tx_buf[2] = (CGW_tx_payload >> 8) & 0xFF;
	CGW_tx_buf[3] = CGW_tx_payload & 0xFF;

	HAL_UART_Transmit(&huart1, CGW_tx_buf, sizeof(CGW_tx_buf), 100);
}

void Send_Data_SPD(void) {
	SPD_tx_payload = (uint8_t)(film_opacity * 5);

	HAL_UART_Transmit(&huart5, &SPD_tx_payload, 1, 100);
}
/* USER CODE END 4 */

/**
  * @brief  This function is executed in case of error occurrence.
  * @retval None
  */
void Error_Handler(void)
{
  /* USER CODE BEGIN Error_Handler_Debug */
  /* User can add his own implementation to report the HAL error return state */
  __disable_irq();
  while (1)
  {
  }
  /* USER CODE END Error_Handler_Debug */
}

#ifdef  USE_FULL_ASSERT
/**
  * @brief  Reports the name of the source file and the source line number
  *         where the assert_param error has occurred.
  * @param  file: pointer to the source file name
  * @param  line: assert_param error line source number
  * @retval None
  */
void assert_failed(uint8_t *file, uint32_t line)
{
  /* USER CODE BEGIN 6 */
  /* User can add his own implementation to report the file name and line number,
     ex: printf("Wrong parameters value: file %s on line %d\r\n", file, line) */
  /* USER CODE END 6 */
}
#endif /* USE_FULL_ASSERT */
