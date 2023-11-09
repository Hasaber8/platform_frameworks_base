/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.gmscompat;

import android.app.Application;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.SystemProperties;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.Arrays;

/** @hide */
public final class AttestationHooks {
    private static final String TAG = "GmsCompat/Attestation";
    private static final boolean DEBUG = false;

    private static final String PACKAGE_GMS = "com.google.android.gms";
    private static final String PACKAGE_FINSKY = "com.android.vending";
    private static final String PROCESS_UNSTABLE = "com.google.android.gms.unstable";

    private static volatile boolean sIsGms = false;
    private static volatile boolean sIsFinsky = false;

    private AttestationHooks() { }

    private static void setBuildField(String key, String value) {
        try {
            Log.i(TAG, "Spoofing Build." + key + " with value \"" + value + "\"");
            Field field = Build.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to spoof Build." + key, e);
        }
    }

    private static void setVersionField(String key, Object value) {
        try {
            Log.i(TAG, "Spoofing Build.VERSION." + key + " with value \"" + value + "\"");
            Field field = Build.VERSION.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to spoof Build." + key, e);
        }
    }

    private static void spoofBuildGms() {
        setBuildField("PRODUCT", "CatS60");
        setBuildField("DEVICE", "CatS60");
        setBuildField("MANUFACTURER", "BullittGroupLimited");
        setBuildField("BRAND", "Cat");
        setBuildField("MODEL", "S60");
        setBuildField("FINGERPRINT", "Cat/CatS60/CatS60:6.0.1/MMB29M/LTE_D0201121.0_S60_0.031.02:user/release-keys");
        //setVersionField("SECURITY_PATCH", "2017-01-01");
        //setVersionField("DEVICE_INITIAL_SDK_INT", Build.VERSION_CODES.N);
    }

    public static void initApplicationBeforeOnCreate(Application app) {
        if (PACKAGE_GMS.equals(app.getPackageName()) &&
                PROCESS_UNSTABLE.equals(Application.getProcessName())) {
            sIsGms = true;
            spoofBuildGms();
        }

        if (PACKAGE_FINSKY.equals(app.getPackageName())) {
            sIsFinsky = true;
        }
    }

    private static boolean isCallerSafetyNet() {
        return sIsGms && Arrays.stream(Thread.currentThread().getStackTrace())
                .anyMatch(elem -> elem.getClassName().contains("DroidGuard"));
    }

    public static void onEngineGetCertificateChain() {
        if (isCallerSafetyNet() || sIsFinsky) {
            Log.i(TAG, "Blocked key attestation sIsGms=" + sIsGms + " sIsFinsky=" + sIsFinsky);
            throw new UnsupportedOperationException();
        }
    }
}
