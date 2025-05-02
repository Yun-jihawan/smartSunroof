#include "debug.h"

#include "usart.h"

#if (DEBUG_LEVEL > 0)
int __io_putchar(int ch)
{
    HAL_UART_Transmit(&huart1, (uint8_t *)&ch, 1, HAL_MAX_DELAY);
    return ch;
}
#endif
