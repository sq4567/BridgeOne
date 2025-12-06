idf_component_register(
    SRCS
        "BridgeOne.c"
        "usb_descriptors.c"
        "hid_handler.c"
        "hid_test.c"
        "uart_handler.c"
        "usb_cdc_log.c"
    INCLUDE_DIRS "."
    REQUIRES
        tinyusb
        freertos
        driver
        esp_timer
)

# TinyUSB 설정: tusb_config.h 파일 포함 경로 및 컴파일 정의
target_include_directories(${COMPONENT_LIB} PUBLIC "${CMAKE_CURRENT_SOURCE_DIR}")
target_compile_definitions(${COMPONENT_LIB} PUBLIC CFG_TUSB_MCU=OPT_MCU_ESP32S3)
