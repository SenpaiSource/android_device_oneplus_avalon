#
# SPDX-FileCopyrightText: 2025 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit_only.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit from avalon device
$(call inherit-product, device/oneplus/avalon/device.mk)

# Inherit some common Lineage stuff.
$(call inherit-product, vendor/lineage/config/common_full_phone.mk)

PRODUCT_NAME := lineage_avalon
PRODUCT_DEVICE := avalon
PRODUCT_MANUFACTURER := OnePlus
PRODUCT_BRAND := OnePlus
PRODUCT_MODEL := CPH2661

PRODUCT_GMS_CLIENTID_BASE := android-oneplus

# Luna
WITH_GMS := true
WITH_BCR := true

TARGET_CUSTOM_UDFPS := true
TARGET_ENABLE_BLUR := true
TARGET_BOOT_ANIMATION_RES := 1080
TARGET_SUPPORTED_REFRESH_RATES := 60,90,120
TARGET_OPTIMIZED_DEXOPT := true
TARGET_DISABLE_MATLOG := true
HBM_SUPPORTED := true
HBM_NODE := /sys/class/backlight/panel0-backlight/hbm_mode
BYPASS_CHARGE_SUPPORTED := true
USE_REALITY_ENGINE := true

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildDesc="qssi_64-user 16 BP2A.250605.015 1782873585026 release-keys" \
    BuildFingerprint=OnePlus/CPH2661IN/OP5E93L1:16/UKQ1.231108.001/U.R4T2.3a24ca8-1593c70-163c453:user/release-keys \
    DeviceName=OP5E93L1 \
    DeviceProduct=CPH2661 \
    SystemDevice=OP5E93L1 \
    SystemName=CPH2661
