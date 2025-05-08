#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <BLE2902.h> // BLE Descriptor for notifications
#include <HardwareSerial.h> // 시리얼 통신을 위해 추가

// --- Configuration ---
// Android app (BrightnessOverlayService.kt)에서 정의된 UUID와 일치해야 합니다.
#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"
#define DEVICE_NAME         "ESP32_Brightness"

// NUCLEO 보드와 통신할 시리얼 포트 설정 (Serial2 사용)
// ESP32의 Serial2 기본 핀: RX = GPIO 16, TX = GPIO 17
// 실제 연결에 맞게 핀 번호를 변경해야 할 수 있습니다.
// NUCLEO 보드의 시리얼 TX 핀 -> ESP32의 RX2 핀 (GPIO 16)
// NUCLEO 보드의 시리얼 RX 핀 -> ESP32의 TX2 핀 (GPIO 17)
// NUCLEO 보드와 ESP32의 GND를 서로 연결해야 합니다.
#define NUCLEO_SERIAL_BAUD_RATE 115200 // NUCLEO 보드의 시리얼 통신 속도와 일치시켜야 함
HardwareSerial NucleoSerial(2); // Use Serial2 (UART2)

// --- Global Variables ---
BLECharacteristic *pCharacteristic; // BLE 특성 객체
bool deviceConnected = false;       // 클라이언트 연결 상태 플래그

uint8_t currentBrightnessValue = 0; // 현재 밝기 값 (점진적으로 변경될 값)
uint8_t targetBrightnessValue = 0;  // NUCLEO로부터 수신한 목표 밝기 값
uint8_t lastSentBrightnessValue = 0;// 마지막으로 BLE 전송한 밝기 값

unsigned long lastSendTime = 0;     // 마지막 BLE 전송 시간
const long sendInterval = 10;      // BLE 전송 간격 (밀리초) - 필요시 조정

// 점진적 밝기 변경을 위한 변수
const uint8_t brightnessChangeStep = 1;     // 한 번에 변경할 밝기 단계 (1이 가장 부드러움)
unsigned long lastBrightnessUpdateTime = 0;   // 마지막 밝기 점진적 변경 시간
const long brightnessUpdateInterval = 10;   // 밝기 점진적 변경 간격 (밀리초) - 값이 작을수록 부드럽지만 CPU 사용량 증가

// --- BLE Server Callbacks ---
// 클라이언트 연결/해제 시 호출될 콜백 클래스
class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
      Serial.println("Device connected via BLE");
    }

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
      Serial.println("Device disconnected from BLE");
      // 연결이 끊어지면 다시 광고를 시작합니다.
      BLEDevice::startAdvertising();
      Serial.println("Start BLE advertising");
    }
};

// --- Setup ---
// 초기화 코드 (한 번 실행)
void setup() {
  Serial.begin(115200); // 시리얼 모니터용 (디버깅)
  Serial.println("Starting ESP32 BLE Sender (Receiving from Nucleo with Gradual Brightness)");

  // NUCLEO 보드와 통신할 시리얼 포트(Serial2) 초기화
  NucleoSerial.begin(NUCLEO_SERIAL_BAUD_RATE, SERIAL_8N1, 16, 17); // 핀 명시적 지정 (RX, TX)
  Serial.println("Serial2 initialized for Nucleo communication.");

  // 1. BLE 장치 초기화
  BLEDevice::init(DEVICE_NAME);

  // 2. BLE 서버 생성
  BLEServer *pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  // 3. BLE 서비스 생성
  BLEService *pService = pServer->createService(SERVICE_UUID);

  // 4. BLE 특성 생성 (읽기 및 알림 속성)
  pCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID,
                      BLECharacteristic::PROPERTY_READ   |
                      BLECharacteristic::PROPERTY_NOTIFY
                    );

  // 5. 특성에 대한 설명자 추가 (알림 활성화에 필요)
  pCharacteristic->addDescriptor(new BLE2902());

  // 6. 서비스 시작
  pService->start();

  // 7. 광고 시작
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x0);
  BLEDevice::startAdvertising();
  Serial.println("BLE Characteristic defined! Now advertising...");
}

