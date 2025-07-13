#include "dfplayer.h"

static UART_HandleTypeDef *_huart;
static uint8_t _sending[10];
static uint8_t _received[10];
static uint8_t _isSending = 0;
static uint32_t _timeOutTimer = 0;

// Initialize the DFPlayer Mini with the provided UART handle
void DFPlayerMini_InitPlayer(UART_HandleTypeDef *huart) {
    _huart = huart;

    // Initialize the sending buffer with default values
    _sending[0] = 0x7E;	 // start
    _sending[1] = 0xFF;	 // version
    _sending[2] = 0x06;  // Data length
    _sending[9] = 0xEF;	 // End
    DFPlayerMini_SetVolume(10);
}

void init_test(UART_HandleTypeDef *huart, uint8_t volume) {
	DFPlayerMini_SendDataWithTwoArgs(0x0C, 0x00, 0x00); // reset
//	Printf("DF Reset \r\n");
	HAL_Delay(2000);

	DFPlayerMini_SendDataWithTwoArgs(0x3F, 0x00, 0x00); // initialize parameters
//	Printf("Send Cmd that initialize \r\n");
	HAL_Delay(200);

	DFPlayerMini_SetVolume(volume); // initialize volume
//	Printf("Send Cmd that change volume \r\n");
	HAL_Delay(500);
}

void DFPlayerMini_SendData(void) {
    if (_sending[4]) {  // ACK 모드가 활성화 되어 있으면 마지막 전송이 끝날 때까지 대기
        while (_isSending) {
            HAL_Delay(0);  // STM32에서는 delay(0) 대신 HAL_Delay(0)을 사용하지만, 사실 0은 아무 작업도 하지 않음
            DFPlayerMini_WaitForData();  // 데이터가 올 때까지 대기
        }
    }

    // USART를 통해 데이터를 전송
    if (HAL_UART_Transmit(_huart, _sending, 10, 500) != HAL_OK) {
        // 전송 오류 처리 (필요시 추가)
    }

    _timeOutTimer = HAL_GetTick();  // 현재 시간 기록 (타임아웃 측정)
    _isSending = _sending[4];  // ACK 상태에 따라 _isSending을 설정

    if (!_sending[4]) {  // ACK 모드가 비활성화 되어 있으면 10ms 대기
        HAL_Delay(10);
    }
}

// Send the data stack to the DFPlayer Mini (with command and two arguments)
void DFPlayerMini_SendDataWithNoArg(uint8_t command) {
	DFPlayerMini_SendDataWithOneArg(command, 0);
}

// Send the data stack to the DFPlayer Mini (with command and one argument)
void DFPlayerMini_SendDataWithOneArg(uint8_t command, uint16_t argument) {
    _sending[3] = command;
    DFPlayerMini_Uint16ToArray(argument, _sending+5);
    DFPlayerMini_Uint16ToArray(DFPlayerMini_CalculateCheckSum(_sending), _sending+7);
    DFPlayerMini_SendData();
}

// Send the data stack to the DFPlayer Mini (with command and two 16-bit arguments)
void DFPlayerMini_SendDataWithTwoArgs(uint8_t command, uint16_t argumentHigh, uint16_t argumentLow) {
	DFPlayerMini_SendDataWithOneArg(command, (argumentHigh << 8) | argumentLow);
}

//// Send the data stack to the DFPlayer Mini (with command and no argument)
//void DFPlayerMini_SendDataWithNoArg(uint8_t command) {
//    _sending[3] = command;
//    DFPlayerMini_Uint16ToArray(DFPlayerMini_CalculateCheckSum(_sending), &_sending[7]);
//
//    HAL_UART_Transmit(_huart, _sending, 10, 500);
//}

void DFPlayerMini_PlayFile(int fileNumber) {
    DFPlayerMini_SendDataWithOneArg(0x03, fileNumber);
}

void DFPlayerMini_StopPlayback(void) {
    DFPlayerMini_SendDataWithNoArg(0x16);  // No argument, just the command
}

