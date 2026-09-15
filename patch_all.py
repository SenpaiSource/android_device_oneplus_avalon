import os

def patch_file(filepath, search, insertion):
    if not os.path.exists(filepath): return False
    with open(filepath, 'r') as f: content = f.read()
    if search in content: return False
    if '</resources>' in content and '</resources>' in insertion:
        content = content.replace('</resources>', insertion.replace('</resources>', '') + '\n</resources>')
    elif 'inherit-product' in content and 'inherit-product' in insertion:
        target = "# Inherit from the common OEM chipset makefile."
        if target in content:
            content = content.replace(target, insertion.replace(target, '') + '\n' + target)
        else:
            content += '\n' + insertion
    with open(filepath, 'w') as f: f.write(content)
    return True

snapshot = """    <!-- The amount to scale fullscreen snapshots for Overview and snapshot starting windows. -->
    <item name="config_highResTaskSnapshotScale" format="float" type="dimen">0.7</item>
</resources>"""

webview = """    <!-- Bytes that the PinnerService will pin for WebView -->
    <integer name="config_pinnerWebviewPinBytes">20971520</integer>
</resources>"""

pinner = """    <!-- Array of files to pin to the memory via PinnerService -->
    <string-array translatable="false" name="config_defaultPinnerServiceFiles">
        <item>"/vendor/lib64/libsdmextension.so"</item>
        <item>"/vendor/lib64/libllvm-qgl.so"</item>
    </string-array>
</resources>"""

spammy = """# Silence spammy vendor logs
SPAMMY_LOG_TAGS := \\
    Diag_Lib \\
    AGM \\
    AHAL \\
    CamX

ifneq ($(TARGET_BUILD_VARIANT),eng)
PRODUCT_VENDOR_PROPERTIES += \\
    $(foreach tag,$(SPAMMY_LOG_TAGS),log.tag.$(tag)=S)
endif
inherit-product"""

import sys
step = sys.argv[1]
if step == "snapshot": patch_file("overlay/OPlusFrameworksResTarget/res/values/config.xml", "config_highResTaskSnapshotScale", snapshot)
elif step == "webview": patch_file("overlay/OPlusFrameworksResTarget/res/values/config.xml", "config_pinnerWebviewPinBytes", webview)
elif step == "pinner": patch_file("overlay/OPlusFrameworksResTarget/res/values/config.xml", "config_defaultPinnerServiceFiles", pinner)
elif step == "spammy": patch_file("device.mk", "SPAMMY_LOG_TAGS", spammy)
