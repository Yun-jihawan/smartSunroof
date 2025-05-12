#ifndef DFPLAYERMINI_H
#define DFPLAYERMINI_H

#include "stm32l0xx_hal.h"
#include <stdbool.h>

// Define constants
#define DFPLAYER_EQ_NORMAL 0
#define DFPLAYER_EQ_POP 1
#define DFPLAYER_EQ_ROCK 2
#define DFPLAYER_EQ_JAZZ 3
#define DFPLAYER_EQ_CLASSIC 4
#define DFPLAYER_EQ_BASS 5

#define DFPLAYER_DEVICE_U_DISK 1
#define DFPLAYER_DEVICE_SD 2
#define DFPLAYER_DEVICE_AUX 3
#define DFPLAYER_DEVICE_SLEEP 4
#define DFPLAYER_DEVICE_FLASH 5

// Function declarations
void DFPlayerMini_InitPlayer(UART_HandleTypeDef *huart);
void DFPlayerMini_PlayFile(int fileNumber);
void DFPlayerMini_StopPlayback(void);
void DFPlayerMini_PlayNext(void);
void DFPlayerMini_PlayPrevious(void);
void DFPlayerMini_SetVolume(uint8_t volume);
void DFPlayerMini_IncreaseVolume(void);
void DFPlayerMini_DecreaseVolume(void);
void DFPlayerMini_SetEQ(uint8_t eq);
void DFPlayerMini_EnterSleepMode(void);
void DFPlayerMini_ResetPlayer(void);
void DFPlayerMini_StartPlayback(void);
void DFPlayerMini_PausePlayback(void);
void DFPlayerMini_PlayFromFolder(uint8_t folderNumber, uint8_t fileNumber);
void DFPlayerMini_LoopFile(int fileNumber);
void DFPlayerMini_EnableLoopPlayback(void);
void DFPlayerMini_DisableLoopPlayback(void);

// Helper function declarations
void DFPlayerMini_SendData(void);
void DFPlayerMini_SendDataWithOneArg(uint8_t command, uint16_t argument);
void DFPlayerMini_SendDataWithTwoArgs(uint8_t command, uint16_t argumentHigh, uint16_t argumentLow);
void DFPlayerMini_SendDataWithNoArg(uint8_t command);
_Bool DFPlayerMini_IsDataAvailable(void);
uint16_t DFPlayerMini_CalculateCheckSum(uint8_t *buffer);
_Bool DFPlayerMini_WaitForData(void);

void init_test(UART_HandleTypeDef *huart, uint8_t volume);
#endif
