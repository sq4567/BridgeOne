#!/usr/bin/env python3
"""
BridgeOne sdkconfig Patcher
Patches sdkconfig to disable USB Serial JTAG settings
Required because eFuse USB_PHY_SEL=1 dedicates PHY to USB_OTG
and ESP-IDF Kconfig defaults to enabling USB Serial JTAG
"""

import re
import sys
import os

def patch_sdkconfig(sdkconfig_path="sdkconfig"):
    """Patch sdkconfig to force disable USB Serial JTAG settings"""

    if not os.path.exists(sdkconfig_path):
        print(f"[ERROR] {sdkconfig_path} not found!")
        return False

    print(f"[READ] Reading {sdkconfig_path}...")
    with open(sdkconfig_path, 'r', encoding='utf-8') as f:
        content = f.read()

    original_content = content
    modifications = []

    # Settings that must be disabled
    settings_to_fix = {
        'CONFIG_USJ_ENABLE_USB_SERIAL_JTAG': 'CONFIG_USJ_ENABLE_USB_SERIAL_JTAG=n',
        'CONFIG_ESP_CONSOLE_SECONDARY_USB_SERIAL_JTAG': 'CONFIG_ESP_CONSOLE_SECONDARY_USB_SERIAL_JTAG=n',
        'CONFIG_ESP_CONSOLE_USB_SERIAL_JTAG_ENABLED': 'CONFIG_ESP_CONSOLE_USB_SERIAL_JTAG_ENABLED=n',
    }

    for setting, replacement in settings_to_fix.items():
        # Replace =y with =n
        if f'{setting}=y' in content:
            content = content.replace(f'{setting}=y', replacement)
            modifications.append(f"  [OK] {setting}=y -> {replacement}")

        # Replace "# XXX is not set" with =n
        elif f'# {setting} is not set' in content:
            content = content.replace(f'# {setting} is not set', replacement)
            modifications.append(f"  [OK] # {setting} is not set -> {replacement}")

        # Check if already correct
        elif replacement in content:
            modifications.append(f"  [CHECK] {setting} already disabled")

        # Add if missing (last resort)
        else:
            # Find the section and add it
            content += f'\n{replacement}\n'
            modifications.append(f"  [NEW] Added {replacement} (was missing)")

    # Only write if changed
    if content != original_content:
        print(f"\n[PATCH] Patching settings:")
        for mod in modifications:
            print(mod)

        print(f"\n[WRITE] Writing changes to {sdkconfig_path}...")
        with open(sdkconfig_path, 'w', encoding='utf-8') as f:
            f.write(content)

        # Verify the patch
        print("\n[CHECK] Verification:")
        with open(sdkconfig_path, 'r', encoding='utf-8') as f:
            for line in f:
                for setting in settings_to_fix.keys():
                    if setting in line:
                        print(f"  {line.rstrip()}")

        print(f"\n[OK] {sdkconfig_path} patched successfully!")
        return True
    else:
        print("[CHECK] sdkconfig already correct - no changes needed")
        return True

if __name__ == '__main__':
    if len(sys.argv) > 1:
        sdkconfig_path = sys.argv[1]
    else:
        sdkconfig_path = 'sdkconfig'

    success = patch_sdkconfig(sdkconfig_path)
    sys.exit(0 if success else 1)
