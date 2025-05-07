/*
 선루프 제어상태 수정
 스마트 모드와 사용자 모드로 분리
 0 = 닫힘, 1 = 틸팅, 2 = 오픈, 3 = 스탑

 스마트 모드에서는 빗물, 속도, 내부/외부 공기질에 의해 선루프를 팅팅하느냐
 닫는냐 결정, 스마트 모드에서 오픈, 스탑을 하는 경우는 없다고 판단

 사용자 모드에서는 닫힘부터 스탑까지 모드 가능
 사용자가 원하는 명령이 현재 선루프 상태와 다르면 사용자 명령 진행
 만약 닫힘 상태가 아닌 상황에서 현재 상태와 같은 명령이 들어오면 스탑. 즉, 틸팅
 중 멈추거나 오픈중 멈추는 기능

 해당 파일에서 진행되는 과정의 결과는 선루프의 제어 명령을 리턴하는거임
 리턴받은 제어 명령을 송신 함수를 통해 STM_선루프로 보내서 제어 하면 됨

 원래 Send_Sunroof_Command_CGW_to_SUN 함수에서 Smart_Sunroof_Control 로 이름
 변경, 인자 구조체 형식으로 변경

 송수신 로직은 만들어야함

 유저가 라즈베리파이를 통해 공기질, 온도등 임계값 설정하면 그에 맞게 조절하게끔
 하는건지 의문, 이부분은 조금만 수정하면 되니까 나중에 회의 후 수정

  ** 추가 수정사항
 in_out_mode 변수 추가
 차량 내부에서 환기팬은 항상 가동되는것으로 확인
 즉, 내기, 외기모드만 정해주면 환기 팬은 항상 가동중이고 여기서 에어컨/히터를
 사용하겠다 하면 해당 팬에다가 그에 맞는 공기를 주입해주는 것

 환기 팬의 디폴트는 내기 순환모드, Smart_Sunroof_Control 함수를 통해 내기/외기
 를 결정하고 이를 main에서 fan을 on 시키면 될 것으로 보임 그 후 에어컨/히터
 사용을 결정하면 될듯

 아래 조건문을 보면 in_out_mode 변수에 값을 안넣은 조건들이 있는데 해당 부분은
 디폴트 값을 사용하겠다는 뜻. 즉, 별다른 말이 없으면 내기 모드로 순환환
 */

#include "sunroof_control.h"

#include "main.h"
#include "state.h"

SunroofInput_t     sunroof_input;
SensingThreshold_t thershold_input = {.temp_threshold      = 24.0f,
                                      .humi_threshold      = 70.f,
                                      .light_threshold     = 800.0f,
                                      .velocity_threshold  = 60.0f,
                                      .air_worst_threshold = 1.4f,
                                      .air_bad_threshold   = 1.2f,
                                      .air_soso_threshold  = 1.0f,
                                      .air_good_threshold  = 0.8f,
                                      .aqi_in_threshold    = 0.8f};
uint8_t in_out_mode = 0; // 환기 내기/외기모드 구분, 0 = 내기, 1 = 외기,
                         // 기본값은 내기 순환모드로 하면 됨
float current_in_di  = 0.0f;
float current_out_di = 0.0f;

// 정규화 함수
static float Normalize(float value, float threshold)
{
    return value / threshold;
}
// 공기질 가중 합 지수 계산 함수
static float Calculate_WeightedAirQualityIndex_In(mq135_data_t *aq)
{
    float normalized_benzene = Normalize(aq->benzene, 0.003f); // WHO 기준
    float normalized_co      = Normalize(aq->co, 10.0f);
    float normalized_co2     = Normalize(aq->co2, 1000.0f);
    float normalized_smoke   = Normalize(aq->smoke, 10.0f);

    return normalized_benzene * 0.15f + normalized_co * 0.30f
           + normalized_co2 * 0.30f + normalized_smoke * 0.25f;
}
static float Calculate_WeightedAirQualityIndex_Out(mq135_data_t *aq, float pm)
{
    float normalized_benzene = Normalize(aq->benzene, 0.003f); // WHO 기준
    float normalized_co      = Normalize(aq->co, 10.0f);
    float normalized_co2     = Normalize(aq->co2, 1000.0f);
    float normalized_smoke   = Normalize(aq->smoke, 10.0f);
    float normalized_pm25    = Normalize(pm, 15.0f);

    return normalized_benzene * 0.10f + normalized_co * 0.20f
           + normalized_co2 * 0.20f + normalized_smoke * 0.15f
           + normalized_pm25 * 0.35;
}

static air_quality_state_t convert_aqi_to_air_quality_state(float aqi_value)
{
    // AQI is below good threshold (0.8)
    if (aqi_value <= thershold_input.air_good_threshold)
    {
        return AIR_QUALITY_GOOD;
    }
    // AQI is between good (0.8) and moderate (1.0)
    if (aqi_value <= thershold_input.air_soso_threshold)
    {
        return AIR_QUALITY_MODERATE;
    }
    // AQI is between moderate (1.0) and bad (1.2)
    if (aqi_value <= thershold_input.air_bad_threshold)
    {
        return AIR_QUALITY_BAD;
    }
    // AQI is above bad threshold (1.2)
    return AIR_QUALITY_VERY_BAD;
}

