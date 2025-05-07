# Sunroof STM32L073RZ

### 작업 완료
2. 조도 센서 값 수신 (timer 10s)
    * Analog Read Inside : PA1 / Outside : PA0
    
    조사 결과 10k ohm 저항 사용하는것이 적절한(?) 값이라고 하나, 현재 1K만 있어서 사용중임임
    
    이를 통한 투명도 출력을 현재 <u>__PC8__<u> 을 통해 LED Output을 해둔 상태임.

3. 빗물감지 센서 값 수신 (timer 10s)
    * ADC : PC5
    
    이를 통한 우천 여부를 현재 <u>__PB2__</u> 를 통해 LED Output을 해둔 상태임.
g
4. 모터 구동 (Sunroof)
    * Encoder Sensing : PC0 (Sensor A), PC1 (Sensor B)
    * Motor PWM : PB3 (Tim2 Ch2)
    * Motor DIR : PA6
    * Motor Brake : PC7
    * Encoder Sensing : PB10 (Sensor A), PB11 (Sensor B)
    * Motor PWM : PA7 (Tim3 Ch2)
    * Motor DIR : PA5
    * Motor Brake : PA9

5. 투명도 조절 (film) - LED로
    우선적으로 film 조절하는 값을 LED로 넣어두었음. (위에 언급됨됨)

### 05.06 진행 사항

UART 통신을 위한 Port 확보.
* UART4를 통해 Main MCU와 통신 기능 구현 완료

* UART1을 통해 ESP 보드와 통신 기능 구현 중 문제 발생.
    * UART1에서 사용하는 PA9 Pin이 Motor Brake를 위해 필요함.
    * 이를 위해 UART5로 통신하여야 할 것 같음. 우선적으로 UART5를 통한 통신 기능 구현은 정상 작동함을 확인해둔 상태임.

* 2개 Motor 사용을 위한 핀 구성 완료.
    * 현재 encoder와 Control을 위한 Pin을 모두 설정해둔 상태임. 모터 개수가 부족해 직접적인 제어 코드 구현과 테스트는 05.07에 진행해볼 예정임.