void DFPlayerMini_PlayNext(void) {
    DFPlayerMini_SendDataWithNoArg(0x01);  // No argument, just the command
}

void DFPlayerMini_PlayPrevious(void) {
    DFPlayerMini_SendDataWithNoArg(0x02);  // No argument, just the command
}

void DFPlayerMini_SetVolume(uint8_t volume) {
    DFPlayerMini_SendDataWithOneArg(0x06, volume);
}

void DFPlayerMini_IncreaseVolume(void) {
    DFPlayerMini_SendDataWithNoArg(0x04);  // No argument, just the command
}

void DFPlayerMini_DecreaseVolume(void) {
    DFPlayerMini_SendDataWithNoArg(0x05);  // No argument, just the command
}

void DFPlayerMini_SetEQ(uint8_t eq) {
    DFPlayerMini_SendDataWithOneArg(0x07, eq);
}

void DFPlayerMini_EnterSleepMode(void) {
    DFPlayerMini_SendDataWithNoArg(0x0A);  // No argument, just the command
}

void DFPlayerMini_ResetPlayer(void) {
    DFPlayerMini_SendDataWithNoArg(0x0C);  // No argument, just the command
}

void DFPlayerMini_StartPlayback(void) {
    DFPlayerMini_SendDataWithNoArg(0x0D);  // No argument, just the command
}

void DFPlayerMini_PausePlayback(void) {
    DFPlayerMini_SendDataWithNoArg(0x0E);  // No argument, just the command
}

void DFPlayerMini_PlayFromFolder(uint8_t folderNumber, uint8_t fileNumber) {
	DFPlayerMini_SendDataWithTwoArgs(0x0F, folderNumber, fileNumber);
}

void DFPlayerMini_LoopFile(int fileNumber) {
    DFPlayerMini_SendDataWithOneArg(0x08, fileNumber);
}

void DFPlayerMini_EnableLoopPlayback(void) {
    DFPlayerMini_SendDataWithNoArg(0x19);  // No argument, just the command
}

void DFPlayerMini_DisableLoopPlayback(void) {
    DFPlayerMini_SendDataWithNoArg(0x19);  // No argument, just the command
}

// Wait for data from DFPlayer Mini to become available
_Bool DFPlayerMini_WaitForData(void) {
	uint32_t startTick = HAL_GetTick();  // 타임아웃 시작 시간
	while (!HAL_UART_Receive(_huart, _received, 10, 500)) {
		if (HAL_GetTick() - startTick > 500) {  // 500ms 타임아웃
			return 0;  // 타임아웃 발생
		}
	}
	return 1;  // 데이터가 정상적으로 수신됨
}

// Check if data is available from DFPlayer Mini
_Bool DFPlayerMini_IsDataAvailable(void) {
    return HAL_UART_Receive(_huart, _received, 10, 500) == HAL_OK;
}

// Parse the data received from DFPlayer Mini
void DFPlayerMini_ParseData(void) {
    // You can parse the received data here if needed
}

// Validate the received stack by checking checksum
bool DFPlayerMini_ValidateReceivedData(void) {
    return DFPlayerMini_CalculateCheckSum(_received) == uint16ToUint16(&_received[7]);
}

// Convert a 16-bit value into two bytes
void DFPlayerMini_Uint16ToArray(uint16_t value, uint8_t *array) {
    *array = (uint8_t)(value >> 8);
    *(array + 1) = (uint8_t)(value);
}

// Calculate the checksum for the data
uint16_t DFPlayerMini_CalculateCheckSum(uint8_t *buffer) {
    uint16_t sum = 0;
    for (int i = 1; i < 7; i++) {
        sum += buffer[i];
    }
    return -sum;
}

// Convert two bytes into a 16-bit value
uint16_t DFPlayerMini_ArrayToUint16(uint8_t *array) {
    uint16_t value = *array;
    value <<= 8;
    value += *(array + 1);
    return value;
}
