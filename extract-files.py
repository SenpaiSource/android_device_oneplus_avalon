#!/usr/bin/env -S PYTHONPATH=../../../tools/extract-utils:../../../hardware/oplus/extract-utils-ext python3
#
# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

from extract_utils.fixups_blob import (
    blob_fixup,
    blob_fixups_user_type,
)
from extract_utils.fixups_lib import (
    lib_fixups,
    lib_fixups_user_type,
)
from extract_utils.main import (
    ExtractUtils,
    ExtractUtilsModule,
)
from extract_utils_oplus import (
    add_generated_thermal_engine_config,
)

namespace_imports = [
    'hardware/oplus',
    'hardware/qcom-caf/sm8650',
    'vendor/oneplus/sm8650-common',
    'vendor/qcom/opensource/commonsys-intf/display',
]


def lib_fixup_vendor_suffix(lib: str, partition: str, *args, **kwargs):
    return f'{lib}_{partition}' if partition == 'vendor' else None

lib_fixups: lib_fixups_user_type = {
    **lib_fixups,
    (
        'com.qti.sensor.s5k3p9',
        'libhwconfigurationutil',
        'libPanelChaplin',
        'libpwirisfeature',
        'libpwirishalwrapper',
        'vendor.pixelworks.hardware.display@1.0',
        'vendor.pixelworks.hardware.display@1.1',
        'vendor.pixelworks.hardware.display@1.2',
        'vendor.pixelworks.hardware.feature@1.0',
        'vendor.pixelworks.hardware.feature@1.1',
    ): lib_fixup_vendor_suffix,
}

