/*
 * Copyright (C) 2024 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.qualcomm.qti.poweroffalarm;

import android.app.AlarmManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ServiceManager;
import android.util.Log;

import vendor.qti.hardware.alarm.IAlarm;

public class AlarmService extends Service {

    private static final String TAG = "PowerOffAlarm";
    private static final String SERVICE = "vendor.qti.hardware.alarm.IAlarm/default";

    /**
     * Boot-lead buffer: the RTC alarm is programmed this many milliseconds
     * BEFORE the actual alarm time so the device has time to cold-boot.
     * Stock OOS uses 120,000 ms (2 minutes) for alarm-clock apps.
     */
    private static final long BOOT_BUFFER_MS = 120_000L;

    /** Retry delay when the HAL service is not yet registered (ms). */
    private static final long HAL_RETRY_DELAY_MS = 3_000L;
    private static final int  HAL_RETRY_MAX      = 10;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private int mHalRetries = 0;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mHalRetries = 0; // reset retry counter on each new alarm event
            syncRtcAlarm();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        registerReceiver(mReceiver,
                new IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED),
                Context.RECEIVER_NOT_EXPORTED);
        syncRtcAlarm();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private void syncRtcAlarm() {
        AlarmManager am = getSystemService(AlarmManager.class);
        if (am == null) return;

        AlarmManager.AlarmClockInfo next = am.getNextAlarmClock();

        IAlarm hal;
        try {
            hal = IAlarm.Stub.asInterface(ServiceManager.getService(SERVICE));
        } catch (Exception e) {
            Log.e(TAG, "Exception getting Alarm HAL service", e);
            hal = null;
        }

        if (hal == null) {
            if (mHalRetries < HAL_RETRY_MAX) {
                mHalRetries++;
                Log.w(TAG, "Alarm HAL not available yet, retry " + mHalRetries
                        + "/" + HAL_RETRY_MAX + " in " + HAL_RETRY_DELAY_MS + "ms");
                mHandler.postDelayed(this::syncRtcAlarm, HAL_RETRY_DELAY_MS);
            } else {
                Log.e(TAG, "Alarm HAL " + SERVICE + " permanently unavailable");
            }
            return;
        }

        mHalRetries = 0;

        try {
            if (next != null) {
                // Subtract boot buffer so hardware wakes up 2 min before alarm rings.
                long rtcMs  = next.getTriggerTime() - BOOT_BUFFER_MS;
                long rtcSec = rtcMs / 1000L;
                Log.i(TAG, "Next alarm: " + next.getTriggerTime() / 1000L
                        + "s  RTC target (−2min): " + rtcSec + "s");
                hal.setAlarm(rtcSec);
            } else {
                Log.i(TAG, "No pending alarm clock — cancelling RTC alarm via HAL");
                hal.cancelAlarm();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to communicate with Alarm HAL", e);
        }
    }
}
