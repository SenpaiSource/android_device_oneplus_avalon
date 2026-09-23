/*
 * Copyright (C) 2024 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.qualcomm.qti.poweroffalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "PowerOffAlarm/Boot";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Boot completed, starting AlarmService");
        context.startService(new Intent(context, AlarmService.class));
    }
}