static dust_state_t convert_pm25_to_dust_state(float pm25_value)
{
    // PM2.5 is 0-15: 좋음 (Good)
    if (pm25_value <= 15.0f)
    {
        return DUST_GOOD;
    }
    // PM2.5 is 16-25: 보통 (Moderate)
    if (pm25_value <= 25.0f)
    {
        return DUST_MODERATE;
    }
    // PM2.5 is 26-50: 나쁨 (Bad)
    if (pm25_value <= 50.0f)
    {
        return DUST_BAD;
    }
    // PM2.5 is 51+: 매우 나쁨 (Very Bad)
    return DUST_VERY_BAD;
}

static void update_air_quality_states(float             aqi_in,
                                      float             aqi_out,
                                      float             pm25,
                                      air_dust_level_t *air_dust_level)
{
    if (air_dust_level == NULL)
    {
        return;
    }

    // Convert AQI values to air quality states
    air_quality_state_t internal_state =
        convert_aqi_to_air_quality_state(aqi_in);
    air_quality_state_t external_state =
        convert_aqi_to_air_quality_state(aqi_out);

    // Convert PM2.5 value to dust state
    dust_state_t dust_state = convert_pm25_to_dust_state(pm25);

    // Update the air_dust_level structure
    air_dust_level->internal_air_quality_state = internal_state;
    air_dust_level->external_air_quality_state = external_state;
    air_dust_level->dust_state                 = dust_state;
}

// 선루프 상태 결정 함수
uint8_t Smart_Sunroof_Control(sunroof_t *sunroof)
{
    sensor_data_t  *data  = &sunroof->data;
    system_state_t *state = &sunroof->state;

    uint8_t need_vent = 0; // 틸팅 되는지 확인

    // 1. 조도 높고 닫힘상태 아니면 닫기
    if (data->illum > thershold_input.light_threshold
        && state->roof != SUNROOF_CLOSED)
    {
        need_vent = 0;
    }

    // 2. 공기질 평가
    float aqi_in = Calculate_WeightedAirQualityIndex_In(&data->aq[0]);
    float aqi_out =
        Calculate_WeightedAirQualityIndex_Out(&data->aq[1], data->pm);

    // 공기질 및 미세먼지 상태 업데이트
    update_air_quality_states(aqi_in, aqi_out, data->pm, &air_dust_level);

    // 2-1. 외부 공기질이 많이 나쁘면 그냥 닫기
    if (aqi_in < aqi_out && aqi_out > thershold_input.air_bad_threshold)
    {
        in_out_mode = 0; // 내기모드
        return SUNROOF_CLOSED;
    }
    // 2-2. 내부 공기질이 외부보다 나쁘고 그 차이가 0.4 이상이면 틸팅
    if (aqi_in - aqi_out >= 0.4f)
    {
        in_out_mode = 1; // 외기모드
        need_vent   = 1;
    }
    // 2-3. 내부 공기질과 외부 공기질이 차이가 적으면 닫기
    if (aqi_in - aqi_out <= 0.2f && aqi_in - aqi_out >= -0.2f
        && state->roof != SUNROOF_CLOSED)
    {
        in_out_mode = 0; // 내기모드
        need_vent   = 0; // 닫기
    }
    // 3. 고속 주행 시 무조건 닫힘
    if (data->velocity > thershold_input.velocity_threshold
        && state->roof != SUNROOF_CLOSED)
    {
        return SUNROOF_CLOSED;
    }
    // 4. 비가 올때 상황
    if (data->rain)
    {
        in_out_mode = 0; // 내기모드
        // 4-1. 속력이 30km/h 미만이면 닫힘
        if (data->velocity < 30.0f && state->roof != SUNROOF_CLOSED)
        {
            return SUNROOF_CLOSED;
        }
        // 4-2. 속력이 30km/h 이상이면 틸팅
        if (data->velocity >= 30.0f && state->roof == SUNROOF_CLOSED)
        {
            need_vent = 1;
        }
    }
    // 5-1. 환기 필요 시 틸팅
    if (need_vent == 1)
    {
        return SUNROOF_TILT;
    }
    // 5-2. 선루프 닫힘
    if (need_vent == 0)
    {
        return SUNROOF_CLOSED;
    }
    // 6. 아무 조건도 해당 안 되면 현상태 유지
    return state->roof;
}

uint8_t User_Sunroof_Control(uint8_t command, uint8_t current_state)
{
    if (command != current_state)
    {
        return command;
    }
    else if (current_state != SUNROOF_CLOSED && command == current_state)
    {
        return SUNROOF_STOP;
    }

    return KEEP_STATE; // 현재 선루프 상태와 유저 명령으로 들어온 선루프 상태가
                       // 같으면 아무런 작업 x, KEEP_STATE를 리턴하는 이유는
                       // 아무 작업도 하지 말라는 뜻
}
