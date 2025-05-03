
#include "mq135.h"

#include "adc.h"
#include "debug.h"
#include "main.h"

#include <math.h>

static const float aBezneze = 23.4461175f;
static const float bBenzene = -3.98724605f;
// static const float aCor     = 0.00035f;
// static const float bCor     = 0.02718f;
// static const float cCor     = 1.39538f;
// static const float dCor     = 0.0018f;

// Additional gas curve coefficients
static const float aCO    = -0.36f;
static const float bCO    = 1.72f;
static const float aCO2   = -0.38f;
static const float bCO2   = 3.75f;
static const float aSmoke = -0.42f;
static const float bSmoke = 2.90f;

static float MQ135_ReadAverageADC(mq135_sensor_t *hmq)
{
    if (hmq == NULL || hmq->hadc == NULL)
        return 0.0f;

    float                  sum     = 0;
    ADC_ChannelConfTypeDef sConfig = {0};

    sConfig.Channel = hmq->adc_channel;
    // sConfig.Rank = ADC_REGULAR_RANK_1;
    // sConfig.SamplingTime = ADC_SAMPLETIME_39CYCLES_5;

    HAL_ADC_ConfigChannel(hmq->hadc, &sConfig);

    for (int i = 0; i < hmq->average_count; i++)
    {
        HAL_ADC_Start(hmq->hadc);
        if (HAL_ADC_PollForConversion(hmq->hadc, HAL_MAX_DELAY) == HAL_OK)
        {
            sum += HAL_ADC_GetValue(hmq->hadc);
        }
        HAL_ADC_Stop(hmq->hadc);
    }

    return sum / hmq->average_count;
}

static float MQ135_GetResistance(mq135_sensor_t *hmq)
{
    float adc_avg = MQ135_ReadAverageADC(hmq);
    float voltage = (adc_avg / ((1 << hmq->adc_bits) - 1)) * hmq->vref;
    if (voltage <= 0.0f)
        return 1e9f; // Avoid divide-by-zero
    return ((hmq->vref / voltage) - 1.0f) * hmq->RL;
}

// static float MQ135_GetCorrection(float t, float h)
// {
//     return aCor * t * t - bCor * t + cCor - (h - 33.0f) * dCor;
// }

static void MQ135_Init(mq135_sensor_t    *hmq,
                       ADC_HandleTypeDef *hadc,
                       uint32_t           channel,
                       int                average,
                       uint8_t            bits,
                       float              vref)
{
    if (hmq == NULL)
        return;
    hmq->hadc          = hadc;
    hmq->adc_channel   = channel;
    hmq->adc_bits      = bits;
    hmq->vref          = vref;
    hmq->average_count = average;
    hmq->RL            = MQ135_DEFAULT_RL;
    hmq->R0            = 1.0f;
}

static void MQ135_SetRL(mq135_sensor_t *hmq, float RL)
{
    if (hmq)
        hmq->RL = RL;
}

static void MQ135_SetR0(mq135_sensor_t *hmq, float R0)
{
    if (hmq)
        hmq->R0 = R0;
}

// static float MQ135_Calibrate(mq135_sensor_t *hmq, int cPPM)
// {
//     float RS = MQ135_GetResistance(hmq);
//     hmq->R0  = RS * powf((float)cPPM / aBezneze, 1.0f / -bBenzene);
//     return hmq->R0;
// }

static double MQ135_GetPPM(mq135_sensor_t *hmq)
{
    float RS = MQ135_GetResistance(hmq);
    return aBezneze * powf(RS / hmq->R0, bBenzene);
}

// static double MQ135_GetCorrectedPPM(mq135_sensor_t *hmq,
//                                     float                temperature,
//                                     float                humidity)
// {
//     float RS =
//         MQ135_GetResistance(hmq) / MQ135_GetCorrection(temperature,
//         humidity);
//     return aBezneze * powf(RS / hmq->R0, bBenzene);
// }

static double MQ135_GetCO_PPM(mq135_sensor_t *hmq)
{
    float RS    = MQ135_GetResistance(hmq);
    float ratio = RS / hmq->R0;
    return powf(10.0f, (aCO * log10f(ratio) + bCO));
}

static double MQ135_GetCO2_PPM(mq135_sensor_t *hmq)
{
    float RS    = MQ135_GetResistance(hmq);
    float ratio = RS / hmq->R0;
    return powf(10.0f, (aCO2 * log10f(ratio) + bCO2));
}

static double MQ135_GetSmoke_PPM(mq135_sensor_t *hmq)
{
    float RS    = MQ135_GetResistance(hmq);
    float ratio = RS / hmq->R0;
    return powf(10.0f, (aSmoke * log10f(ratio) + bSmoke));
}

void AQ_Init(mq135_sensor_t *hmq_in, mq135_sensor_t *hmq_out)
{
    MQ135_Init(hmq_in,
               &hadc,
               ADC_CHANNEL_0,
               MQ135_DEFAULT_AVERAGE,
               MQ135_DEFAULT_ADC_BITS,
               MQ135_DEFAULT_VREF);
    MQ135_SetRL(hmq_in, 10.0f); // 로드 저항 값 설정 (보통 10k옴)
    MQ135_SetR0(hmq_in, 9.83f); // 깨끗한 공기에서의 초기 R0 값 (직접 보정해서
                                // 측정하는 게 가장 정확)
    MQ135_Init(hmq_out,
               &hadc,
               ADC_CHANNEL_1,
               MQ135_DEFAULT_AVERAGE,
               MQ135_DEFAULT_ADC_BITS,
               MQ135_DEFAULT_VREF);
    MQ135_SetRL(hmq_out, 10.0f); // 로드 저항 값 설정 (보통 10k옴)
    MQ135_SetR0(hmq_out, 9.83f); // 깨끗한 공기에서의 초기 R0 값 (직접 보정해서
                                 // 측정하는 게 가장 정확)
}

void AQ_Read(mq135_sensor_t *hmq_in,
             mq135_sensor_t *hmq_out,
             mq135_data_t   *mq135)
{
    mq135->benzene_ppm_in = MQ135_GetPPM(hmq_in) / 10.000;
    mq135->co_ppm_in      = MQ135_GetCO_PPM(hmq_in) / 9.00;
    mq135->co2_ppm_in     = MQ135_GetCO2_PPM(hmq_in) / 5.00;
    mq135->smoke_ppm_in   = MQ135_GetSmoke_PPM(hmq_in) / 50.00;

    mq135->benzene_ppm_out = MQ135_GetPPM(hmq_out) / 10.000;
    mq135->co_ppm_out      = MQ135_GetCO_PPM(hmq_out) / 9.00;
    mq135->co2_ppm_out     = MQ135_GetCO2_PPM(hmq_out) / 5.00;
    mq135->smoke_ppm_out   = MQ135_GetSmoke_PPM(hmq_out) / 50.00;

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
}
