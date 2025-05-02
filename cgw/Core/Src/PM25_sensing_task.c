#include "PM25_GP2Y1023AU0F.h"
#include "cmsis_os2.h"
#include "debug.h"
#include "main.h"
#include "usart.h"

void StartPmReaderTask(void *argument)
{
    float              *pm = (float *)argument;
    sharp_dust_sensor_t dust_sensor;

    sharp_dust_sensor_init(&dust_sensor, PM2_5_GPIO_Port, PM2_5_Pin);
    for (;;)
    {
        sharp_dust_sensor_scan(&dust_sensor);
        *pm = sharp_dust_sensor_get_concentration(&dust_sensor);

#if (DEBUG_LEVEL > 0)
        // 농도 출력
        printf("\r\n=== PM2.5 Sensor ===\r\n");
        printf("Dust concentration: %.2f ugram/m^3\r\n",
               sharp_dust_sensor_get_concentration(&dust_sensor));
#endif
        osDelay(1000);
    }
}
