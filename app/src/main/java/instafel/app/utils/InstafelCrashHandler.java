/*
 * (c) 2026 Muhammed Ali Bulut, All rights reserved.
 *
 * See LICENSE file in repository root for copy file of license. For copyright
 * notices, technical issues, feedback, or any other related to this code file or
 * project, please contact me via mamii@mamii.dev or other ways.
 */

package instafel.app.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import instafel.app.activity.crash_manager.ifl_a_crash_viewer;
import instafel.app.managers.CrashManager;

public class InstafelCrashHandler implements Thread.UncaughtExceptionHandler {

    private final Context mContext;

    public InstafelCrashHandler(Context context) {
        mContext = context;
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        Log.e("InstafelCrash", "Uncaught exception intercepted by InstafelCrashHandler", e);

        JSONObject logObject = null;
        String formattedLog = null;
        try {
            CrashManager crashManager = new CrashManager(mContext);
            logObject = crashManager.saveLog(e);
            formattedLog = CrashManager.formatCrashLog(logObject);

            // Automatically copy to clipboard immediately so the user can paste right away
            ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && formattedLog != null) {
                ClipData clip = ClipData.newPlainText("Instafel Crash Log", formattedLog);
                clipboard.setPrimaryClip(clip);
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        // Display user notification Toast
        try {
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(mContext, "Instafel crashed! Error log copied to clipboard.", Toast.LENGTH_LONG).show();
            });
        } catch (Throwable ignored) {}

        // Launch interactive Crash Viewer in independent :crash process
        try {
            Intent intent = new Intent(mContext, ifl_a_crash_viewer.class);
            if (logObject != null) {
                intent.putExtra("data", logObject.toString());
            }
            intent.putExtra("from_crash", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mContext.startActivity(intent);
        } catch (Throwable launchEx) {
            launchEx.printStackTrace();
        }

        try {
            Thread.sleep(600);
        } catch (InterruptedException ignored) {}

        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }
}