// --- Loop ---
// 반복 실행 코드
void loop() {
  unsigned long currentTime = millis(); // 현재 시간 캐싱

  // 1. NUCLEO 보드로부터 시리얼 데이터 수신 확인
  if (NucleoSerial.available() > 0) {
    // NUCLEO가 1바이트 밝기 값을 보낸다고 가정
    uint8_t receivedValue = NucleoSerial.read();

    // [0, 100] 범위 내에 있는지 확인
    if (receivedValue <= 100) {
      // [0, 100] 범위의 값을 [0, 255] 범위로 매핑
      uint8_t mappedValue = map(receivedValue, 0, 100, 0, 255);

      if (mappedValue != targetBrightnessValue) { // 목표값이 변경된 경우에만 업데이트
        targetBrightnessValue = mappedValue;
        Serial.print("Received from Nucleo (Original [0-100]): ");
        Serial.println(receivedValue);
        Serial.print("Mapped to [0-255]: ");
        Serial.println(targetBrightnessValue);
      }
    } else {
      // 범위를 벗어난 값은 무시
      Serial.print("Ignored out-of-range brightness value: ");
      Serial.println(receivedValue);
    }
  }

  // 2. 밝기 값 점진적으로 변경
  if (currentTime - lastBrightnessUpdateTime >= brightnessUpdateInterval) {
    if (currentBrightnessValue != targetBrightnessValue) {
      if (currentBrightnessValue < targetBrightnessValue) {
        // 목표값까지의 차이가 step보다 크거나 같으면 step만큼 증가
        if (targetBrightnessValue - currentBrightnessValue >= brightnessChangeStep) {
          currentBrightnessValue += brightnessChangeStep;
        } else { // 차이가 step보다 작으면 목표값으로 바로 설정 (오버슈팅 방지)
          currentBrightnessValue = targetBrightnessValue;
        }
      } else { // currentBrightnessValue > targetBrightnessValue
        // 목표값까지의 차이가 step보다 크거나 같으면 step만큼 감소
        if (currentBrightnessValue - targetBrightnessValue >= brightnessChangeStep) {
          currentBrightnessValue -= brightnessChangeStep;
        } else { // 차이가 step보다 작으면 목표값으로 바로 설정 (언더슈팅 방지)
          currentBrightnessValue = targetBrightnessValue;
        }
      }
      Serial.print("Current Brightness (Smooth): "); // 디버깅용: 점진적 변경 값 출력
      Serial.println(currentBrightnessValue);
    }
    lastBrightnessUpdateTime = currentTime;
  }

  // 3. BLE 클라이언트(안드로이드 앱)가 연결되어 있는지 확인
  if (deviceConnected) {
    // 마지막 전송 후 일정 시간이 지났고, 현재 밝기 값이 마지막으로 전송한 값과 다른 경우
    if ((currentTime - lastSendTime >= sendInterval) && (currentBrightnessValue != lastSentBrightnessValue)) {
      // 3.1 BLE 특성 값 설정
      pCharacteristic->setValue(&currentBrightnessValue, 1); // 1 바이트 데이터 설정

      // 3.2 BLE 알림 전송
      pCharacteristic->notify();

      Serial.print("Brightness Sent via BLE: ");
      Serial.println(currentBrightnessValue);

      lastSentBrightnessValue = currentBrightnessValue; // 마지막 전송 값 업데이트
      lastSendTime = currentTime; // 마지막 전송 시간 업데이트
    }
  }

  // 짧은 딜레이 추가 (CPU 사용량 감소 및 다른 작업 처리 시간 확보)
  // 점진적 밝기 변경 로직이 brightnessUpdateInterval에 따라 실행되므로,
  // 이 delay는 해당 간격보다 짧거나 비슷하게 유지하는 것이 좋습니다.
  // 너무 길면 밝기 변경이 끊겨 보일 수 있습니다.
  delay(10);
}
