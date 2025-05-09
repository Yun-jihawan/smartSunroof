#include "uart_cgw_rpi.h"
#include "main.h"

ThresholdData_from_rpi_t thresholdData;
ControlData_from_rpit controlData;

void send_cgw_to_rpi_init(void){
	controlData.mode = AUTOMATIC;
}

extern uint8_t rx_data[5]; // 수신 버퍼
void HAL_UART_RxCpltCallback(UART_HandleTypeDef *huart)
{
  if (huart->Instance == USART4)
  {
    HAL_GPIO_TogglePin(GPIOA, GPIO_PIN_5);
    uint8_t debug_msg[5];

    sprintf((char *)debug_msg, "%d %d %d %d %d\r\n", rx_data[0], rx_data[1], rx_data[2], rx_data[3], rx_data[4]);
    printf("identifier: %d\r\n", rx_data[0]);
    int identifier = rx_data[0];
    switch(identifier) {
    	case CONTROL_IDENTIFIER:
    		thresholdData.temp_threshold = rx_data[1];
    		thresholdData.air_quality_threshold = rx_data[2];
    		thresholdData.dust_threshold = rx_data[3];
    		thresholdData.light_threshold = rx_data[4];
    		break;
    	case THRES_IDENTIFIER:
    		controlData.roof = rx_data[1];
    		controlData.transparency = rx_data[2];
    		controlData.air_control = rx_data[3];
    		controlData.mode = RECEIVED_MASK & rx_data[4]; // 사용자 모드이면 안보내기
    	    printf("received : %d\r\n", controlData.mode);

    		break;
    	default:
			// 에러 처리: 잘못된 식별자
			const char* err_msg = "ERR: Invalid identifier\r\n";
			HAL_UART_Transmit(&huart2, (uint8_t *)err_msg, strlen(err_msg), 100);

			// 예시: 에러 LED 점등 (e.g., GPIOA Pin 5)
			HAL_GPIO_WritePin(GPIOA, GPIO_PIN_5, GPIO_PIN_SET);

			// 필요 시: 에러 카운트 증가, 무시, 로그 저장 등
			break;
    }

    // 받은 데이터를 다시 전송 (Echo)
    HAL_UART_Transmit(&huart2, debug_msg, strlen((char *)debug_msg), 100);

    // 다시 수신 대기 (인터럽트 재등록)
    HAL_UART_Receive_DMA(&huart4, rx_data, 5);
  }
}

void send_device_state(state_cgw_to_pi_t *stat) {
    uint8_t header = 0xFF; // 식별자 1
    uint8_t buffer[8];

    // 전송 포맷: [0xFF, 선루프 상태, 투명도, 에어컨, 자동/수동모드, 속도, 0x00, 0x00]
    buffer[0] = (uint8_t)header;
    buffer[1] = (uint8_t)stat->roof;
    buffer[2] = (uint8_t)stat->transparency;
    buffer[3] = (uint8_t)stat->airconditioner;
    buffer[4] = (uint8_t)stat->mode;
    buffer[5] = (uint8_t)stat->velocity;
    buffer[6] = (uint8_t)0x00;
    buffer[7] = (uint8_t)0x00;

    HAL_UART_Transmit(&huart4, buffer, sizeof(buffer), HAL_MAX_DELAY);
}

void send_sensor_data(dht11_data_t *dht11, air_dust_level_t *air_dust_stat) {
    uint8_t header = 0xFE; // 시작 식별자
    uint8_t buffer[8];

    // 전송 포맷: [0xFE, 내부 온도, 내부 습도, 외부 온도, 외부 습도, 내부 공기질, 외부 공기질, 미세먼지]
    buffer[0] = (uint8_t)header;
    buffer[1] = (uint8_t)dht11[0].temp;
    buffer[2] = (uint8_t)dht11[0].rh;
    buffer[3] = (uint8_t)dht11[1].temp;
    buffer[4] = (uint8_t)dht11[1].rh;
    buffer[5] = (uint8_t)air_dust_stat->internal_air_quality_state;
    buffer[6] = (uint8_t)air_dust_stat->external_air_quality_state;
    buffer[7] = (uint8_t)air_dust_stat->dust_state;

    HAL_UART_Transmit(&huart4, buffer, sizeof(buffer), HAL_MAX_DELAY);
}
