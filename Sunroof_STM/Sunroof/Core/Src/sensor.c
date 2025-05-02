/*
 * sensor.c
 *
 *  Created on: May 1, 2025
 *      Author: USER
 */
#include "sensor.h"

uint16_t HW_AdcReadChannel( uint32_t Channel )
{

  ADC_ChannelConfTypeDef adcConf;
  uint16_t adcData = 0;

	/* wait the the Vrefint used by adc is set */
	while (__HAL_PWR_GET_FLAG(PWR_FLAG_VREFINTRDY) == RESET) {};

	__HAL_RCC_ADC1_CLK_ENABLE();

	/*calibrate ADC if any calibraiton hardware*/
	HAL_ADCEx_Calibration_Start(&hadc, ADC_SINGLE_ENDED );

	/* Deselects all channels*/
	adcConf.Channel = ADC_CHANNEL_MASK;
	adcConf.Rank = ADC_RANK_NONE;
	HAL_ADC_ConfigChannel( &hadc, &adcConf);

	/* configure adc channel */
	adcConf.Channel = Channel;
	adcConf.Rank = ADC_RANK_CHANNEL_NUMBER;
	HAL_ADC_ConfigChannel( &hadc, &adcConf);

	/* Start the conversion process */
	HAL_ADC_Start( &hadc);

	/* Wait for the end of conversion */
	HAL_ADC_PollForConversion( &hadc, HAL_MAX_DELAY );

	/* Get the converted value of regular channel */
	adcData = HAL_ADC_GetValue ( &hadc);

	__HAL_ADC_DISABLE( &hadc) ;

	__HAL_RCC_ADC1_CLK_DISABLE();

  return adcData;
}

void read_illum(void){
	in_illum = HW_AdcReadChannel(ADC_CHANNEL_1);
    out_illum = HW_AdcReadChannel(ADC_CHANNEL_0);
}

void read_rain(void){
	rain_sense = HW_AdcReadChannel(ADC_CHANNEL_15);

	rain_state = (rain_sense > RAIN_TH);
}
