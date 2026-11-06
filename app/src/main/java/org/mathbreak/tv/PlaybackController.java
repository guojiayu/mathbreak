package org.mathbreak.tv;

import android.content.ComponentName;
import android.content.Context;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import java.util.List;

/** Best effort; only resumes the specific package MathBreak previously asked to pause. */
final class PlaybackController {
    private PlaybackController() {}

    static boolean pause(Context context) {
        QuizState.prefs(context).edit().remove(QuizState.KEY_PAUSED_PACKAGE).apply();
        for (MediaController c : active(context)) {
            PlaybackState state = c.getPlaybackState();
            if (!context.getPackageName().equals(c.getPackageName()) && state != null
                    && state.getState() == PlaybackState.STATE_PLAYING) {
                try {
                    c.getTransportControls().pause();
                    QuizState.prefs(context).edit()
                            .putString(QuizState.KEY_PAUSED_PACKAGE, c.getPackageName()).apply();
                    return true;
                } catch (Exception ignored) { /* Another player may reject pause. */ }
            }
        }
        return false;
    }

    static void resume(Context context) {
        String pkg = QuizState.prefs(context).getString(QuizState.KEY_PAUSED_PACKAGE, "");
        QuizState.prefs(context).edit().remove(QuizState.KEY_PAUSED_PACKAGE).apply();
        if (pkg.isEmpty()) return;
        for (MediaController c : active(context)) {
            if (pkg.equals(c.getPackageName())) {
                PlaybackState state = c.getPlaybackState();
                if (state != null && state.getState() == PlaybackState.STATE_PAUSED) {
                    try { c.getTransportControls().play(); }
                    catch (Exception ignored) { /* Non-cooperating player. */ }
                }
                return;
            }
        }
    }

    private static List<MediaController> active(Context context) {
        try {
            MediaSessionManager manager = (MediaSessionManager)
                    context.getSystemService(Context.MEDIA_SESSION_SERVICE);
            if (manager != null) return manager.getActiveSessions(
                    new ComponentName(context, MediaListenerService.class));
        } catch (SecurityException ignored) { /* Notification access not granted. */ }
        return java.util.Collections.emptyList();
    }
}
