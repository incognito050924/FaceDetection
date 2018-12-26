# 필요사항
1. OpenCV 라이브러리 추가 (완료)
2. OpenCV 모듈 추가 (완료)
3. Cmake 설정 (CmakeLists.txt 수정)
4. [Android ndk version 17 이하로 변경(현재 최신 Ver. 18)](https://webnautes.tistory.com/1273?category=704164)

## Cmake 설정(OpenCV sdk 위치 지정)
* [OpenCV 3.4.4 Android Library 다운로드](https://github.com/opencv/opencv/releases/download/3.4.4/opencv-3.4.4-android-sdk.zip)
* 다운로드한 zip파일 압축 해제. "OpenCV-android-sdk" 디렉터리 존재하는지 확인
* 기본적으로 windows os 기준으로 설정되어있으며 C:/Users/%USERNAME%/Downloads 에서 OpenCV-android-sdk 디렉터리를 찾음.
* 위의 다운로드 폴더에서 찾지 못한 경우 OS 환경변수 OPENCV_SDK_DIR 참조
* app/CmakeLists.txt 14번 라인을 수정하여 직접 지정 가능 (단, Cmake 문법 상 파일 구분자는 윈도에서도 "\"가 아닌 "/" 사용함)

## Android ndk 지정(ndk-build tool 17 이후 버전 사용 시 에러 발생함. 17 버전까지 사용 가능)
* [android-ndk-r17c-windows-x86_64](https://dl.google.com/android/repository/android-ndk-r17c-windows-x86_64.zip)
* [Windows 64bit 이외의 ndk 다운로드](https://developer.android.com/ndk/downloads/older_releases)
* Android studio 에서 File -> Project structure (Ctrl + Alt + Shift + S) 에서 SDK Location 탭 -> Android NDK location에 다운로드하여 압축 해제한 디렉터리 지정
