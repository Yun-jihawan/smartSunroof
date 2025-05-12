#include <Arduino.h>
#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h> // ArduinoJson v6 이상 필요

// --- WiFi 설정 ---
const char* ssid = "meow"; // 실제 WiFi SSID로 변경
const char* password = "zxcasdqwe123"; // 실제 WiFi 비밀번호로 변경

// --- MQTT 설정 ---
const char* mqtt_server = "test.mosquitto.org"; // MQTT 브로커 주소
const int mqtt_port = 1883;
const char* vehicleId = "test_vehicle_123"; // 차량 ID (예: "testVehicle123") - 실제 ID로 변경
char mqtt_client_id[50];

// MQTT 토픽 정의
char topic_status[100];
char topic_environment[100];

// MQTT 클라이언트
WiFiClient espClient;
PubSubClient client(espClient);
unsigned long lastMsg = 0;
unsigned long lastReconnectAttempt = 0;

// 시스템 상태 구조체 정의 (기존 코드와 동일)
typedef struct {
  uint8_t mode;           // 0번 비트
  uint8_t roof;           // 1-2번 비트 (수정된 주석)
  uint8_t airconditioner; // 3-4번 비트 (수정된 주석)
} system_state_t;

// DHT11 데이터 구조체 정의 (기존 코드와 동일)
typedef struct {
  uint8_t temp; // 온도 데이터
  uint8_t rh;   // 습도 데이터
} dht11_data_t;

// 공기 품질 레벨 구조체 정의 (기존 코드와 동일)
typedef struct {
  uint8_t internal_air_quality_state; // 0-1번 비트
  uint8_t external_air_quality_state; // 2-3번 비트
  uint8_t dust_state;                 // 4-5번 비트
} air_dust_level_t;

// 전체 데이터를 담는 구조체 (기존 코드와 동일)
typedef struct {
  system_state_t state;
  dht11_data_t dht_in;
  dht11_data_t dht_out;
  air_dust_level_t air_dust_level;
} sunroof_data_t;

// 전역 변수 선언 (기존 코드와 동일)
sunroof_data_t sunroof_data;
const int BUFFER_SIZE = 7;
uint8_t rx_buffer[BUFFER_SIZE];
int rx_index = 0;
bool packet_complete = false;

// --- Helper 함수: 숫자 상태를 문자열로 변환 ---
// state.h 및 MQTT 인터페이스 문서 참고

const char* getRoofStatusString(uint8_t roofState) {
  switch (roofState) {
    case 0: return "CLOSED"; // SUNROOF_CLOSED
    case 1: return "TILT";   // SUNROOF_TILT
    case 2: return "OPEN";   // SUNROOF_OPEN
    case 3: return "STOP";   // SUNROOF_STOP
    default: return "unknown";
  }
}

const char* getAirConditionerStatusString(uint8_t acState) {
  switch (acState) {
    case 0: return "OFF";  // AIR_CONDITIONER_OFF
    case 1: return "O/A";  // AIR_CONDITIONER_OUT
    case 2: return "I/A";  // AIR_CONDITIONER_IN
    case 3: return "MAX"; // AIR_CONDITIONER_MAX
    default: return "unknown";
  }
}

const char* getControlModeString(uint8_t modeState) {
  switch (modeState) {
    case 0: return "manual"; // MODE_MANUAL
    case 1: return "auto";   // MODE_SMART (auto에 해당)
    default: return "auto"; // 기본값
  }
}

const char* getAirQualityString(uint8_t aqState) {
  switch (aqState) {
    case 0: return "좋음";     // AIR_QUALITY_GOOD
    case 1: return "보통";   // AIR_QUALITY_MODERATE
    case 2: return "나쁨";     // AIR_QUALITY_BAD
    case 3: return "매우 나쁨"; // AIR_QUALITY_VERY_BAD
    default: return "알 수 없음";
  }
}

const char* getDustStatusString(uint8_t dustState) {
  switch (dustState) {
    case 0: return "좋음";     // DUST_GOOD (또는 "낮음")
    case 1: return "보통";   // DUST_MODERATE
    case 2: return "나쁨";     // DUST_BAD
    case 3: return "매우 나쁨"; // DUST_VERY_BAD
    default: return "알 수 없음";
  }
}

// --- WiFi 연결 함수 ---
void setup_wifi() {
  delay(10);
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(ssid);

  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  randomSeed(micros());

  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
}

