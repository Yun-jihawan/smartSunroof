#include "transparency.h"

#define MAX_TRANSPARENCY 100
#define MIN_TRANSPARENCY 0
#define MAX_LIGHT_LUX    220.0f
#define MIN_LIGHT_LUX    70.0f

uint8_t calculate_transparency(int light_lux)
{
    uint8_t transparency = 0;
    // 밝기 기준은 70 ~ 220 lux
    if (light_lux <= MIN_LIGHT_LUX)
        return MAX_TRANSPARENCY; // 완전 투명
    else if (light_lux >= MAX_LIGHT_LUX)
        return MIN_TRANSPARENCY; // 완전 불투명
    // 선형적으로 맵핑 (밝을수록 불투명하게)
    float light_ratio = (float)(light_lux - 70) / 150.0f;
    transparency      = (uint8_t)((1.0f - light_ratio) * 100);
    return transparency; // 0 ~ 100
}
