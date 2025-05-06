# Human-Machine Interface(RPI4)

---

## 개요

사용자의 조작을 통해 CGW에 조작 정보 전송 및 필요한 결과를 수신하는 인터페이스

## 기본 정보

### 정보

> CGW와 여러 인터페이스를 통해 정보 송수신
> 

### 시작 가이드 (AUTOSTART 설정)

1. **디렉토리 생성**
    
    ```bash
    mkdir -p ~/.config/autostart
    ```
    
2. **.desktop 파일 작성**
    
    ```bash
    vim ~/.config/autostart/display.desktop
    ```
    
3. **내용 입력**
    
    ```
    [Desktop Entry]
    Type=Application
    Name=Display
    Exec=python3 /home/user/display/main.py
    Path=/home/user/display
    X-GNOME-Autostart-enabled=true
    ```
    
4. **실행 권한 부여**
   ```bash
   chmod +x ~/.config/autostart/display.desktop
   ``` 
