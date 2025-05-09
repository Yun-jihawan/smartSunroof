from enum import Enum

class SunroofState(Enum):
    CLOSE = 0
    TILT = 1
    OPEN = 2
    
class AirConditionState(Enum):
    IN_OFF_OUT_OFF = 0
    IN_OFF_OUT_ON = 1
    IN_ON_OUT_OFF = 2
    IN_ON_OUT_ON = 3

class GeneralState(Enum):
    OFF = 0
    ON = 1
