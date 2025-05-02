#include "adc.h"
#include "cmsis_os2.h"
#include "debug.h"
#include "mq135.h"

void StartAqReaderTask(void *argument)
{
    mq135_data_t       *mq135 = (mq135_data_t *)argument;
    MQ135_HandleTypeDef hmq_in;
    MQ135_HandleTypeDef hmq_out;

    MQ135_Init(&hmq_in,
               &hadc,
               ADC_CHANNEL_0,
               MQ135_DEFAULT_AVERAGE,
               MQ135_DEFAULT_ADC_BITS,
               MQ135_DEFAULT_VREF);
    MQ135_SetRL(&hmq_in, 10.0f); // 로드 저항 값 설정 (보통 10k옴)
    MQ135_SetR0(&hmq_in, 9.83f); // 깨끗한 공기에서의 초기 R0 값 (직접 보정해서
                                 // 측정하는 게 가장 정확)
    MQ135_Init(&hmq_out,
               &hadc,
               ADC_CHANNEL_1,
               MQ135_DEFAULT_AVERAGE,
               MQ135_DEFAULT_ADC_BITS,
               MQ135_DEFAULT_VREF);
    MQ135_SetRL(&hmq_out, 10.0f); // 로드 저항 값 설정 (보통 10k옴)
    MQ135_SetR0(&hmq_out, 9.83f); // 깨끗한 공기에서의 초기 R0 값 (직접 보정해서
                                  // 측정하는 게 가장 정확)
    for (;;)
    {
        mq135->benzene_ppm_in = MQ135_GetPPM(&hmq_in) / 10.000;
        mq135->co_ppm_in      = MQ135_GetCO_PPM(&hmq_in) / 9.00;
        mq135->co2_ppm_in     = MQ135_GetCO2_PPM(&hmq_in) / 5.00;
        mq135->smoke_ppm_in   = MQ135_GetSmoke_PPM(&hmq_in) / 50.00;

        mq135->benzene_ppm_out = MQ135_GetPPM(&hmq_out) / 10.000;
        mq135->co_ppm_out      = MQ135_GetCO_PPM(&hmq_out) / 9.00;
        mq135->co2_ppm_out     = MQ135_GetCO2_PPM(&hmq_out) / 5.00;
        mq135->smoke_ppm_out   = MQ135_GetSmoke_PPM(&hmq_out) / 50.00;

#if (DEBUG_LEVEL > 0)
        // TeraTerm으로 uart통신해서 출력하기
        printf("\r\n=== AQ Indoor Sensor ===\r\n");
        printf("Benzene : %.3f ppm \r\n", mq135->benzene_ppm_in);
        printf("CO : %.2f ppm\r\n", mq135->co_ppm_in);
        printf("CO2 : %.2f ppm\r\n", mq135->co2_ppm_in);
        printf("Smoke : %.2f ppm\r\n", mq135->smoke_ppm_in);

        printf("\r\n=== AQ Outdoor Sensor ===\r\n");
        printf("Benzene : %.3f ppm \r\n", mq135->benzene_ppm_out);
        printf("CO : %.2f ppm\r\n", mq135->co_ppm_out);
        printf("CO2 : %.2f ppm\r\n", mq135->co2_ppm_out);
        printf("Smoke : %.2f ppm\r\n", mq135->smoke_ppm_out);
#endif
        osDelay(5000);
    }
}