// --- MQTT 콜백 함수 (수신 메시지 처리용, 현재는 사용 안함) ---
void mqtt_callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Message arrived [");
  Serial.print(topic);
  Serial.print("] ");
  for (unsigned int i = 0; i < length; i++) {
    Serial.print((char)payload[i]);
  }
  Serial.println();
}

// --- MQTT 재연결 함수 ---
void mqtt_reconnect() {
  // Loop until we're reconnected
  while (!client.connected()) {
    Serial.print("Attempting MQTT connection...");
    // 클라이언트 ID 생성 (고유하게)
    snprintf(mqtt_client_id, sizeof(mqtt_client_id), "esp32-sunroof-%s-%ld", vehicleId, random(0xffff));
    
    // Connect now
    if (client.connect(mqtt_client_id)) {
      Serial.println("connected");
      // 여기에 구독(subscribe) 로직을 추가할 수 있습니다 (필요한 경우)
      // client.subscribe("some_topic_to_listen_to");
    } else {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      Serial.println(" try again in 5 seconds");
      // Wait 5 seconds before retrying
      delay(5000);
    }
  }
}

void setup() {
  Serial.begin(115200);
  Serial2.begin(9600, SERIAL_8N1, 16, 17); // RX2: 16, TX2: 17

  setup_wifi();
  
  // MQTT 토픽 문자열 생성
  snprintf(topic_status, sizeof(topic_status), "bluelink/vehicle/%s/status", vehicleId);
  snprintf(topic_environment, sizeof(topic_environment), "bluelink/vehicle/%s/environment", vehicleId);

  Serial.print("Status Topic: "); Serial.println(topic_status);
  Serial.print("Environment Topic: "); Serial.println(topic_environment);

  client.setBufferSize(512);
  client.setServer(mqtt_server, mqtt_port);
  client.setCallback(mqtt_callback);

  Serial.println("ESP32 Sunroof Receiver with MQTT initialized");
  lastReconnectAttempt = 0; // 재연결 시도 타이머 초기화
}

// --- 수신된 데이터 파싱 함수 (기존 코드와 동일, 주석 약간 수정) ---
void parse_received_data() {
  // 시스템 상태 정보 파싱
  sunroof_data.state.mode = rx_buffer[1] & 0x01;                  // 0번 비트
  sunroof_data.state.roof = (rx_buffer[1] >> 1) & 0x03;          // 1-2번 비트 (00, 01, 10, 11)
  sunroof_data.state.airconditioner = (rx_buffer[1] >> 3) & 0x03; // 3-4번 비트

  // DHT11 온습도 데이터
  sunroof_data.dht_in.temp = rx_buffer[2];
  sunroof_data.dht_in.rh = rx_buffer[3];
  sunroof_data.dht_out.temp = rx_buffer[4];
  sunroof_data.dht_out.rh = rx_buffer[5];

  // 공기 품질 상태 파싱
  sunroof_data.air_dust_level.internal_air_quality_state = rx_buffer[6] & 0x03;       // 0-1번 비트
  sunroof_data.air_dust_level.external_air_quality_state = (rx_buffer[6] >> 2) & 0x03; // 2-3번 비트
  sunroof_data.air_dust_level.dust_state = (rx_buffer[6] >> 4) & 0x03;                 // 4-5번 비트
}

// --- 파싱된 데이터 출력 함수 (기존 코드와 동일, 디버깅용) ---
void display_data() {
  Serial.println("======= 받은 데이터 =======");
  Serial.print("모드: "); Serial.println(getControlModeString(sunroof_data.state.mode));
  Serial.print("선루프 상태: "); Serial.println(getRoofStatusString(sunroof_data.state.roof));
  Serial.print("에어컨 상태: "); Serial.println(getAirConditionerStatusString(sunroof_data.state.airconditioner));
  Serial.print("내부 온도: "); Serial.print(sunroof_data.dht_in.temp); Serial.println("°C");
  Serial.print("내부 습도: "); Serial.print(sunroof_data.dht_in.rh); Serial.println("%");
  Serial.print("외부 온도: "); Serial.print(sunroof_data.dht_out.temp); Serial.println("°C");
  Serial.print("외부 습도: "); Serial.print(sunroof_data.dht_out.rh); Serial.println("%");
  Serial.print("내부 공기 품질: "); Serial.println(getAirQualityString(sunroof_data.air_dust_level.internal_air_quality_state));
  Serial.print("외부 공기 품질: "); Serial.println(getAirQualityString(sunroof_data.air_dust_level.external_air_quality_state));
  Serial.print("먼지 상태: "); Serial.println(getDustStatusString(sunroof_data.air_dust_level.dust_state));
  Serial.println("=========================");
}

