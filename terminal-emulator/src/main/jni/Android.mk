LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := termux
LOCAL_SRC_FILES := termux.c
# Termux JNI is vendored and built with NDK r27: explicitly align every
# PT_LOAD segment for Android's supported 16 KiB memory-page devices.
LOCAL_LDFLAGS += -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384
include $(BUILD_SHARED_LIBRARY)
