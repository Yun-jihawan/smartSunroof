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
uint8_t currentBrightnessValue = 0; // NUCLEO로부터 수신한 현재 밝기 값
uint8_t lastSentBrightnessValue = 0;// 마지막으로 BLE 전송한 밝기 값
unsigned long lastSendTime = 0;     // 마지막 전송 시간
const long sendInterval = 500;      // BLE 전송 간격 (밀리초) - 필요시 조정

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
  Serial.println("Starting ESP32 BLE Sender (Receiving from Nucleo)");

  // NUCLEO 보드와 통신할 시리얼 포트(Serial2) 초기화
  // NucleoSerial.begin(NUCLEO_SERIAL_BAUD_RATE); // 기본 핀 (RX=16, TX=17) 사용 시
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
  // 1. NUCLEO 보드로부터 시리얼 데이터 수신 확인
  if (NucleoSerial.available() > 0) {
    // NUCLEO가 1바이트 밝기 값(0-255)을 보낸다고 가정
    uint8_t receivedValue = NucleoSerial.read();

    // TODO: NUCLEO가 다른 형식(예: 문자열)으로 데이터를 보낸다면 여기서 파싱 로직 수정 필요
    // 예: 여러 바이트를 읽어 숫자로 변환하거나, 특정 구분자까지 읽기 등

    currentBrightnessValue = receivedValue; // 수신한 값으로 현재 밝기 값 업데이트

    // 디버깅: 수신된 값 출력
    Serial.print("Received from Nucleo: ");
    Serial.println(currentBrightnessValue);
  }

  // 2. BLE 클라이언트(안드로이드 앱)가 연결되어 있는지 확인
  if (deviceConnected) {
    unsigned long currentTime = millis();
    // 마지막 전송 후 일정 시간이 지났고, 값이 변경되었는지 확인
    if ((currentTime - lastSendTime >= sendInterval) && (currentBrightnessValue != lastSentBrightnessValue)) {
      // 3. BLE 특성 값 설정
      pCharacteristic->setValue(&currentBrightnessValue, 1); // 1 바이트 데이터 설정

      // 4. BLE 알림 전송
      pCharacteristic->notify();

      Serial.print("Brightness Sent via BLE: ");
      Serial.println(currentBrightnessValue);

      lastSentBrightnessValue = currentBrightnessValue; // 마지막 전송 값 업데이트
      lastSendTime = currentTime; // 마지막 전송 시간 업데이트
    }
  }

  // 짧은 딜레이 추가 (CPU 사용량 감소 및 다른 작업 처리 시간 확보)
  delay(10);
}
