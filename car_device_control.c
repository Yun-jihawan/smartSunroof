 /*
 전체 개요
 현재 에어컨/히터를 조작하는 모드가 사용자인지, 스마트제어인지 구분 후 그에 맞는 함수 호출
 스마트 제어모드에서는 에어컨/히터를 끄지 않음. 꺼야하는 기준을 잡기가 애매함
 원하는 원도에 도달했다고 종료하는 경우는 없음, 만약 스마트 제어 모드에서도 끄고 싶으면 일정 시간후 종료가 가장 합리적

 사용자 모드에서는 season, operate, temp_user 입력 받는데 season은 밑에서 계절이라고 설명했지만 결국 에어컨/히터 구분하는 변수
 사용자 조작에서는 on/off 둘다 가능, 원하는 온도로 설정하지만 그냥 팬만 돌리는게 전부라서 온도 설정까지는 못할듯 합니다.
 
 */
 
 
 #include "car_device_control.h"


 // 불쾌지수 변수
 float current_in_di1;
 float current_out_di1;
 
 // 여름철 DI 수치 구하는 함수(불쾌지수)
 float calculate_discomfort_index(float temperature, float humidity) {
     return (0.81f * temperature + 0.01f * humidity * (0.99f * temperature - 14.3f) + 46.3f);
 }
 void fan_on(uint16_t GPIO_Pin)
 {
     HAL_GPIO_WritePin(GPIOA, GPIO_Pin, GPIO_PIN_SET);
     HAL_GPIO_WritePin(GPIOA, GPIO_PIN_1, GPIO_PIN_RESET); // IN LOW → 릴레이 작동 → 팬 ON
 }
 
 void fan_off(uint16_t GPIO_Pin)
 {
     HAL_GPIO_WritePin(GPIOA, GPIO_Pin, GPIO_PIN_RESET);
     HAL_GPIO_WritePin(GPIOA, GPIO_PIN_1, GPIO_PIN_SET); // IN HIGH → 릴레이 끊김 → 팬 OFF
 }
 void operation_conditioner(uint8_t season, uint8_t operate, float temp)
 {
     if (season == 0)
     {
         // 에어컨 on/off 구현, 파란불 LED 에어컨
         if (operate == 1)
         {
             fan_on(GPIO_PIN_6);
             snprintf(buf1, sizeof(buf1), "air conditioner ON");
         }
         else
         {
             fan_off(GPIO_PIN_6);
             snprintf(buf1, sizeof(buf1), "air conditioner OFF");
         }
 
     }
     else
     {
         // 히터 on/off 구현, 빨간색 LED 히터
         if (operate == 1)
         {
             fan_on(GPIO_PIN_7);
             snprintf(buf1, sizeof(buf1), "heater ON");
         }
         else
         {
             fan_off(GPIO_PIN_7);
             snprintf(buf1, sizeof(buf1), "heater OFF");
         }
 
     }
 }

 //에어컨, 히터 스마트 제어 함수
 uint8_t smart_device_command(float temp_in, float temp_out, float temp_user, float humi_in, float humi_out)
 {
     current_in_di1 = calculate_discomfort_index(temp_in, humi_in); // 현재 내부 불쾌지수
     current_out_di1 = calculate_discomfort_index(temp_out, humi_out); // 현재 외부 불쾌지수
 
     uint8_t season = 0; // 0 == 여름, 1 == 겨울 
 
     
     if (temp_out < 10.0f) // 외부 온도가 10도보다 낮으면 히터를 사용하는 계절로 판단
     {
         season = 1; 
 
         // 차량 내부 온도가 사용자가 설정한 온도보다 낮으면 히터를 사용자가 원하는 온도로 on
         if (temp_in < temp_user)
         {
             if (HAL_GPIO_ReadPin(GPIOA, GPIO_PIN_7) == GPIO_PIN_RESET) // 히터가 켜져있지 않으면 히터 on
             {
                operation_conditioner(season, 1, temp_user);             
             }
         }
     }
 
     //season이 여름으로 결정됐으므로 여름은 불쾌지수로 판단
     else if (current_in_di1 > 68)
     {
         if (HAL_GPIO_ReadPin(GPIOA, GPIO_PIN_6) == GPIO_PIN_RESET) // 에어컨이 켜져있지 않으면 에어컨on
         {
            operation_conditioner(season, 1, temp_user);
         }
     }
 
     return season; // 현재 계절을 반환, 확인용으로 해놓긴했는데 없애도 무방할듯, 오히려 에어컨, 히터 가동상태를를 리턴하는게 나을수도?
 }
 
 // 사용자 조작으로 들어올 경우 조작 함수, 
 // 메인이든 어디든 사용자모드인지 스마트제어모드인지 구분 후 smart_device_command, user_device_command 둘 중 하나 호출하면 될듯
 void user_device_command(uint8_t season, uint8_t operate, uint8_t temp_user)
 {
     operation_conditioner(season, operate, temp_user);
 }
 