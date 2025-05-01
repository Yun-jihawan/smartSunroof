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
} mq135_data;

void   MQ135_Init(MQ135_HandleTypeDef *hmq,
                  ADC_HandleTypeDef   *hadc,
                  uint32_t             channel,
                  int                  average,
                  uint8_t              bits,
                  float                vref);
void   MQ135_SetRL(MQ135_HandleTypeDef *hmq, float RL);
void   MQ135_SetR0(MQ135_HandleTypeDef *hmq, float R0);
float  MQ135_Calibrate(MQ135_HandleTypeDef *hmq, int cPPM);
double MQ135_GetPPM(MQ135_HandleTypeDef *hmq);
double MQ135_GetCorrectedPPM(MQ135_HandleTypeDef *hmq,
                             float                temperature,
                             float                humidity);

double MQ135_GetCO_PPM(MQ135_HandleTypeDef *hmq);
double MQ135_GetCO2_PPM(MQ135_HandleTypeDef *hmq);
double MQ135_GetSmoke_PPM(MQ135_HandleTypeDef *hmq);

#endif