package org.mathbreak.tv;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

public final class QuizAlarmReceiver extends BroadcastReceiver {
    static final int NOTIFICATION_ID = 1001;
    static final String CHANNEL = "mathbreak_quiz_due";

    @Override public void onReceive(Context context, Intent intent) {
        if (!QuizState.quiz(context) || QuizState.pending(context)) return;
        long dueAt = QuizState.prefs(context).getLong(QuizState.KEY_NEXT_AT, 0);
        if (dueAt > System.currentTimeMillis() + 2000) {
            QuizScheduler.restore(context);
            return;
        }
        QuizState.prefs(context).edit().putBoolean(QuizState.KEY_DUE, true).apply();
        // Do NOT pause media in a BroadcastReceiver: quiz may be blocked from appearing.
        notifyPending(context);
        // A WindowManager overlay doesn't put the video Activity through onPause/onResume.
        // A foreground-service launch may still be restricted by Android/TV firmware.
        QuizOverlayService.launch(context);
    }

    static void notifyPending(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Math challenge",
                    NotificationManager.IMPORTANCE_HIGH));
        }
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        Intent launch = new Intent(context, MainActivity.class);
        launch.putExtra(MainActivity.EXTRA_AUTO_QUIZ, true);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, 44, launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, CHANNEL) : new Notification.Builder(context);
        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("MathBreak — quiz time")
                .setContentText("One problem to unlock more TV time")
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentIntent(pi).setAutoCancel(true);
        nm.notify(NOTIFICATION_ID, builder.build());
    }

    static void clearNotification(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(NOTIFICATION_ID);
    }
}
