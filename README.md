# Sunroof STM32L073RZ

### 작업 완료

1. UART 통신을 위한 Port 확보.
    * UART4를 통해 Main MCU와 통신 기능 구현 완료

    * UART1을 통해 ESP 보드와 통신 기능 구현 완료.

2. 조도 센서 값 수신 (timer 10s)
    * Analog Read Inside : PA1(A1) / Outside : PA0(A0)
    
    조사 결과 10k ohm 저항 사용하는것이 적절한(?) 값이라고 하나, 현재 1K만 있어서 사용중임

3. 빗물감지 센서 값 수신 (timer 10s)
    * ADC : PC5

4. 모터 구동 (Sunroof)
    * Encoder Sensing : PC0 (Sensor A), PC1 (Sensor B)
    * Motor PWM : PB3 (Tim2 Ch2)
    * Motor DIR : PA6
    * Motor Brake : PC7
    
    * Encoder Sensing : PB10 (Sensor A), PB11 (Sensor B)
    * Motor PWM : PA7 (Tim3 Ch2)
    * Motor DIR : PA5
    * Motor Brake : PA9

5. 투명도 조절 (film)
    ESP32 보드로 전송 UART5