#include <Arduino.h>

// 시스템 상태 구조체 정의
typedef struct {
  uint8_t mode;            // 0번 비트
  uint8_t roof;            // 1번 비트
  uint8_t airconditioner;  // 3번 비트
} system_state_t;

// DHT11 데이터 구조체 정의
typedef struct {
  uint8_t temp;  // 온도 데이터
  uint8_t rh;    // 습도 데이터
} dht11_data_t;

// 공기 품질 레벨 구조체 정의
typedef struct {
  uint8_t internal_air_quality_state;   // 0-1번 비트
  uint8_t external_air_quality_state;   // 2-3번 비트
  uint8_t dust_state;                   // 4-5번 비트
} air_dust_level_t;

// 전체 데이터를 담는 구조체
typedef struct {
  system_state_t state;
  dht11_data_t dht_in;
  dht11_data_t dht_out;
  air_dust_level_t air_dust_level;
} sunroof_data_t;

// 전역 변수 선언
sunroof_data_t sunroof_data;
const int BUFFER_SIZE = 7;
uint8_t rx_buffer[BUFFER_SIZE];
int rx_index = 0;
bool packet_complete = false;

void setup() {
  // 디버깅용 시리얼 초기화
  Serial.begin(115200);
  
  // NUCLEO-L073RZ와 통신할 시리얼 포트 초기화 (ESP32의 UART2 사용)
  // RX2: GPIO16, TX2: GPIO17 핀 사용
  Serial2.begin(9600, SERIAL_8N1, 16, 17);
  
  Serial.println("ESP32 Sunroof Receiver initialized");
}

void loop() {
  // 시리얼 데이터 수신 확인
  if (Serial2.available()) {
    uint8_t rx_byte = Serial2.read();
    
    // 패킷 시작 식별자(0xBB) 확인
    if (rx_index == 0 && rx_byte == 0xBB) {
      rx_buffer[rx_index++] = rx_byte;
    }
    // 이미 시작 식별자를 확인했으면 나머지 데이터 저장
    else if (rx_index > 0) {
      rx_buffer[rx_index++] = rx_byte;
      
      // 패킷 완성 확인
      if (rx_index >= BUFFER_SIZE) {
        packet_complete = true;
        rx_index = 0;
      }
    }
  }
  
  // 패킷 완성되면 데이터 파싱 및 처리
  if (packet_complete) {
    parse_received_data();
    display_data();
    packet_complete = false;
  }
}

// 수신된 데이터 파싱 함수
void parse_received_data() {
  // 시스템 상태 정보 파싱
  sunroof_data.state.mode = rx_buffer[1] & 0x01;                 // 0번 비트
  sunroof_data.state.roof = (rx_buffer[1] >> 1) & 0x03;          // 1번 비트
  sunroof_data.state.airconditioner = (rx_buffer[1] >> 3) & 0x03; // 3번 비트
  
  // DHT11 온습도 데이터
  sunroof_data.dht_in.temp = rx_buffer[2];
  sunroof_data.dht_in.rh = rx_buffer[3];
  sunroof_data.dht_out.temp = rx_buffer[4];
  sunroof_data.dht_out.rh = rx_buffer[5];
  
  // 공기 품질 상태 파싱
  sunroof_data.air_dust_level.internal_air_quality_state = rx_buffer[6] & 0x03;         // 0-1번 비트
  sunroof_data.air_dust_level.external_air_quality_state = (rx_buffer[6] >> 2) & 0x03;  // 2-3번 비트
  sunroof_data.air_dust_level.dust_state = (rx_buffer[6] >> 4) & 0x03;                  // 4-5번 비트
}

// 파싱된 데이터 출력 함수
void display_data() {
  // 디버깅용 시리얼 모니터에 데이터 출력
  Serial.println("======= 받은 데이터 =======");
  
  // 시스템 상태 정보
  Serial.print("모드: ");
  Serial.println(sunroof_data.state.mode);
  Serial.print("지붕 상태: ");
  Serial.println(sunroof_data.state.roof);
  Serial.print("에어컨 상태: ");
  Serial.println(sunroof_data.state.airconditioner);
  
  // 온습도 데이터
  Serial.print("내부 온도: ");
  Serial.print(sunroof_data.dht_in.temp);
  Serial.println("°C");
  Serial.print("내부 습도: ");
  Serial.print(sunroof_data.dht_in.rh);
  Serial.println("%");
  Serial.print("외부 온도: ");
  Serial.print(sunroof_data.dht_out.temp);
  Serial.println("°C");
  Serial.print("외부 습도: ");
  Serial.print(sunroof_data.dht_out.rh);
  Serial.println("%");
  
  // 공기 품질 상태 정보
  Serial.print("내부 공기 품질: ");
  Serial.println(sunroof_data.air_dust_level.internal_air_quality_state);
  Serial.print("외부 공기 품질: ");
  Serial.println(sunroof_data.air_dust_level.external_air_quality_state);
  Serial.print("먼지 상태: ");
  Serial.println(sunroof_data.air_dust_level.dust_state);
  Serial.println("=========================");
}
