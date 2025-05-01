#include "sunroof_control.h"

// 정규화 함수
float Normalize(float value, float threshold) {
    return value / threshold;
}
// 공기질 가중 합 지수 계산 함수
float Calculate_WeightedAirQualityIndex_In(float benzene, float co, float co2, float smoke) {
    float normalized_benzene = Normalize(benzene, 0.003f);   // WHO 기준
    float normalized_co      = Normalize(co, 10.0f);
    float normalized_co2     = Normalize(co2, 1000.0f);
    float normalized_smoke   = Normalize(smoke, 10.0f);

    return normalized_benzene * 0.15f +
    	   normalized_co      * 0.30f +
		   normalized_co2     * 0.30f +
		   normalized_smoke   * 0.25f;
}
float Calculate_WeightedAirQualityIndex_Out(float benzene, float co, float co2, float smoke, float pm25) {
    float normalized_benzene = Normalize(benzene, 0.003f);   // WHO 기준
    float normalized_co      = Normalize(co, 10.0f);
    float normalized_co2     = Normalize(co2, 1000.0f);
    float normalized_smoke   = Normalize(smoke, 10.0f);
    float normalized_pm25	 = Normalize(pm25, 15.0f);

    return normalized_benzene * 0.10f +
    	   normalized_co      * 0.20f +
		   normalized_co2     * 0.20f +
		   normalized_smoke   * 0.15f +
		   normalized_pm25    * 0.35;
}

// 선루프 상태 결정 후 상태 값 리턴턴
int Send_Sunroof_Command_CGW_to_SUN(float temp_in, float temp_out, float humi_in, float humi_out,
	    float benzene_in, float co_in, float co2_in, float smoke_in,
	    float benzene_out, float co_out, float co2_out, float smoke_out, float pm25_out,
	    int light, int rain_detected, float velocity, int current_state)
{
	int need_vent = 0; // 틸팅 되는지 확인

	// 1. 고속 주행 시 무조건 닫힘
    if (velocity > VELOCITY_THRESHOLD && current_state != SUNROOF_CLOSED) 
    {
        return SUNROOF_CLOSED;
    }
    // 2. 비가 올때 상황
    if (rain_detected)
    {
    	// 2-1. 속력이 30km/h 미만이면 닫힘
    	if (velocity < 30.0f && current_state != SUNROOF_CLOSED)
    	{
    	  	return SUNROOF_CLOSED;
    	}
    	// 2-2. 속력이 30km/h 이상이면 틸팅
    	if (velocity >= 30.0f && current_state == SUNROOF_CLOSED)
    	{
    		need_vent = 1;
    	}
    }

    // 3. 조도 높고 닫힘상태 아니면 닫기, 그리고 투명도도 제어해야함
    if (light > LIGHT_THRESHOLD && current_state != SUNROOF_CLOSED) {
    	need_vent = 2;
    }

    //4. 공기질 평가
    float aqi_in  = Calculate_WeightedAirQualityIndex_In(benzene_in, co_in, co2_in, smoke_in);
    float aqi_out = Calculate_WeightedAirQualityIndex_Out(benzene_out, co_out, co2_out, smoke_out, pm25_out);

    //4-1. 외부 공기질이 많이 나쁘면 그냥 닫기
     if (aqi_in < aqi_out && aqi_out > AIR_BAD_THRESHOLD)
     {
        return SUNROOF_CLOSED;
     }
    // 4-2. 내부 공기질이 외부보다 나쁘고 그 차이가 0.4 이상이면 틸팅
     if (aqi_in - aqi_out >= 0.4f )
     {
    	need_vent = 1;
     }
    // 4-3. 내부 공기질과 외부 공기질이 차이가 적으면 닫기
     if (aqi_in - aqi_out <= 0.2f && aqi_in - aqi_out >= -0.2f)
     {
    	need_vent = 2; // 닫기
     }

    // 5-1. 환기 필요 시 틸팅
    if (need_vent == 1)
    {
        return SUNROOF_TILT;
    }
    //5-2. 선루프 닫힘
    if (need_vent == 2)
    {
         return SUNROOF_CLOSED;
    }
    // 6. 아무 조건도 해당 안 되면 현상태 유지
    return current_state;
}
