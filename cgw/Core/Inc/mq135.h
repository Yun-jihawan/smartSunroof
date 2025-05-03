#ifndef MQ135_STM32_H
#define MQ135_STM32_H

#include "stm32l0xx_hal.h"

#include <stdint.h>

#define MQ135_DEFAULT_AVERAGE  30
#define MQ135_DEFAULT_ADC_BITS 12
#define MQ135_DEFAULT_VREF     3.3f
#define MQ135_DEFAULT_RL       10.0f

typedef struct
{
    ADC_HandleTypeDef *hadc;
    uint32_t           adc_channel;
    uint8_t            adc_bits;
    float              vref;
    float              R0;
    float              RL;
    int                average_count;
} MQ135_HandleTypeDef;

typedef struct
{
    double benzene_ppm_in;
    double co_ppm_in;
    double co2_ppm_in;
    double smoke_ppm_in;

    double benzene_ppm_out;
    double co_ppm_out;
    double co2_ppm_out;
    double smoke_ppm_out;
} mq135_data_t;

void AQ_Init(MQ135_HandleTypeDef *hmq_in, MQ135_HandleTypeDef *hmq_out);
void AQ_Read(MQ135_HandleTypeDef *hmq_in,
             MQ135_HandleTypeDef *hmq_out,
             mq135_data_t        *mq135);

#endif
