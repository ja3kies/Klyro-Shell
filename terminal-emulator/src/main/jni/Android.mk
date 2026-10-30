LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := termux
LOCAL_SRC_FILES := termux.c
include $(BUILD_SHARED_LIBRARY)
