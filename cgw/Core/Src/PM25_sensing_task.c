#include "PM25_GP2Y1023AU0F.h"
#include "main.h"
#include "usart.h"

// int __io_putchar(int ch)
// {
//     HAL_UART_Transmit(&huart2, (uint8_t *)&ch, 1, HAL_MAX_DELAY);
//     return ch;
// }

void StartPmReaderTask(void *argument)
{
    float *pm = (float *)argument;
    sharp_dust_sensor_t dust_sensor;

    sharp_dust_sensor_init(&dust_sensor, PM2_5_GPIO_Port, PM2_5_Pin);
    for (;;)
    {
        sharp_dust_sensor_scan(&dust_sensor);
        *pm = sharp_dust_sensor_get_concentration(&dust_sensor);
        // 농도 출력
        // printf("Dust concentration: %.2f ugram/m^3\r\n",
        //        sharp_dust_sensor_get_concentration(&dust_sensor));
        osDelay(1000);
    }
}
