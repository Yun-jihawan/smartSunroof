#include "debug.h"

#include "usart.h"

#if (DEBUG_LEVEL > 0)
void _putchar(char ch)
{
    HAL_UART_Transmit(&huart2, (uint8_t *)&ch, 1, HAL_MAX_DELAY);
}
#endif
