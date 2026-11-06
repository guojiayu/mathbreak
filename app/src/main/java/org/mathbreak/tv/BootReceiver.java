package org.mathbreak.tv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (QuizState.pending(context)) QuizAlarmReceiver.notifyPending(context);
        else QuizScheduler.restore(context);
    }
}
