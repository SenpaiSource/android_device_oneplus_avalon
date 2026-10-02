#
# Copyright (C) 2021-2026 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# AAPT
PRODUCT_AAPT_CONFIG := normal
PRODUCT_AAPT_PREF_CONFIG := xxxhdpi

# Alert slider
PRODUCT_PACKAGES += \
    KeyHandler \
    tri-state-key-calibrate

# Audio
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/audio/audio_policy_volumes.xml:$(TARGET_COPY_OUT_VENDOR)/etc/audio_policy_volumes.xml \
    $(LOCAL_PATH)/configs/audio/default_volume_tables.xml:$(TARGET_COPY_OUT_VENDOR)/etc/default_volume_tables.xml

# Boot animation
TARGET_SCREEN_HEIGHT := 2772
TARGET_SCREEN_WIDTH := 1240

# Display
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/display/displayconfig.xml:$(TARGET_COPY_OUT_VENDOR)/etc/displayconfig/display_id_4630947214369728643.xml

# LiveDisplay
$(call soong_config_set_bool,OPLUS_LINEAGE_LIVEDISPLAY_HAL,ENABLE_SE,false)

# Overlays
DEVICE_PACKAGE_OVERLAYS += \
    $(LOCAL_PATH)/overlay-lightning

PRODUCT_PACKAGES += \
    KeyHandlerResTarget \
    OPlusFrameworksResTarget \
    OPlusSettingsProviderResTarget \
    OPlusSettingsResTarget \
    OPlusSystemUIResTarget \
    OPlusWifiResTarget

# PowerOffAlarm
PRODUCT_PACKAGES += \
    PowerOffAlarm \
    init.power_off_alarm.rc

# Regional properties
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/recovery/root/vendor/odm/etc/24211/build.EU.prop:$(TARGET_COPY_OUT_ODM)/etc/24211/build.EU.prop \
    $(LOCAL_PATH)/recovery/root/vendor/odm/etc/24211/build.IN.prop:$(TARGET_COPY_OUT_ODM)/etc/24211/build.IN.prop \
    $(LOCAL_PATH)/recovery/root/vendor/odm/etc/24211/build.ROW.prop:$(TARGET_COPY_OUT_ODM)/etc/24211/build.ROW.prop \
    $(LOCAL_PATH)/recovery/root/vendor/odm/etc/24211/build.default.prop:$(TARGET_COPY_OUT_ODM)/etc/24211/build.default.prop

# Soong namespaces
PRODUCT_SOONG_NAMESPACES += \
    $(LOCAL_PATH)

# Touch features
$(call soong_config_set_bool,OPLUS_LINEAGE_TOUCH_HAL,ENABLE_GM,true)

# Vibrator
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/init/vendor.qti.hardware.vibrator.service.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/vendor.qti.hardware.vibrator.service.rc

# WiFi firmware symlinks
PRODUCT_PACKAGES += \
    firmware_wlan_mac.bin_symlink \
    firmware_WCNSS_qcom_cfg.ini_symlink

# LightningOS
LIGHTNINGOS_MAINTAINER := Sathiya_Senpai
WITH_GMS := true

PRODUCT_SYSTEM_PROPERTIES += \
    ro.lightning.display="1240 x 2772, 120 hz" \
    ro.lightning.battery=5500mah \
    ro.lightning.soc="Qualcomm Snapdragon 7+ Gen 3" \
    ro.lightning.camera="50MP + 8MP" \
    ro.lightning.front=16MP \
    ro.lightning.platform=SM7675 \
    ro.lightning.screen="6.74″ AMOLED" \
    ro.lightning.device.name="OnePlus Nord 4"

# Silence spammy vendor logs
SPAMMY_LOG_TAGS := \
    Diag_Lib \
    AGM \
    AHAL \
    CamX

ifneq ($(TARGET_BUILD_VARIANT),eng)
PRODUCT_VENDOR_PROPERTIES += \
    $(foreach tag,$(SPAMMY_LOG_TAGS),log.tag.$(tag)=S)
endif

# Inherit from the common OEM chipset makefile.
$(call inherit-product, device/oneplus/sm8650-common/common.mk)

# Inherit from the proprietary files makefile.
$(call inherit-product, vendor/oneplus/avalon/avalon-vendor.mk)
