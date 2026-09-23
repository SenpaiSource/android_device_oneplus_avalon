/*
 * Copyright (C) 2024 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package vendor.qti.hardware.alarm;

interface IAlarm {
    int cancelAlarm();
    long getAlarm();
    long getRtcTime();
    int setAlarm(in long time);
}
