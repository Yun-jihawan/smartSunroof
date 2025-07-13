#include "adc.h"

#define MIN_ADC_VALUE 500
#define MAX_ADC_VALUE 4095
#define MAX_VELOCITY  200

void POT_Read(uint8_t *velocity)
{
    ADC_ChannelConfTypeDef adcConf;
    uint32_t               adcData = 0;

    /* Deselect all channels */
    adcConf.Channel = ADC_CHANNEL_MASK;
    adcConf.Rank    = ADC_RANK_NONE;
    HAL_ADC_ConfigChannel(&hadc, &adcConf);

    /* Configure adc channel */
    adcConf.Channel = ADC_CHANNEL_6;
    adcConf.Rank    = ADC_RANK_CHANNEL_NUMBER;
    HAL_ADC_ConfigChannel(&hadc, &adcConf);

    /* Start the conversion process */
    HAL_ADC_Start(&hadc);
    if (HAL_ADC_PollForConversion(&hadc, HAL_MAX_DELAY) == HAL_OK)
    {
        adcData = HAL_ADC_GetValue(&hadc);
    }
    HAL_ADC_Stop(&hadc);

    if (adcData < MIN_ADC_VALUE)
    {
        adcData = MIN_ADC_VALUE;
    }
    *velocity = (uint8_t)(((float)(adcData - MIN_ADC_VALUE)
                           / (MAX_ADC_VALUE - MIN_ADC_VALUE))
                          * MAX_VELOCITY);
}
