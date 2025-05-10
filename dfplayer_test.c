/*
 * dfplayer_test.c
 *
 *  Created on: May 9, 2025
 *      Author: USER
 */
#include "dfplayer.h"
#include "main.h"
#include "usart.h"

void dfplayerTask(void const * argument)
{
  /* USER CODE BEGIN dfplayerTask */
  /* Infinite loop */

	// DFPlayer test
	// DFPlayer Mini 초기화
	DFPlayerMini_InitPlayer(&huart4);
	// 선루프 자동 열림 mp3 재생
	DFPlayerMini_PlayFile(1);
	HAL_Delay(2000);
	// 선루프 자동 닫힘 mp3 재생
	DFPlayerMini_PlayFile(2);
	HAL_Delay(2000);
  // 무한 루프
  for(;;)
  {

  }
  /* USER CODE END dfplayerTask */
}


