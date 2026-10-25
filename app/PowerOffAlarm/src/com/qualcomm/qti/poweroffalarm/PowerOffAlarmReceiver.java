/*
 * Copyright (C) 2024 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.qualcomm.qti.poweroffalarm;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vendor.qti.hardware.alarm.IAlarm;

public class PowerOffAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "PowerOffAlarm";
    private static final String ALARM_HAL_SERVICE = "vendor.qti.hardware.alarm.IAlarm/default";

    // Stock actions
    private static final String ACTION_SET_ALARM = "org.codeaurora.poweroffalarm.action.SET_ALARM";
    private static final String ACTION_CANCEL_ALARM = "org.codeaurora.poweroffalarm.action.CANCEL_ALARM";

    // Extra from stock clock apps
    private static final String EXTRA_TIME = "time";

    // SharedPreferences keys
    private static final String PREF_NAME = "power_off_alarm";
    private static final String PREF_KEY_ALARM_TIME = "alarm_time";
    private static final String PREF_KEY_RTC_TIME = "rtc_time";

    /**
     * Boot lead buffer (ms): RTC alarm is set this many milliseconds before the actual
     * alarm trigger time so the device has ample time to cold-boot to the lockscreen.
     * Matches stock 90-second lead time.
     */
    private static final long BOOT_BUFFER_MS = 90_000L;

    /**
     * Single-threaded executor for handling RTC alarm synchronization off the main thread.
     */
    private static final ExecutorService sExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        final String action = intent.getAction();
        Log.d(TAG, "Received broadcast action: " + action);

        final PendingResult pendingResult = goAsync();
        final Context appContext = context.getApplicationContext();

        sExecutor.execute(() -> {
            PowerManager.WakeLock wakeLock = null;
            try {
                PowerManager pm = appContext.getSystemService(PowerManager.class);
                if (pm != null) {
                    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PowerOffAlarm:SyncWakeLock");
                    wakeLock.acquire(10_000L); // 10s safety timeout
                }

                handleAlarmEvent(appContext, intent);
            } catch (Exception e) {
                Log.e(TAG, "Error handling alarm broadcast", e);
            } finally {
                if (wakeLock != null && wakeLock.isHeld()) {
                    wakeLock.release();
                }
                pendingResult.finish();
            }
        });
    }

    private void handleAlarmEvent(Context context, Intent intent) {
        final String action = intent.getAction();

        IAlarm hal = getAlarmHal();
        if (hal == null) {
            Log.e(TAG, "Alarm HAL service unavailable, cannot program RTC alarm");
            return;
        }

        // Direct cancel action from stock clock
        if (ACTION_CANCEL_ALARM.equals(action)) {
            cancelRtcAlarm(context, hal);
            return;
        }

        long explicitTime = 0L;
        if (ACTION_SET_ALARM.equals(action)) {
            explicitTime = intent.getLongExtra(EXTRA_TIME, 0L);
        }

        // Query earliest upcoming alarm across users (for AOSP, Google Clock, third party, and stock)
        long triggerMs = explicitTime > 0L ? explicitTime : getEarliestAlarmTriggerMs(context);

        if (triggerMs <= 0L) {
            Log.i(TAG, "No upcoming alarms scheduled, cancelling RTC wake alarm");
            cancelRtcAlarm(context, hal);
            return;
        }

        setRtcAlarm(context, hal, triggerMs);
    }

    private long getEarliestAlarmTriggerMs(Context context) {
        AlarmManager am = context.getSystemService(AlarmManager.class);
        if (am == null) {
            return 0L;
        }

        long earliestTriggerMs = Long.MAX_VALUE;

        // Check user profiles if UserManager is available
        UserManager um = context.getSystemService(UserManager.class);
        if (um != null) {
            try {
                List<UserHandle> userProfiles = um.getUserProfiles();
                if (userProfiles != null) {
                    for (UserHandle user : userProfiles) {
                        AlarmManager.AlarmClockInfo info = am.getNextAlarmClock(user.getIdentifier());
                        if (info != null && info.getTriggerTime() > 0L) {
                            earliestTriggerMs = Math.min(earliestTriggerMs, info.getTriggerTime());
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed querying alarm clocks per user profile", e);
            }
        }

        // Fallback to default user
        if (earliestTriggerMs == Long.MAX_VALUE) {
            AlarmManager.AlarmClockInfo info = am.getNextAlarmClock();
            if (info != null && info.getTriggerTime() > 0L) {
                earliestTriggerMs = info.getTriggerTime();
            }
        }

        return earliestTriggerMs == Long.MAX_VALUE ? 0L : earliestTriggerMs;
    }

    private void setRtcAlarm(Context context, IAlarm hal, long triggerMs) {
        try {
            long nowMs = System.currentTimeMillis();
            long deltaMs = triggerMs - nowMs - BOOT_BUFFER_MS;

            if (deltaMs <= 0L) {
                if (triggerMs <= nowMs) {
                    // Alarm is already in the past, nothing to program
                    cancelRtcAlarm(context, hal);
                    return;
                }
                // Alarm is imminent (within 90 seconds), wake in 5s if user immediately powers off
                deltaMs = 5_000L;
            }

            long deltaSec = deltaMs / 1000L;
            long rtcNow = hal.getRtcTime();
            long rtcTargetSec = rtcNow + deltaSec;

            Log.i(TAG, "Scheduling RTC alarm: trigger in " + ((triggerMs - nowMs) / 1000L)
                    + "s, RTC now: " + rtcNow + "s, target: " + rtcTargetSec + "s (wake in " + deltaSec + "s)");

            int result = hal.setAlarm(rtcTargetSec);
            if (result == 0) {
                saveAlarmPreferences(context, triggerMs, rtcTargetSec);
            } else {
                Log.e(TAG, "hal.setAlarm returned error code: " + result);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception while setting RTC alarm via HAL", e);
        }
    }

    private void cancelRtcAlarm(Context context, IAlarm hal) {
        try {
            int result = hal.cancelAlarm();
            Log.i(TAG, "Cancelled RTC alarm via HAL (res=" + result + ")");
            saveAlarmPreferences(context, 0L, 0L);
        } catch (Exception e) {
            Log.e(TAG, "Exception while cancelling RTC alarm via HAL", e);
        }
    }

    private IAlarm getAlarmHal() {
        try {
            IBinder binder = ServiceManager.waitForService(ALARM_HAL_SERVICE);
            if (binder == null) {
                binder = ServiceManager.getService(ALARM_HAL_SERVICE);
            }
            if (binder != null) {
                return IAlarm.Stub.asInterface(binder);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to connect to Alarm HAL " + ALARM_HAL_SERVICE, e);
        }
        return null;
    }

    private void saveAlarmPreferences(Context context, long triggerMs, long rtcSec) {
        try {
            Context deContext = context.createDeviceProtectedStorageContext();
            SharedPreferences prefs = deContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putLong(PREF_KEY_ALARM_TIME, triggerMs)
                    .putLong(PREF_KEY_RTC_TIME, rtcSec)
                    .apply();
        } catch (Exception e) {
            Log.w(TAG, "Failed to save alarm preferences", e);
        }
    }
}
