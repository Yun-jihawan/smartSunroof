# Sunroof STM32L073RZ

### 작업 필요
1. Main STM으로부터 신호 송/수신 (Sunroof 개방 모드 (int), 투명도)
    Mode_bit int형 analog
    Opacity_bit int형 analog


### 작업 완료
2. 조도 센서 값 수신 (timer 1s)
    * Analog Read Inside : PA1 / Outside : PA0
    
    조사 결과 10k ohm 저항 사용하는것이 적절한(?) 값이라고 하나, 현재 1K만 있어서 사용중임임
    
    이를 통한 투명도 출력을 현재 <u>__PC8__<u> 을 통해 LED Output을 해둔 상태임.

3. 빗물감지 센서 값 수신 (timer 10s)
    * ADC : PC5
    
    이를 통한 우천 여부를 현재 <u>__PB2__</u> 를 통해 LED Output을 해둔 상태임.

4. 모터 구동 (Sunroof)
    * Encoder Sensing : PC0 (Sensor A), PC1 (Sensor B)
    * Motor PWM : PB3 (Tim2 Ch2)
    * Motor DIR : PA6
    * Motor Brake : PC7

    #### Error :  12V 전원이 안들어오는것이었음... <br> 현재 5V로 전원 인가하는 부분에서는 문제가 생기지 않도록 구현해 둔 상태이나, 이따가 가서 케이블 바꾸고 해봐야 할 것 같음..

5. 투명도 조절 (film) - LED로
    우선적으로 film 조절하는 값을 LED로 넣어두었음. (위에 언급됨됨)