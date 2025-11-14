# The following five lines of boilerplate have to be in your project's
# CMakeLists in this exact order for cmake to work correctly
cmake_minimum_required(VERSION 3.16)

include($ENV{IDF_PATH}/tools/cmake/project.cmake)
project(BridgeOne)

# BridgeOne: Force USB Serial JTAG disabled for USB_OTG (eFuse USB_PHY_SEL=1)
# This must run after confgen has created sdkconfig
message(STATUS "BridgeOne: Patching sdkconfig to disable USB Serial JTAG...")
execute_process(
    COMMAND python "${CMAKE_CURRENT_SOURCE_DIR}/patch_sdkconfig.py" "${CMAKE_CURRENT_SOURCE_DIR}/sdkconfig"
    RESULT_VARIABLE PATCH_RESULT
    OUTPUT_VARIABLE PATCH_OUTPUT
)

if(PATCH_RESULT EQUAL 0)
    message(STATUS "[OK] sdkconfig patched successfully")
else()
    message(WARNING "[WARN] sdkconfig patch returned non-zero exit code: ${PATCH_RESULT}")
endif()

# Also add CMake-based patching as backup (post-build)
add_custom_command(TARGET app POST_BUILD
    COMMAND ${CMAKE_COMMAND} -P "${CMAKE_CURRENT_SOURCE_DIR}/fix_usb_serial_jtag.cmake"
    COMMENT "Enforcing USB Serial JTAG disabled in sdkconfig (post-build verify)..."
)
