#include "wh148.h"

#include "adc.h"

#define MAX_VELOCITY 200

void POT_Init()
{
    // ADC 캘리브레이션 (선택 사항이지만 정확도 향상에 도움)
    if (HAL_ADCEx_Calibration_Start(&hadc, ADC_SINGLE_ENDED) != HAL_OK)
    {
        Error_Handler();
    }
}

void POT_Read(uint16_t *velocity)
{
    uint32_t adc_value = 0;

    // 1. ADC 변환 시작
    if (HAL_ADC_Start(&hadc) != HAL_OK)
    {
        // 시작 오류 처리
        Error_Handler();
    }

    // 2. 변환 완료 대기 (폴링 방식)
    // HAL_MAX_DELAY는 무한정 대기함을 의미, 타임아웃 설정 가능
    if (HAL_ADC_PollForConversion(&hadc, HAL_MAX_DELAY) == HAL_OK)
    {
        // 3. ADC 값 읽기
        adc_value = HAL_ADC_GetValue(&hadc);

        // 4. 속도로 변환
        *velocity = (uint16_t)((adc_value / 4095.0f) * MAX_VELOCITY);

#if (DEBUG_LEVEL > 0)
        // 5. 결과 출력 (예: UART 사용 - printf 리디렉션 필요)
        printf("\r\n=== Potentiometer ===\r\n");
        printf("ADC Value: %lu, Velocity: %u km/h\r\n", adc_value, *velocity);
#endif
    }
    else
    {
        // 폴링 타임아웃 또는 오류 처리
        Error_Handler();
    }

    // 6. ADC 중지 (선택 사항, 단일 변환 후 매번 중지)
    // HAL_ADC_Stop(&hadc); // 연속 변환 모드가 아닐 때 필요 시 사용
}
