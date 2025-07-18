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
    AIR_CONDITIONER_OUT  = 0x01U,
    AIR_CONDITIONER_IN = 0x02U,
    AIR_CONDITIONER_MAX = 0x03U
} aircond_state_t;

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
    MODE_MANUAL = 0x00U,
    MODE_SMART  = 0x01U
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
} system_state_t;

#endif /* STATE_H_ */
