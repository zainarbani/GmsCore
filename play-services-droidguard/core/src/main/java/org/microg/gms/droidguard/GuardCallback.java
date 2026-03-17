/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaDrm;
import android.util.Log;

import org.microg.gms.droidguard.core.DgpDatabaseHelper;
import org.microg.gms.droidguard.core.FallbackCreator;
import org.microg.gms.settings.SettingsContract;

import java.util.HashMap;

import static android.os.Build.VERSION.SDK_INT;

/**
 * Callbacks invoked from the DroidGuard VM
 * <p>
 * We keep this file in Java to ensure ABI compatibility.
 * Methods are invoked by name from within the VM and thus must keep current name.
 */
public class GuardCallback {
    private static final String TAG = "GmsGuardCallback";
    private final Context context;
    private final String packageName;

    public GuardCallback(Context context, String packageName) {
        this.context = context;
        this.packageName = packageName;
    }

    public final String a(final byte[] array) {
        Log.d(TAG, "a[?](" + array + ")");
        return new String(FallbackCreator.create(new HashMap<>(), array, "", context, null));
    }

    // getAndroidId
    public final String b() {
        try {
            long androidId = SettingsContract.INSTANCE.getSettings(context, SettingsContract.CheckIn.INSTANCE.getContentUri(context), new String[]{SettingsContract.CheckIn.ANDROID_ID}, cursor -> cursor.getLong(0));
            Log.d(TAG, "b[getAndroidId]() = " + androidId);
            return String.valueOf(androidId);
        } catch (Throwable e) {
            Log.w(TAG, "Failed to get Android ID, fallback to random", e);
        }
        long androidId = (long) (Math.random() * Long.MAX_VALUE);
        Log.d(TAG, "b[getAndroidId]() = " + androidId + " (random)");
        return String.valueOf(androidId);
    }

    // getPackageName
    public final String c() {
        Log.d(TAG, "c[getPackageName]() = " + packageName);
        return packageName;
    }

    // closeMediaDrmSession
    public final void d(final Object mediaDrm, final byte[] sessionId) {
        Log.d(TAG, "d[closeMediaDrmSession](" + mediaDrm + ", " + sessionId + ")");
        synchronized (MediaDrmLock.LOCK) {
            if (SDK_INT >= 18) {
                ((MediaDrm) mediaDrm).closeSession(sessionId);
            }
        }
    }

    public final void e(final int task) {
        DgpDatabaseHelper helper = new DgpDatabaseHelper(context);
        SQLiteDatabase db = null;
        try {
            db = helper.getWritableDatabase();
            db.beginTransaction();
            db.delete("t", null, null);
            if (task == 0 || task == 1) {
                ContentValues values = new ContentValues();
                values.put("a", new byte[]{(byte) task});
                db.insert("t", null, values);
            }
            db.setTransactionSuccessful();
        } catch (Throwable t) {
            Log.w(TAG, "Failed to persist callback task state", t);
        } finally {
            if (db != null && db.inTransaction()) {
                db.endTransaction();
            }
            helper.close();
        }
    }
}
