#ifndef STATE_H_
#define STATE_H_

#include <stdint.h>

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

typedef enum
{
    SUNROOF_CLOSED = 0,
    SUNROOF_TILT   = 1,
    SUNROOF_OPEN   = 2,
    SUNROOF_STOP   = 3,
    KEEP_STATE     = 10
} roof_state_t;

typedef enum
{
    USER  = 0x00U,
    SMART = 0x01U
} control_mode_t;

typedef struct
{
    air_quality_state_t internal_air_quality_state;
    air_quality_state_t external_air_quality_state;
    dust_state_t        dust_state;
} air_dust_level_t;

typedef struct
{
    control_mode_t      mode;
    roof_state_t        roof;
    uint8_t             transparency;
    aircond_state_t     airconditioner;
    ventilation_state_t ventilation;
} system_state_t;

#endif /* STATE_H_ */
