# 프로젝트 : 스마트 선루프 자동 제어 및 차양 시스템

**팀 이름** : 사파게티

**구성원** : 송기종, 안상우 이예린, 윤지환 조용일, 김혁순

## 🎯 프로젝트 목표 : 

- 선루프 자동 환기 및 차양 기능을 통해 운전 중 불필요한 조작을 줄이면서 운전자 및 탑승자에게 쾌적한 실내 환경을 제공한다.
- 이진 적인 차양 조절이 아닌, 세밀한 빛 투과율 조정 기능을 제공한다.

## **📌**주요 기능

![image](https://github.com/user-attachments/assets/5bb1ea40-95f7-4435-8a83-033dbd3718e1)


![image](https://github.com/user-attachments/assets/1b0d5334-b950-48e4-b0b6-5b342d2792ab)


### 선루프 제어 기능

- 온습도, 공기질, 미세먼지, 빗물 감지 센서를
활용하여 차량 내/외부 환경 데이터를 수집
- 환경 데이터를 기반으로 제어 로직을 거쳐
스마트 선루프 개폐 제어

### 선루프 투명도 제어 기능

- 일사량을 감지하여 선루프 투명도 제어
- SPD 필름 공급 문제로 모바일과 연동하여 모바일로 투명도 표시

### 차량 내부 장치 제어 기능

- 온습도에 따른 에어컨 제어 기능
- 내/외부 공기질을 파악해 환기모드를 내기/외기 모드 전환

### 차량 모니터링 기능

- 차량 내부 온습도를 디스플레이로 표시
- 선루프 개폐 상태를 디스플레이로 표시
- 투명도 상태를 디스플레이로 표시
- 내부 장치 상태를 디스플레이로 표시

### 기대 효과

- 주행 중 조작 부담 감소로 운전 집중도 및 안전성 향상
- 졸음 운전 위험 감소 및 쾌적한 실내 환경 유지
- 적절한 채광 제공 및 에너지 절감
- 사용 편의성과 개인 맞춤 환경 제공
- 사용자 조작 용이성 향상

---

## 🎥 시연 영상

https://youtu.be/Iv8Jby0_KCU

---

## ⚙ 하드웨어 구성

### **ECU**

**STM32_Nucelo-L073RZ(1)**

- 메인 ECU 및 CGW
- 센서 데이터 수집
- 스마트 선루프 및 투명도 제어 로직 생성
- RPI4로 상태 값 송신
- STM32_Nucelo-L073RZ(2) 로 제어명령 송신

**STM32_Nucelo-L073RZ(2)**

- Actuator ECU
- CGW로부터 센서 값 요청 신호 받고 센서 값 송신
- CGW로부터 제어 명령 수신 후 모터 구동
- 투명도 값을 모바일로 전송

**RPI4**

- HMI
- 터치 스크린으로 차량 내/외부 상태 표시
- 선루프 및 투명도 상태 표시
- 사용자 조작 입력 시 명령 값 CGW로 송신

### Sensor

**MQ-135**

- 공기질 센서
- Benzene, CO, CO2, Smoke 입력

**GP2Y1023AU0F**

- 미세먼지 센서
- PM2.5 수준의 초 미세먼지 감지 가능

**DHT11**

- 온습도 입력 센서

**WH148**

- 가변저항
- 속도 값 입력하기 위해 사용

**CdS_Cell**

- 조도 감지 센서
- 조도 값을 통해 투명도 결정

**MH-RD**

- 빗물 감지 센서
- 빗물이 감지되면 1 아니면 0으로 결정된다.

### Motor

**MB2832E-1268**

- DC 엔코더 모터
- 선루프 개폐를 담당한다.
- 두개를 사용하여 닫힘, 팅틸, 개방을 한다.

---

## 💻 소프트웨어 구성

### Skill

- **기술 스택 :** C언어, Python, Raspberrypi, UART 통신, Json, 웹 개발
- **개발 툴** : STM32CubeIDE, STM32CubeMX, VSCode, Rasbian
- **협업 툴** : Jira, Confluence, Git, GitBHub

---

## 📙 시스템 설계서

**시스템 정적 아키텍처**

- 기능별 책임이 명확한 역할 기반 모듈 분리 구조로 시스템을 설계하고, 판단 로직은 STM1에 집중하여 중앙 집중 제어 방식을 적용
- 실제 장치의 물리적 위치를 고려해 기능을 분류하고, 하드웨어 구성과 설치 편의성을 함께 반영한 구조로 설계

![image](https://github.com/user-attachments/assets/28065764-30a8-4a1f-bf0c-fd4ee019b784)


### 스마트 제어 모드 판단

![image](https://github.com/user-attachments/assets/77cda669-11f7-4983-ad5c-4c97665cd776)



- 차량 내부의 상태를 파악하고 스마트/직접 제어 모드를 수신해서 스마트 제어 명령 모드를 판단 한다.
- 우선순위는 왼쪽부터 오른쪽으로 점점 낮아지는 순으로 진행한다.

### 전체 시스템 구성도

![image](https://github.com/user-attachments/assets/bc005494-8075-47c4-ba40-dba3c717b915)

- 보드별로 UART 통신을 통해 10초 주기로 데이터를 주고 받으며 Actuator는 Web으로 BT를 통해 데이터를 송신한다.
- 사용자는 디스플레이를 통해 차량 내/외부의 공조 값을 알 수 있고 선루프 및 투명도 상태도 파악이 가능하다.
- 투명도는 모바일과 연동하여 화면 밝기 조절로 나타낸다.

---

## 💾 데이터 인터페이스

### SUN -> CGW 
* Baud rate : 9600bps
  
![image](https://github.com/user-attachments/assets/0b0c37b1-e2ff-4a4c-981c-0c6310d0f24c)

### CGW->SUN
*  Baud rate : 9600bps
*  Period : 10s
*  Request Frame : 0xFFFF
  
 ![image](https://github.com/user-attachments/assets/01f460e3-692d-446a-8ab5-144465338c8d)

 ### SUN -> SPD
 * Baud rate : 9600bps

![image](https://github.com/user-attachments/assets/853c2394-bf35-4f13-9e88-04d019b07917)

### CGW -> HMI
* Baud rate : 9600bps
* Period : 10s

![image](https://github.com/user-attachments/assets/5f1107ec-5f33-4671-b19c-5b4db3d4bae4)

* Baud rate : 9600bps
* Period : 10s

![image](https://github.com/user-attachments/assets/b3fde0cb-df99-4242-9601-522045b2c7d5)

### HMI -> CGW
* Baud rate : 9600bps

![image](https://github.com/user-attachments/assets/26e1804f-bbcd-44aa-a38d-8094ab7c4c05)


![image](https://github.com/user-attachments/assets/5e685746-a913-4096-ac62-78b8ecb1c944)


---

## 📄 Pin Map

### CGW (NUCLEO-L073RZ)

![image](https://github.com/user-attachments/assets/7ec00fb6-ce4e-44ff-b5d8-dbbb398d66c0)


### SUN (NUCLEO-L073RZ)

![image](https://github.com/user-attachments/assets/227337f3-a988-4cbf-866d-f4c882111c43)

### HMI(RPI4)

![image](https://github.com/user-attachments/assets/2c621510-c7cf-4c3b-9847-c8360b33365f)


### SPD (ESP32-WROOM-32)

![image](https://github.com/user-attachments/assets/3206fd85-4c59-484e-a075-cc46ad472a86)



## 🪜 아키텍처 및 다이어그램

<details>
  <summary>시스템 정적 아키텍처</summary>
    
   ![image](https://github.com/user-attachments/assets/28065764-30a8-4a1f-bf0c-fd4ee019b784)
   
</details>
    
<details>
  <summary>네트워크 인터페이스</summary>
    
   ![image](https://github.com/user-attachments/assets/6d3f856b-910a-4188-b84a-18741921c0a3)

   ![image](https://github.com/user-attachments/assets/29e0b738-74ec-4fb5-8d81-13c21df3057c)

   ![image](https://github.com/user-attachments/assets/3797bb65-04ec-48ba-a8f2-c4bdbca78536)
   
</details>   
    
<details>
  <summary>SW 정적 아키텍처</summary>
    
  ![image](https://github.com/user-attachments/assets/c7807b2d-d8cd-495a-9554-723741ea594e)
    
</details>
    
<details>
  <summary>SW 동적 아키텍처</summary>
    
  ![image](https://github.com/user-attachments/assets/7cb2c37b-0f42-4cc9-b0e0-3b06bdf335eb)
    
</details>    

<details>
  <summary>SUN Sequence Diagram</summary>
    
  ![image](https://github.com/user-attachments/assets/86ba25bb-22fa-4654-bd20-b5418d264e1e)
    
</details>  

<details>
  <summary>RPI Sequence Diagram</summary>
    
  ![image](https://github.com/user-attachments/assets/6bbb0111-b138-43ea-9d7d-5c30b603fd44)
    
</details>  

<details>
  <summary>CGW 흐름도</summary>
    
  ![image](https://github.com/user-attachments/assets/400eb808-7d4b-4af9-ad7d-7c3735ad37da)
    
</details> 

<details>
  <summary>HMI 흐름도</summary>
    
  ![image](https://github.com/user-attachments/assets/44878dcb-896f-48dd-ac91-c323b27ebb4f)
    
</details> 

<details>
  <summary>CGW 회로도</summary>
    
  ![image](https://github.com/user-attachments/assets/c23ffa36-95cf-4a8a-94f2-e9ef7b9843aa)

</details> 

<details>
  <summary>SUN 회로도</summary>
    
![image](https://github.com/user-attachments/assets/f9f832e1-f762-4377-8db6-d47af66b74ab)

</details> 

<details>
  <summary>RPI4 회로도</summary>
    
![image](https://github.com/user-attachments/assets/f69f8587-e785-425c-a2e3-44585be60850)

</details> 

## 🫶 감사합니다.
