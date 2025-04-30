# Sunroof STM32L073RZ

1. Main STM으로부터 제어 신호 수신 (Sunroof 개방 모드 (int), 투명도)
    Mode_bit int형 analog
    Opacity_bit int형 analog

4. 모터 구동 (Sunroof)

5. 투명도 조절 (flim)
    -> 우선적으로 film 조절하는 값을 LED로 넣어두었음.

### 완료
2. 조도 센서 값 수신 (1s)
    Analog Read, 10k ohm 저항
    Inside : PA1 / Outside : PA0

3. 빗물감지 센서 값 수신 (10s)
    Analog Read
    PC5