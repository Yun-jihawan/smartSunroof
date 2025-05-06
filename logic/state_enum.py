from enum import Enum

class SunroofState(Enum):
    CLOSE = 0
    TILTING = 1
    OPEN = 2

class AirConditionState(Enum):
    IN_OFF_OUT_OFF = 0
    IN_OFF_OUT_ON = 1
    IN_ON_OUT_OFF = 2
    IN_ON_OUT_ON = 3

class GeneralState(Enum):
    OFF = 0
    ON = 1

class SensitivityState(Enum):
    OFF = 0
    LOW = 1
    MID = 2
    HIGH = 3

class DataType(Enum):
    USER_SETTING = 0 # freeset button clicked -> sendData(DataType(FREESET))
    FREESET = 1 # user seetting modified -> sendData(DataType(USER_SETTING))
