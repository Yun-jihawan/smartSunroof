#include "usart.h"
#include "PM25_GP2Y1023AU0F.h"

/* USER CODE BEGIN Header_StartDefaultTask */
/**
 * @brief  Function implementing the defaultTask thread.
 * @param  argument: Not used
 * @retval None
 */
int __io_putchar(int ch)
{
  HAL_UART_Transmit(&huart2, (uint8_t *)&ch, 1, HAL_MAX_DELAY);
  return ch;
}

/* USER CODE END Header_StartDefaultTask */
void StartDustTask(void const *argument)
{
  /* USER CODE BEGIN StartDefaultTask */
  /* Infinite loop */
  sharp_dust_sensor_init(&dust_sensor, GPIOA, GPIO_PIN_0);

  for (;;)
  {
    sharp_dust_sensor_scan(&dust_sensor);
    // 농도 출력
    printf("Dust concentration: %.2f ugram/m^3\r\n", sharp_dust_sensor_get_concentration(&dust_sensor));
    osDelay(1000);
  }
  /* USER CODE END StartDefaultTask */
}

/* Private application code --------------------------------------------------*/
/* USER CODE BEGIN Application */

/* USER CODE END Application */