blob_fixups: blob_fixups_user_type = {
    'odm/etc/camera/CameraHWConfiguration.config': blob_fixup()
        # Expose AUX cameras
        .regex_replace('SystemCamera =  0;  0;  0;  0;  1;', 'SystemCamera =  0;  0;  0;  0;  0;'),
    'odm/lib64/libAlgoProcess.so': blob_fixup()
        .replace_needed('android.hardware.graphics.common-V3-ndk.so', 'android.hardware.graphics.common-V7-ndk.so')
        .replace_needed('android.hardware.graphics.common-V4-ndk.so', 'android.hardware.graphics.common-V7-ndk.so'),
    (
        'odm/lib64/libCOppLceTonemapAPI.so',
        'odm/lib64/libSuperRaw.so',
        'odm/lib64/libYTCommon.so',
        'odm/lib64/libyuv2.so',
    ): blob_fixup()
        .replace_needed('libstdc++.so', 'libstdc++_vendor.so'),
    'odm/lib64/soundfx/liboplus_spatializer.so': blob_fixup()
        .replace_needed('libstdc++.so', 'libstdc++_vendor.so')
        .sig_replace(
            '00 00 00 00 01 00 00 00 ff ff ff ff 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 00 00 00 00',
            '02 00 00 00 01 00 00 00 ff ff ff ff 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 00 00 00 00',
        )
        .sig_replace(
            '49 00 80 52 08 e1 46 39 69 02 00 b9 68 04 00 39',
            '29 00 80 52 08 e1 46 39 69 02 00 b9 68 04 00 39',
        )
        .sig_replace(
            '09 01 40 b9 26 05 00 11 ca 18 00 12 5f 01 00 71 24 19 49 7a 06 01 00 b9 6c 01 00 54 a1 ff ff f0 c2 ff ff 90 a3 ff ff f0 a4 02 40 f9 65 02 40 f9 21 f4 3a 91 42 dc 22 91 63 50 08 91 c0 00 80 52 f8 12 00 94',
            '68 02 40 f9 89 9e 81 b9 16 7d 09 9b 80 e6 40 f9 c0 00 00 b5 e0 03 16 aa 22 13 00 94 80 e6 00 f9 68 02 40 f9 88 e2 00 f9 e1 03 1f 2a e2 03 16 aa 20 13 00 94 1f 20 03 d5 1f 20 03 d5 1f 20 03 d5 1f 20 03 d5',
        )
        .sig_replace(
            'a1 ff ff d0 a2 ff ff d0 a3 ff ff f0 21 f4 3a 91 42 fc 1e 91 63 20 24 91 40 00 80 52 09 11 00 94',
            '7f 32 01 b9 7f 92 01 b9 28 00 80 52 68 22 05 39 1f 20 03 d5 1f 20 03 d5 1f 20 03 d5 1f 20 03 d5',
        )
        .sig_replace(
            'df 0e 00 71 e9 4e 40 b9 e8 07 9f 1a 68 fe 01 39',
            'df 0e 00 71 e9 4e 40 b9 28 00 80 52 68 fe 01 39',
        ),
    ('odm/lib64/libHIS.so', 'odm/lib64/libOGLManager.so', 'odm/lib64/libOPAlgoCamAiBeautyFaceRetouchCn.so', 'odm/lib64/libOPAlgoCamFaceBeautyCap.so'): blob_fixup()
        .clear_symbol_version('AHardwareBuffer_acquire')
        .clear_symbol_version('AHardwareBuffer_allocate')
        .clear_symbol_version('AHardwareBuffer_describe')
        .clear_symbol_version('AHardwareBuffer_lock')
        .clear_symbol_version('AHardwareBuffer_lockPlanes')
        .clear_symbol_version('AHardwareBuffer_release')
        .clear_symbol_version('AHardwareBuffer_unlock')
        .replace_needed('libui.so', 'libui-stock.so'),
    'odm/lib64/libarcsoft_high_dynamic_range_v4.so': blob_fixup()
        .clear_symbol_version('remote_handle_close')
        .clear_symbol_version('remote_handle_invoke')
        .clear_symbol_version('remote_handle_open')
        .clear_symbol_version('remote_register_buf_attr')
        .clear_symbol_version('remote_register_buf'),
    (
        'odm/lib64/libdisplaycolorfeature.so',
        'odm/lib64/libdisplayfossfeature_nature.so',
        'vendor/bin/hw/vendor.qti.hardware.display.composer-service',
        'vendor/lib64/libdpps.so',
        'vendor/lib64/libsnapdragoncolor-manager.so',
    ): blob_fixup()
        .replace_needed('libtinyxml2.so', 'libtinyxml2-v34.so'),
    ('vendor/bin/hw/vendor.qti.hardware.vibrator.service', 'vendor/lib64/vendor.qti.hardware.vibrator.impl.so'): blob_fixup()
        .replace_needed('android.hardware.vibrator-V2-ndk_platform.so', 'android.hardware.vibrator-V2-ndk.so'),
    'vendor/etc/libnfc-nci.conf': blob_fixup()
        .regex_replace('NFC_DEBUG_ENABLED=1', 'NFC_DEBUG_ENABLED=0'),
    'vendor/etc/libnfc-nxp.conf': blob_fixup()
        .regex_replace('(NXPLOG_.*_LOGLEVEL)=0x03', '\\1=0x02')
        .regex_replace('NFC_DEBUG_ENABLED=1', 'NFC_DEBUG_ENABLED=0'),
    'vendor/lib64/libcwb_qcom_aidl.so': blob_fixup()
        .add_needed('libui_shim.so'),
    (
        'odm/lib64/camera/components/com.oplus.node.mvgsat.so',
        'odm/lib64/camera/components/com.oplus.node.sstabphoto.so',
        'odm/lib64/hw/camera.oemlayer.so',
        'odm/lib64/libmvgcommon.so',
        'odm/lib64/libsharebuffer_impl.so',
        'vendor/lib64/camera/components/com.qti.node.dewarp.so',
    ): blob_fixup()
        .replace_needed('libui.so', 'libui-stock.so'),
    'vendor/lib64/libui-stock.so': blob_fixup()
        .replace_needed('android.hardware.graphics.common-V4-ndk.so', 'android.hardware.graphics.common-V7-ndk.so'),
}  # fmt: skip

module = ExtractUtilsModule(
    'avalon',
    'oneplus',
    namespace_imports=namespace_imports,
    blob_fixups=blob_fixups,
    lib_fixups=lib_fixups,
    add_firmware_proprietary_file=True,
)

add_generated_thermal_engine_config(
    module,
    horae_target_config='system_ext/etc/horae/horae_SM7675.conf',
)

if __name__ == '__main__':
    utils = ExtractUtils.device_with_common(
        module, 'sm8650-common', module.vendor
    )
    utils.run()