// --- MQTT 데이터 발행 함수 ---
void publish_mqtt_data() {
  if (!client.connected()) {
    Serial.println("MQTT client not connected. Cannot publish.");
    return;
  }

  // JSON 문서 크기 (필요에 따라 조절)
  const int status_doc_capacity = 256; // 상태 JSON 문서 크기
  const int env_doc_capacity = 384;    // 환경 JSON 문서 크기

  // Status JSON
  StaticJsonDocument<status_doc_capacity> status_doc; // MQTT 인터페이스 문서의 status 필드 기반
  status_doc["overall_control_mode"] = getControlModeString(sunroof_data.state.mode);
  status_doc["sunroof_status"] = getRoofStatusString(sunroof_data.state.roof);
  status_doc["ac_status"] = getAirConditionerStatusString(sunroof_data.state.airconditioner);
  status_doc["is_driving"] = false; // UART 데이터에 없음, 기본값 설정
  status_doc["current_speed_kmh"] = 0;    // UART 데이터에 없음, 기본값 설정
  // status_doc["timestamp"] = millis(); // 필요시 타임스탬프 추가

  char status_payload[status_doc_capacity]; // 페이로드 버퍼 크기도 문서 크기에 맞춤
  serializeJson(status_doc, status_payload);
  if (client.publish(topic_status, status_payload)) {
    Serial.print("Published to status topic: "); Serial.println(status_payload);
  } else {
    Serial.println("Failed to publish to status topic");
  }

  // Environment JSON
  StaticJsonDocument<env_doc_capacity> env_doc; // MQTT 인터페이스 문서의 environment 필드 기반
  env_doc["indoor_temp_celsius"] = sunroof_data.dht_in.temp;
  env_doc["indoor_humidity_percent"] = sunroof_data.dht_in.rh;
  env_doc["outdoor_temp_celsius"] = sunroof_data.dht_out.temp;
  env_doc["outdoor_humidity_percent"] = sunroof_data.dht_out.rh;
  env_doc["indoor_air_quality_text"] = getAirQualityString(sunroof_data.air_dust_level.internal_air_quality_state);
  // env_doc["indoor_aqi"] = ... ; // UART 데이터에 없음
  env_doc["outdoor_air_quality_text"] = getAirQualityString(sunroof_data.air_dust_level.external_air_quality_state);
  // env_doc["outdoor_aqi"] = ... ; // UART 데이터에 없음
  // env_doc["fine_dust_pm25_ug_m3"] = ... ; // UART 데이터에 없음, dust_state만 사용
  env_doc["fine_dust_status_text"] = getDustStatusString(sunroof_data.air_dust_level.dust_state);
  // env_doc["timestamp"] = millis(); // 필요시 타임스탬프 추가

  char env_payload[env_doc_capacity]; // 페이로드 버퍼 크기도 문서 크기에 맞춤
  serializeJson(env_doc, env_payload);
  if (client.publish(topic_environment, env_payload)) {
    Serial.print("Published to environment topic: "); Serial.println(env_payload);
  } else {
    Serial.println("Failed to publish to environment topic");
  }
}


void loop() {
  // --- MQTT 연결 관리 ---
  if (!client.connected()) {
    long now = millis();
    // 5초마다 재연결 시도 (너무 자주 시도하지 않도록)
    if (now - lastReconnectAttempt > 5000) {
      lastReconnectAttempt = now;
      mqtt_reconnect();
    }
  } else {
    client.loop(); // MQTT 메시지 수신 및 연결 유지
  }

  // --- 시리얼 데이터 수신 (기존 코드와 동일) ---
  if (Serial2.available()) {
    uint8_t rx_byte = Serial2.read();
    
    if (rx_index == 0 && rx_byte == 0xAA) { // 패킷 시작 식별자
      rx_buffer[rx_index++] = rx_byte;
    }
    else if (rx_index > 0) { // 데이터 저장
      rx_buffer[rx_index++] = rx_byte;
      
      if (rx_index >= BUFFER_SIZE) { // 패킷 완성
        packet_complete = true;
        rx_index = 0;
      }
    }
  }
  
  // --- 패킷 완성 시 처리 ---
  if (packet_complete) {
    parse_received_data();
    display_data(); // 디버깅용 데이터 출력 (시리얼 모니터)
    
    // MQTT로 데이터 발행 (연결된 경우에만)
    if (client.connected()) {
      publish_mqtt_data();
    }
    
    packet_complete = false;
  }
}