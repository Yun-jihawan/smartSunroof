#ifndef CGW_RPI_UART_H
#define CGW_RPI_UART_H

typedef enum
{
    AIR_QUALITY_GOOD     = 0x00U, // 좋음
    AIR_QUALITY_MODERATE = 0x01U, // 보통
    AIR_QUALITY_BAD      = 0x02U, // 나쁨
    AIR_QUALITY_VERY_BAD = 0x03U  // 매우 나쁨
} air_quality_state_t;

typedef enum
{
    DUST_GOOD     = 0x00U, // 좋음
    DUST_MODERATE = 0x01U, // 보통
    DUST_BAD      = 0x02U, // 나쁨
    DUST_VERY_BAD = 0x03U  // 매우 나쁨
} dust_state_t;

typedef enum
{
    AIR_CONDITIONER_OFF = 0x00U,
    AIR_CONDITIONER_ON  = 0x01U
} aircond_state_t;

typedef enum
{
    VENTILATION_OFF = 0x00U,
    VENTILATION_ON  = 0x01U
} ventilation_state_t;

typedef struct
{
    uint8_t internal_air_quality_stat;
    uint8_t external_air_quality_stat;
    uint8_t dust_stat;
} air_dust_level_t;

typedef struct
{
    uint8_t sunroof_stat;     // 0: 닫힘, 1: 열림
    uint8_t transparency;     // 0 ~ 100 (%)
    uint8_t airconditioner;   // 0: OFF, 1: ON
    uint8_t ventilation_mode; // 0: OFF, 1: ON
    uint8_t velocity;         // 속도
} state_cgw_to_pi_t;

#endif /* CGW_RPI_UART_H */
