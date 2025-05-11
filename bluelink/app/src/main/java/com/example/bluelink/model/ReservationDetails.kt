package com.example.bluelink.model

// 서비스 예약 정보를 위한 데이터 클래스 (SWR-MOB-19)
data class ReservationDetails(
    val date: String = "",                // 예약 날짜 (YYYY-MM-DD 형식으로 가정)
    val time: String = "",                // 예약 시간 (HH:MM 형식으로 가정)
    val serviceCenter: String = "",       // 선택된 서비스 센터
    val requestDetails: String = ""       // 추가 요청 사항
)