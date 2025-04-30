
#include "mq135.h"
#include <math.h>

static const float aBezneze = 23.4461175f;
static const float bBenzene = -3.98724605f;
static const float aCor = 0.00035f;
static const float bCor = 0.02718f;
static const float cCor = 1.39538f;
static const float dCor = 0.0018f;

// Additional gas curve coefficients
static const float aCO = -0.36f;
static const float bCO = 1.72f;
static const float aCO2 = -0.38f;
static const float bCO2 = 3.75f;
static const float aSmoke = -0.42f;
static const float bSmoke = 2.90f;

static float MQ135_ReadAverageADC(MQ135_HandleTypeDef* hmq) {
    if (hmq == NULL || hmq->hadc == NULL) return 0.0f;

    float sum = 0;
    ADC_ChannelConfTypeDef sConfig = {0};

    sConfig.Channel = hmq->adc_channel;
    //sConfig.Rank = ADC_REGULAR_RANK_1;
    //sConfig.SamplingTime = ADC_SAMPLETIME_39CYCLES_5;

    HAL_ADC_ConfigChannel(hmq->hadc, &sConfig);

    for (int i = 0; i < hmq->average_count; i++) {
        HAL_ADC_Start(hmq->hadc);
        if (HAL_ADC_PollForConversion(hmq->hadc, HAL_MAX_DELAY) == HAL_OK) {
            sum += HAL_ADC_GetValue(hmq->hadc);
        }
        HAL_ADC_Stop(hmq->hadc);
    }

    return sum / hmq->average_count;
}

static float MQ135_GetResistance(MQ135_HandleTypeDef* hmq) {
    float adc_avg = MQ135_ReadAverageADC(hmq);
    float voltage = (adc_avg / ((1 << hmq->adc_bits) - 1)) * hmq->vref;
    if (voltage <= 0.0f) return 1e9f; // Avoid divide-by-zero
    return ((hmq->vref / voltage) - 1.0f) * hmq->RL;
}

static float MQ135_GetCorrection(float t, float h) {
    return aCor * t * t - bCor * t + cCor - (h - 33.0f) * dCor;
}

void MQ135_Init(MQ135_HandleTypeDef* hmq, ADC_HandleTypeDef* hadc, uint32_t channel, int average, uint8_t bits, float vref) {
    if (hmq == NULL) return;
    hmq->hadc = hadc;
    hmq->adc_channel = channel;
    hmq->adc_bits = bits;
    hmq->vref = vref;
    hmq->average_count = average;
    hmq->RL = MQ135_DEFAULT_RL;
    hmq->R0 = 1.0f;
}

void MQ135_SetRL(MQ135_HandleTypeDef* hmq, float RL) {
    if (hmq) hmq->RL = RL;
}

void MQ135_SetR0(MQ135_HandleTypeDef* hmq, float R0) {
    if (hmq) hmq->R0 = R0;
}

float MQ135_Calibrate(MQ135_HandleTypeDef* hmq, int cPPM) {
    float RS = MQ135_GetResistance(hmq);
    hmq->R0 = RS * powf((float)cPPM / aBezneze, 1.0f / -bBenzene);
    return hmq->R0;
}

double MQ135_GetPPM(MQ135_HandleTypeDef* hmq) {
    float RS = MQ135_GetResistance(hmq);
    return aBezneze * powf(RS / hmq->R0, bBenzene);
}

double MQ135_GetCorrectedPPM(MQ135_HandleTypeDef* hmq, float temperature, float humidity) {
    float RS = MQ135_GetResistance(hmq) / MQ135_GetCorrection(temperature, humidity);
    return aBezneze * powf(RS / hmq->R0, bBenzene);
}

double MQ135_GetCO_PPM(MQ135_HandleTypeDef* hmq) {
    float RS = MQ135_GetResistance(hmq);
    float ratio = RS / hmq->R0;
    return powf(10.0f, (aCO * log10f(ratio) + bCO));
}

double MQ135_GetCO2_PPM(MQ135_HandleTypeDef* hmq) {
    float RS = MQ135_GetResistance(hmq);
    float ratio = RS / hmq->R0;
    return powf(10.0f, (aCO2 * log10f(ratio) + bCO2));
}

double MQ135_GetSmoke_PPM(MQ135_HandleTypeDef* hmq) {
    float RS = MQ135_GetResistance(hmq);
    float ratio = RS / hmq->R0;
    return powf(10.0f, (aSmoke * log10f(ratio) + bSmoke));
}
