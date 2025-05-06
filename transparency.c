#include "transparency.h"


uint8_t calculate_transparency(int mode, int light_lux, int user_transparency)
{
	uint8_t transparency = 0;
	// 모드가 0이면 스마트 제어 상태
	if (mode == 0)
	{
	    // 밝기 기준은 0 ~ 2000 lux
	    if (light_lux <= 0)
	        return 100;  // 완전 투명
	    else if (light_lux >= 2000)
	        return 0;    // 완전 불투명

	    // 선형적으로 맵핑 (밝을수록 불투명하게)
	    float light_ratio = (float)light_lux / 2000.0f;
	    transparency = (uint8_t)((1.0f - light_ratio) * 100);
	}
	// 모드가 1이면 사용자 제어 상태
	else
	{
		transparency = user_transparency;
	}


    return transparency; // 0 ~ 100
}
