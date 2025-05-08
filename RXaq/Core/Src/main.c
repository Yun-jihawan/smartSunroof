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
#include "dma.h"
#include "usart.h"
#include "gpio.h"

/* Private includes ----------------------------------------------------------*/
/* USER CODE BEGIN Includes */
//#include "sunroof_control.h"
#include "stm32l0xx_it.h"
#include <stdlib.h>
#include <time.h>
#include "car_device_control.h"
#include "sunroof_control.h"
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

/* USER CODE BEGIN PV */
uint8_t rx_buf[4];
uint8_t mode = 0;
uint8_t season = 0;
uint8_t tx_done = 1;  // 송신 완료 플래그

uint16_t in_illum  = 0;
uint16_t out_illum = 0;
uint8_t  rain_flag = 0;

uint8_t sunroof_command = 0;          // STM_SUN으로 보내는 선루프 제어 명령
uint8_t sunroof_state_sun = 0;        // STM_SUN으로 부터 받아오는 선루프 상태 값
uint8_t sunroof_transparency = 0;     // STM_SUN으로 보내는 선루프 투명도 제어 명령
uint8_t transparency = 0;
volatile uint8_t rx_sensor_sun = 0;   // STM_SUN으로부터 데이터 받았는지 확인하는 변수
char tx_data[2]= "00";                // STM_SUN으로 선루프 제어 명령과 투명도 값을 보내는 변수
/* USER CODE END PV */

/* Private function prototypes -----------------------------------------------*/
void SystemClock_Config(void);
/* USER CODE BEGIN PFP */
void Send_Sunroof_Command_CGW_to_RPI(char tx_command[]);
/* USER CODE END PFP */

/* Private user code ---------------------------------------------------------*/
/* USER CODE BEGIN 0 */
int __io_putchar(int ch) {
    HAL_UART_Transmit(&huart2, (uint8_t*)&ch, 1, HAL_MAX_DELAY);
    return ch;
}
void init_rand(void)
{
    srand(HAL_GetTick()); // tick을 시드로 사용
}
void HAL_UART_RxCpltCallback(UART_HandleTypeDef *huart)
{

    if (huart->Instance == USART4)
    {
        uint32_t payload = (rx_buf[0] << 24) | (rx_buf[1] << 16) | (rx_buf[2] << 8) | rx_buf[3];

       	in_illum  				    = (payload >> 20) 		& 0x0FFF; 
       	out_illum 				    = (payload >> 8)  		& 0x0FFF;
       	rain_flag 				    = (payload >> 7)  		& 0x01;
       	sunroof_state_sun 		= (payload >> 5) 		  & 0x03;
       	sunroof_transparency 	= (payload) 			    & 0x1F;
       	 char msg[64];
       	 sprintf(msg, "In: %u, Out: %u, Rain: %u \r\n", in_illum, out_illum, rain_flag);
       	 printf("%s\r\n", msg);

       	rx_sensor_sun = 1;  // 센서 값 수신 완료
       	HAL_UART_Receive_DMA(&huart4, rx_buf, sizeof(rx_buf));
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
  MX_USART2_UART_Init();
  MX_USART4_UART_Init();
  MX_USART1_UART_Init();
  /* USER CODE BEGIN 2 */
   HAL_UART_Receive_DMA(&huart4, rx_buf, sizeof(rx_buf)); // USART1 수신 시작
  /* USER CODE END 2 */

  /* Infinite loop */
  /* USER CODE BEGIN WHILE */
  while (1)
  {

	  //uart 송신 코드 DMA 
	  if (rx_sensor_sun)
	  {
			  sunroof_input.benzene_in = 0.003f;
			  sunroof_input.co_in = rand() % 40;
			  sunroof_input.co2_in = rand() % 2000;
			  sunroof_input.smoke_in = rand() % 50;
			  sunroof_input.benzene_out = 0.002f;
			  sunroof_input.co_out =  rand() % 40;
			  sunroof_input.co2_out = rand() % 2000;
			  sunroof_input.smoke_out = rand() % 50;
			  sunroof_input.pm25_out = rand() % 30;
			  sunroof_input.light = out_illum;                     // uart 통해 선루프로부터 들어온 센서 값
			  sunroof_input.rain_detected = rain_flag;             // uart 통해 선루프로부터 들어온 센서 값
			  sunroof_input.velocity = rand() % 100;
			  sunroof_input.current_state = sunroof_state_sun;
			  sunroof_command = Smart_Sunroof_Control(sunroof_input);
			  transparency = calculate_transparency(0, sunroof_input.light, 0);
			  transparency /=5;

			  tx_data[0] = (sunroof_command + '0');
			  tx_data[1] = transparency;

			  if (sunroof_command != sunroof_state_sun)
			  {
				  Send_Sunroof_Command_CGW_to_RPI(tx_data);
			  }
			  rx_sensor_sun = 0;
	  }

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
  RCC_OscInitStruct.OscillatorType = RCC_OSCILLATORTYPE_MSI;
  RCC_OscInitStruct.MSIState = RCC_MSI_ON;
  RCC_OscInitStruct.MSICalibrationValue = 0;
  RCC_OscInitStruct.MSIClockRange = RCC_MSIRANGE_5;
  RCC_OscInitStruct.PLL.PLLState = RCC_PLL_NONE;
  if (HAL_RCC_OscConfig(&RCC_OscInitStruct) != HAL_OK)
  {
    Error_Handler();
  }

  /** Initializes the CPU, AHB and APB buses clocks
  */
  RCC_ClkInitStruct.ClockType = RCC_CLOCKTYPE_HCLK|RCC_CLOCKTYPE_SYSCLK
                              |RCC_CLOCKTYPE_PCLK1|RCC_CLOCKTYPE_PCLK2;
  RCC_ClkInitStruct.SYSCLKSource = RCC_SYSCLKSOURCE_MSI;
  RCC_ClkInitStruct.AHBCLKDivider = RCC_SYSCLK_DIV1;
  RCC_ClkInitStruct.APB1CLKDivider = RCC_HCLK_DIV1;
  RCC_ClkInitStruct.APB2CLKDivider = RCC_HCLK_DIV1;

  if (HAL_RCC_ClockConfig(&RCC_ClkInitStruct, FLASH_LATENCY_0) != HAL_OK)
  {
    Error_Handler();
  }
  PeriphClkInit.PeriphClockSelection = RCC_PERIPHCLK_USART1|RCC_PERIPHCLK_USART2;
  PeriphClkInit.Usart1ClockSelection = RCC_USART1CLKSOURCE_PCLK2;
  PeriphClkInit.Usart2ClockSelection = RCC_USART2CLKSOURCE_PCLK1;
  if (HAL_RCCEx_PeriphCLKConfig(&PeriphClkInit) != HAL_OK)
  {
    Error_Handler();
  }
}

/* USER CODE BEGIN 4 */
void Send_Sunroof_Command_CGW_to_RPI(char tx_command[])
{
  HAL_UART_Transmit(&huart4, (uint8_t *)tx_command, strlen(tx_data), 100);
}


void HAL_UART_TxCpltCallback(UART_HandleTypeDef *huart)
{
    if (huart->Instance == USART4)
    {
        tx_done = 1;  // 송신 완료 표시
    }
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
