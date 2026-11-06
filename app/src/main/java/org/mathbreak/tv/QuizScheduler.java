package org.mathbreak.tv;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** One-shot, approximate elapsed watching break timer. No exact-alarm permission needed. */
final class QuizScheduler {
    private QuizScheduler() {}

    private static PendingIntent alarmIntent(Context context) {
        Intent intent = new Intent(context, QuizAlarmReceiver.class);
        return PendingIntent.getBroadcast(context, 42, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void start(Context context, int minutes) {
        long at = System.currentTimeMillis() + minutes * 60_000L;
        QuizState.prefs(context).edit()
                .putString(QuizState.KEY_MODE, QuizState.QUIZ)
                .putInt(QuizState.KEY_MINUTES, minutes)
                .putBoolean(QuizState.KEY_DUE, false)
                .putLong(QuizState.KEY_NEXT_AT, at).apply();
        scheduleAt(context, at);
    }

    static void afterCorrect(Context context) {
        start(context, QuizState.prefs(context).getInt(QuizState.KEY_MINUTES, 30));
    }

    static void restore(Context context) {
        if (!QuizState.quiz(context) || QuizState.pending(context)) return;
        long at = QuizState.prefs(context).getLong(QuizState.KEY_NEXT_AT, 0);
        if (at != 0) scheduleAt(context, Math.max(at, System.currentTimeMillis() + 5000));
    }

    private static void scheduleAt(Context context, long at) {
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm == null) return;
        PendingIntent intent = alarmIntent(context);
        if (Build.VERSION.SDK_INT >= 23) alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent);
        else alarm.set(AlarmManager.RTC_WAKEUP, at, intent);
    }

    static void cancel(Context context) {
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm != null) alarm.cancel(alarmIntent(context));
    }
}
