#include "dht11.h"

#include "cmsis_os2.h"
#include "debug.h"
#include "main.h"
#include "tim.h"

#include <string.h>

static void    delay(uint16_t time);
static void    Set_Pin_Output(GPIO_TypeDef *GPIOx, uint16_t GPIO_Pin);
static void    Set_Pin_Input(GPIO_TypeDef *GPIOx, uint16_t GPIO_Pin);
static void    DHT11_Start(GPIO_TypeDef *port, uint16_t pin);
static uint8_t DHT11_Check_Response(GPIO_TypeDef *port, uint16_t pin);
static uint8_t DHT11_Read(GPIO_TypeDef *port, uint16_t pin);
static void    DHT11_Read_Data(dht11_sensor_t *sensor);
static void
DHT11_Init(dht11_sensor_t *sensor, GPIO_TypeDef *port, uint16_t pin);

void StartDhtReaderTask(void *argument)
{
    dht11_data_t  *dht = (dht11_data_t *)argument;
    dht11_sensor_t internal;
    dht11_sensor_t external;

    DHT11_Init(&internal, DHT11_INTERNAL_GPIO_Port, DHT11_INTERNAL_Pin);
    DHT11_Init(&external, DHT11_EXTERNAL_GPIO_Port, DHT11_EXTERNAL_Pin);

    for (;;)
    {
        DHT11_Read_Data(&internal);
        DHT11_Read_Data(&external);

        dht->internal_rh   = internal.Rh_byte1;
        dht->internal_temp = internal.Temp_byte1;
        dht->external_rh   = external.Rh_byte1;
        dht->external_temp = external.Temp_byte1;

#if (DEBUG_LEVEL > 0)
        printf("\r\n=== DHT Sensor ===\r\n");
        printf("내부[습도:%d%%, 온도:%d°C], 외부[습도:%d%%, 온도:%d°C]\r\n",
               dht->internal_rh,
               dht->internal_temp,
               dht->external_rh,
               dht->external_temp);
#endif
        osDelay(2000);
    }
}

static void delay(uint16_t time)
{
    /* change your code here for the delay in microseconds */
    __HAL_TIM_SET_COUNTER(&htim6, 0);
    while ((__HAL_TIM_GET_COUNTER(&htim6)) < time)
        ;
}

static void Set_Pin_Output(GPIO_TypeDef *GPIOx, uint16_t GPIO_Pin)
{
    GPIO_InitTypeDef GPIO_InitStruct = {0};
    GPIO_InitStruct.Pin              = GPIO_Pin;
    GPIO_InitStruct.Mode             = GPIO_MODE_OUTPUT_PP;
    GPIO_InitStruct.Speed            = GPIO_SPEED_FREQ_LOW;
    HAL_GPIO_Init(GPIOx, &GPIO_InitStruct);
}

static void Set_Pin_Input(GPIO_TypeDef *GPIOx, uint16_t GPIO_Pin)
{
    GPIO_InitTypeDef GPIO_InitStruct = {0};
    GPIO_InitStruct.Pin              = GPIO_Pin;
    GPIO_InitStruct.Mode             = GPIO_MODE_INPUT;
    GPIO_InitStruct.Pull             = GPIO_PULLUP;
    HAL_GPIO_Init(GPIOx, &GPIO_InitStruct);
}

static void DHT11_Start(GPIO_TypeDef *port, uint16_t pin)
{
    Set_Pin_Output(port, pin);       // set the pin as output
    HAL_GPIO_WritePin(port, pin, 0); // pull the pin low
    delay(18000);                    // wait for 18ms
    HAL_GPIO_WritePin(port, pin, 1); // pull the pin high
    delay(20);                       // wait for 20us
    Set_Pin_Input(port, pin);        // set as input
}

static uint8_t DHT11_Check_Response(GPIO_TypeDef *port, uint16_t pin)
{
    uint8_t Response = 0;
    delay(40);
    if (!(HAL_GPIO_ReadPin(port, pin)))
    {
        delay(80);
        if ((HAL_GPIO_ReadPin(port, pin)))
            Response = 1;
        else
            Response = -1; // 255
    }
    while ((HAL_GPIO_ReadPin(port, pin)))
        ; // wait for the pin to go low

#if (DEBUG_LEVEL > 0)
    printf("DHT11 응답: %d\r\n", Response);
#endif
    return Response;
}

static uint8_t DHT11_Read(GPIO_TypeDef *port, uint16_t pin)
{
    uint8_t i, j;
    for (j = 0; j < 8; j++)
    {
        while (!(HAL_GPIO_ReadPin(port, pin)))
            ;                               // wait for the pin to go high
        delay(40);                          // wait for 40 us
        if (!(HAL_GPIO_ReadPin(port, pin))) // if the pin is low
        {
            i &= ~(1 << (7 - j)); // write 0
        }
        else
            i |= (1 << (7 - j)); // if the pin is high, write 1
        while ((HAL_GPIO_ReadPin(port, pin)))
            ; // wait for the pin to go low
    }
    return i;
}

static void DHT11_Read_Data(dht11_sensor_t *sensor)
{
    GPIO_TypeDef *port = sensor->port;
    uint16_t      pin  = sensor->pin;

    DHT11_Start(port, pin);
    DHT11_Check_Response(port, pin);

    sensor->Rh_byte1   = DHT11_Read(port, pin);
    sensor->Rh_byte2   = DHT11_Read(port, pin);
    sensor->Temp_byte1 = DHT11_Read(port, pin);
    sensor->Temp_byte2 = DHT11_Read(port, pin);
    sensor->SUM        = DHT11_Read(port, pin);

#if (DEBUG_LEVEL > 0)
    uint8_t sum = sensor->Rh_byte1 + sensor->Rh_byte2 + sensor->Temp_byte1
                  + sensor->Temp_byte2;
    if (sum != sensor->SUM)
    {
        printf("DHT11 체크섬 오류: 계산된 합=%d, 수신된 체크섬=%d\r\n",
               sum,
               sensor->SUM);
    }
    else
    {
        printf("DHT11 데이터 읽기 성공 - 습도: %d.%d%%, 온도: %d.%d°C\r\n",
               sensor->Rh_byte1,
               sensor->Rh_byte2,
               sensor->Temp_byte1,
               sensor->Temp_byte2);
    }
#endif
}

static void DHT11_Init(dht11_sensor_t *sensor, GPIO_TypeDef *port, uint16_t pin)
{
    memset(sensor, 0, sizeof(dht11_sensor_t));
    sensor->port = port;
    sensor->pin  = pin;
}
