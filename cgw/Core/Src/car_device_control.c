/*
전체 개요
현재 에어컨/히터를 조작하는 모드가 사용자인지, 스마트제어인지 구분 후 그에 맞는
함수 호출 스마트 제어모드에서는 에어컨/히터를 끄지 않음. 꺼야하는 기준을 잡기가
애매함 원하는 원도에 도달했다고 종료하는 경우는 없음, 만약 스마트 제어
모드에서도 끄고 싶으면 일정 시간후 종료가 가장 합리적

사용자 모드에서는 season, operate, temp_user 입력 받는데 season은 밑에서
계절이라고 설명했지만 결국 에어컨/히터 구분하는 변수 사용자 조작에서는 on/off
둘다 가능, 원하는 온도로 설정하지만 그냥 팬만 돌리는게 전부라서 온도 설정까지는
못할듯 합니다.


** 2025.05.04 추가 수정 사항
환기 모드 팬은 내기/외기 둘중 하나로 항상 가동
에어컨/히터는 이미 돌아가고있는 팬에 그에 맞는 공기를 주입하는 것

기존 fan_on 함수 -> conditioner_on, fan_off -> conditioner_off
fan_on/fan_off 함수는 팬을 내기/외기 모드로 키고 끄는 것
conditioner_on / conditioner_off 함수는 에어컨/히터 on/off 함수

내기/외기 모드를 구분하는건 나중에 led로 보여줘도 될듯

사용자가 자동차 시동을 켰을 때
1. 선루프 제어 함수를 통해 선루프 상태와 내기/외기 순환 모드를 결정
2. fan_on 함수 호출을 통해 내기/외기 모드로 팬 on
3. smart_device_command 함수를 통해 에어컨/히터 제어
*/

#include "car_device_control.h"

#include "state.h"
#include "sunroof_control.h"

#define TEMP_THRESHOLD             10.0f
#define DISCOMFORT_INDEX_THRESHOLD 68.0f

float current_in_di1;
float current_out_di1;

static float calculate_discomfort_index(float temperature, float humidity)
{
    return (0.81f * temperature
            + 0.01f * humidity * (0.99f * temperature - 14.3f) + 46.3f);
}
// static void fan_on()
// {
//     if (in_out_mode == 0)
//     {
//         // 내기모드
//     }
//     else
//     {
//         // 외기모드
//     }
//     HAL_GPIO_WritePin(GPIOA,
//                       GPIO_PIN_1,
//                       GPIO_PIN_RESET); // IN LOW → 릴레이 작동 → 팬 ON
// }
// static void fan_off()
// {
//     if (in_out_mode == 0)
//     {
//         // 내기모드
//     }
//     else
//     {
//         // 외기모드
//     }
//     HAL_GPIO_WritePin(GPIOA,
//                       GPIO_PIN_1,
//                       GPIO_PIN_SET); // IN HIGH → 릴레이 끊김 → 팬 OFF
// }
static void conditioner_on(uint16_t GPIO_Pin)
{
    HAL_GPIO_WritePin(GPIOA, GPIO_Pin, GPIO_PIN_SET);
}

static void conditioner_off(uint16_t GPIO_Pin)
{
    HAL_GPIO_WritePin(GPIOA, GPIO_Pin, GPIO_PIN_RESET);
}
static void operation_conditioner(uint8_t season, uint8_t operate, float temp)
{
    if (season == 0)
    {
        // 에어컨 on/off 구현, 파란불 LED 에어컨
        if (operate == 1)
        {
            conditioner_on(GPIO_PIN_6);
        }
        else
        {
            conditioner_off(GPIO_PIN_6);
        }
    }
    else
    {
        // 히터 on/off 구현, 빨간색 LED 히터
        if (operate == 1)
        {
            conditioner_on(GPIO_PIN_7);
        }
        else
        {
            conditioner_off(GPIO_PIN_7);
        }
    }
}
uint8_t smart_device_command(dht11_data_t *dht, uint8_t temp_user)
{
    float temp_in  = dht[0].temp; // 내부 온도
    float humi_in  = dht[0].rh;   // 내부 습도
    float temp_out = dht[1].temp; // 외부 온도
    float humi_out = dht[1].rh;   // 외부 습도

    // 불쾌지수 계산
    current_in_di1 =
        calculate_discomfort_index(temp_in, humi_in); // 현재 내부 불쾌지수
    current_out_di1 =
        calculate_discomfort_index(temp_out,
                                   humi_out); // 현재 외부 불쾌지수

    uint8_t aircond_state = 0;
    uint8_t season = 0; // 0 == 여름, 1 == 겨울

    if (temp_out < TEMP_THRESHOLD)
    {
        season = 1; // 외부 온도가 10도보나 낮으면 히터를 사용하는 계절로 인식

        // 차량 내부 온도가 사용자가 설정한 온도보다 낮으면 히터를 사용자가
        // 원하는 온도로 on
        if (temp_in < temp_user)
        {
            if (HAL_GPIO_ReadPin(GPIOA, GPIO_PIN_7)
                == GPIO_PIN_RESET) // 온도가 낮은데 히터가 안켜져 있으면
                                   // 히터 키기
            {
                operation_conditioner(season, 1, temp_user);
            }
            aircond_state = AIR_CONDITIONER_IN;
        }
    }

    // season이 여름으로 결정됐으므로 여름은 불쾌지수로 판단
    else if (current_in_di1 > DISCOMFORT_INDEX_THRESHOLD)
    {
        if (HAL_GPIO_ReadPin(GPIOA, GPIO_PIN_6) == GPIO_PIN_RESET)
        {
            operation_conditioner(season, 1, temp_user);
        }
        aircond_state = AIR_CONDITIONER_IN;
    }
    return aircond_state | in_out_mode;
}

void user_device_command(uint8_t season, uint8_t operate, uint8_t temp_user)
{
    operation_conditioner(season, operate, temp_user);
}
