# CMake script to fix USB Serial JTAG settings in sdkconfig
# This script runs after build to ensure USB Serial JTAG is disabled
# Required because eFuse USB_PHY_SEL=1 dedicates internal PHY to USB_OTG

set(SDKCONFIG_FILE "${CMAKE_CURRENT_SOURCE_DIR}/sdkconfig")

if(NOT EXISTS "${SDKCONFIG_FILE}")
    message(WARNING "sdkconfig file not found: ${SDKCONFIG_FILE}")
    return()
endif()

# Read sdkconfig file
file(READ "${SDKCONFIG_FILE}" SDKCONFIG_CONTENT)

# Define the three settings that must be disabled
set(SETTINGS_TO_FIX
    "CONFIG_USJ_ENABLE_USB_SERIAL_JTAG"
    "CONFIG_ESP_CONSOLE_USB_SERIAL_JTAG_ENABLED"
    "CONFIG_ESP_CONSOLE_SECONDARY_USB_SERIAL_JTAG"
)

set(MODIFIED FALSE)

foreach(SETTING ${SETTINGS_TO_FIX})
    # Check if this setting exists and is set to =y
    if(SDKCONFIG_CONTENT MATCHES "${SETTING}=y")
        # Replace =y with =n
        string(REGEX REPLACE "${SETTING}=y" "${SETTING}=n" SDKCONFIG_CONTENT "${SDKCONFIG_CONTENT}")
        message(STATUS "Fixed: ${SETTING}=y → ${SETTING}=n")
        set(MODIFIED TRUE)
    endif()

    # Also handle commented versions: # CONFIG_XXX is not set → CONFIG_XXX=n
    if(SDKCONFIG_CONTENT MATCHES "# ${SETTING} is not set")
        string(REGEX REPLACE "# ${SETTING} is not set" "${SETTING}=n" SDKCONFIG_CONTENT "${SDKCONFIG_CONTENT}")
        message(STATUS "Fixed: # ${SETTING} is not set → ${SETTING}=n")
        set(MODIFIED TRUE)
    endif()
endforeach()

# Write back if modified
if(MODIFIED)
    file(WRITE "${SDKCONFIG_FILE}" "${SDKCONFIG_CONTENT}")
    message(STATUS "✅ sdkconfig updated: USB Serial JTAG settings forced to disabled")
else()
    message(STATUS "✓ sdkconfig already correct: USB Serial JTAG is disabled")
endif()
