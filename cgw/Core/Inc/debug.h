#ifndef DEBUG_H_
#define DEBUG_H_

#ifndef DEBUG_LEVEL
#define DEBUG_LEVEL 0
#endif

#if (DEBUG_LEVEL > 0)
// printf() 호출 시 HardFault가 발생한다면 CubeMX의 FREERTOS 설정에서
// Config paramters > Memory Management scheme을 heap_3으로 설정해야 합니다.
#include "printf.h"
#endif

#endif /* DEBUG_H_ */
