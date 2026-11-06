package org.mathbreak.tv;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

/** A small, focusable system alert window; the underlying video Activity remains resumed. */
public final class QuizOverlayService extends Service {
    private static final int NOTIFICATION = 1002;
    private static final int WHITE = Color.rgb(245, 249, 255);
    private static final int TEAL = Color.rgb(85, 224, 201);
    private static final int RED = Color.rgb(255, 125, 132);
    private WindowManager windows;
    private FrameLayout root;
    private Question question;
    private final boolean[] wrong = new boolean[5];
    private int attempts;
    private boolean hintVisible, pausedByUs, solved;

    static boolean launch(Context context) {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(context)) return false;
        try {
            Intent intent = new Intent(context, QuizOverlayService.class);
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
            else context.startService(intent);
            return true;
        } catch (RuntimeException e) {
            android.util.Log.w("MathBreak", "Overlay service launch rejected", e);
            return false;
        }
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!QuizState.quiz(this) || !QuizState.pending(this)
                || (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this))) {
            stopSelf();
            return START_NOT_STICKY;
        }
        try {
            startAsForeground();
            if (root != null) return START_NOT_STICKY;
            question = new QuestionBank(this).at(QuizState.currentQuestion(this));
            windows = (WindowManager) getSystemService(WINDOW_SERVICE);
            root = new FrameLayout(this) {
                @Override public boolean dispatchKeyEvent(KeyEvent event) {
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                        if (event.getAction() == KeyEvent.ACTION_UP)
                            Toast.makeText(QuizOverlayService.this,
                                    "Solve this problem to continue TV", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    return super.dispatchKeyEvent(event);
                }
            };
            renderQuestion();
            int width = Math.min((int) (getResources().getDisplayMetrics().widthPixels * 0.86f), dp(1040));
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    width, WindowManager.LayoutParams.WRAP_CONTENT,
                    Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            : WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND
                            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.CENTER;
            params.dimAmount = 0.28f;
            windows.addView(root, params);
            QuizAlarmReceiver.clearNotification(this);
            pausedByUs = PlaybackController.pause(this);
        } catch (Exception e) {
            android.util.Log.e("MathBreak", "Unable to show quiz overlay", e);
            QuizAlarmReceiver.notifyPending(this);
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void startAsForeground() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26 && manager != null)
            manager.createNotificationChannel(new NotificationChannel(
                    "mathbreak_active", "MathBreak active challenge", NotificationManager.IMPORTANCE_LOW));
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, "mathbreak_active") : new Notification.Builder(this);
        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("MathBreak quiz")
                .setContentText("Answer correctly to continue TV").setOngoing(true);
        startForeground(NOTIFICATION, builder.build());
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable panel(int color, int border) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(15));
        drawable.setStroke(dp(2), border);
        return drawable;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private void renderQuestion() {
        root.removeAllViews();
        ScrollView scroll = new ScrollView(this) {
            @Override protected void onMeasure(int w, int h) {
                int max = (int) (getResources().getDisplayMetrics().heightPixels * 0.68f);
                super.onMeasure(w, View.MeasureSpec.makeMeasureSpec(max, View.MeasureSpec.AT_MOST));
            }
        };
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(26), dp(20), dp(26), dp(21));
        panel.setBackground(panel(Color.rgb(15, 28, 53), TEAL));
        scroll.addView(panel);
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -2));

        panel.addView(text("MATHBREAK • ONE QUESTION", 16, TEAL, true));
        TextView title = text("Solve to resume your video", 23, WHITE, true);
        title.setPadding(0, dp(8), 0, dp(12));
        panel.addView(title);
        TextView prompt = text(question.prompt, 23, WHITE, false);
        prompt.setPadding(0, 0, 0, dp(16));
        panel.addView(prompt);

        LinearLayout answers = new LinearLayout(this);
        answers.setOrientation(LinearLayout.HORIZONTAL);
        panel.addView(answers, new LinearLayout.LayoutParams(-1, dp(108)));
        Button focus = null;
        for (int i = 0; i < 5; i++) {
            final int choice = i;
            Button button = new Button(this);
            button.setText((char)('A' + i) + "\n" + question.choices[i]);
            button.setAllCaps(false);
            button.setTextSize(17);
            button.setTextColor(wrong[i] ? RED : WHITE);
            button.setGravity(Gravity.CENTER);
            StateListDrawable states = new StateListDrawable();
            states.addState(new int[] {android.R.attr.state_focused},
                    panel(Color.rgb(25, 75, 79), TEAL));
            states.addState(new int[] {}, panel(wrong[i] ? Color.rgb(73, 34, 49)
                    : Color.rgb(22, 40, 68), wrong[i] ? RED : Color.rgb(58, 77, 110)));
            button.setBackground(states);
            button.setEnabled(!wrong[i]);
            button.setOnClickListener(v -> answer(choice));
            LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(0, -1, 1f);
            layout.setMargins(dp(3), 0, dp(3), 0);
            answers.addView(button, layout);
            if (focus == null && !wrong[i]) focus = button;
        }
        TextView status = text(attempts == 0 ? "Use D-pad and OK to choose an answer."
                : "Incorrect; try another answer.", 16, attempts == 0 ? TEAL : RED, false);
        status.setPadding(0, dp(12), 0, dp(6));
        panel.addView(status);
        if (attempts >= 2 && !hintVisible) {
            Button hintButton = new Button(this);
            hintButton.setText("Show hint");
            hintButton.setAllCaps(false);
            hintButton.setOnClickListener(v -> { hintVisible = true; renderQuestion(); });
            panel.addView(hintButton);
        }
        if (hintVisible) panel.addView(text("Hint: " + question.hint, 16, TEAL, false));
        if (focus != null) focus.requestFocus();
    }

    private void answer(int selected) {
        if (solved) return;
        if (selected != question.correct) {
            if (!wrong[selected]) { wrong[selected] = true; attempts++; }
            renderQuestion();
            return;
        }
        solved = true;
        QuizState.markCorrect(this);
        QuizScheduler.afterCorrect(this);
        QuizAlarmReceiver.clearNotification(this);
        Toast.makeText(this, "Correct! Video unlocked.", Toast.LENGTH_SHORT).show();
        stopSelf();
    }

    @Override public void onDestroy() {
        if (root != null && windows != null) {
            try { windows.removeViewImmediate(root); }
            catch (IllegalArgumentException ignored) { }
            root = null;
        }
        if (pausedByUs) PlaybackController.resume(this);
        pausedByUs = false;
        if (!solved && QuizState.quiz(this) && QuizState.pending(this))
            QuizAlarmReceiver.notifyPending(this);
        super.onDestroy();
    }
}
