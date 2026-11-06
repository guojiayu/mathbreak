package org.mathbreak.tv;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

/** Remote-first, dependency-free TV Activity. No embedded video player. */
public final class MainActivity extends Activity {
    static final String EXTRA_AUTO_QUIZ = "auto_quiz";
    private static final int C_BG = Color.rgb(11, 19, 38);
    private static final int C_CARD = Color.rgb(20, 34, 62);
    private static final int C_WHITE = Color.rgb(244, 249, 255);
    private static final int C_MUTED = Color.rgb(166, 186, 211);
    private static final int C_TEAL = Color.rgb(85, 224, 201);
    private static final int C_RED = Color.rgb(255, 125, 132);

    private QuestionBank bank;
    private Question question;
    private LinearLayout column;
    private LinearLayout outer;
    private boolean isSolved = false;
    private boolean[] wrong = new boolean[5];
    private int wrongCount;
    private boolean showHint = false;
    private String screen = "";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        try { bank = new QuestionBank(this); }
        catch (Exception error) {
            new AlertDialog.Builder(this).setTitle("Question bank error")
                    .setMessage(error.getMessage()).setPositiveButton("Close", (d, w) -> finish()).show();
            return;
        }
        launchCurrentMode();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        launchCurrentMode();
    }

    private void launchCurrentMode() {
        if (bank == null) return;
        if (!QuizState.prefs(this).getBoolean("onboarded", false)) {
            showWelcome();
            return;
        }
        isSolved = false;
        wrongCount = 0;
        showHint = false;
        wrong = new boolean[5];
        question = bank.at(QuizState.currentQuestion(this));
        if (QuizState.quiz(this)) {
            if (QuizState.pending(this) && QuizOverlayService.launch(this)) {
                finish();
            } else showWaiting();
        } else showQuestion();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }

    private GradientDrawable bg(int color, int border) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(17));
        if (border != Color.TRANSPARENT) shape.setStroke(dp(2), border);
        return shape;
    }

    private StateListDrawable selectableBackground() {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, bg(Color.rgb(26, 70, 78), C_TEAL));
        states.addState(new int[]{android.R.attr.state_pressed}, bg(Color.rgb(28, 76, 76), C_TEAL));
        states.addState(new int[]{}, bg(C_CARD, Color.rgb(50, 73, 109)));
        return states;
    }

    private void startScreen(String state, String eyebrow, String title, String subtitle) {
        screen = state;
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(C_BG);
        scroll.setClipToPadding(false);
        LinearLayout host = new LinearLayout(this);
        host.setGravity(Gravity.CENTER_HORIZONTAL);
        host.setOrientation(LinearLayout.VERTICAL);
        host.setPadding(dp(36), dp(27), dp(36), dp(24));
        scroll.addView(host, new ScrollView.LayoutParams(-1, -1));
        column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(24), dp(12), dp(24), dp(16));
        column.setBackground(bg(Color.rgb(15, 27, 50), Color.rgb(28, 47, 80)));
        column.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        host.addView(column);
        outer = host;
        setContentView(scroll);
        TextView header = text("● MATHBREAK TV     •     " + eyebrow.toUpperCase(Locale.US), 16, C_TEAL, true);
        column.addView(header, spaced(-1, -2, 0, 0, 0, 12));
        TextView heading = text(title, 32, C_WHITE, true);
        column.addView(heading, spaced(-1, -2, 0, 0, 0, 10));
        if (!TextUtils.isEmpty(subtitle)) {
            TextView desc = text(subtitle, 17, C_MUTED, false);
            column.addView(desc, spaced(-1, -2, 0, 0, 0, 20));
        }
    }

    private LinearLayout.LayoutParams spaced(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h));
        lp.setMargins(dp(l), dp(t), dp(r), dp(b));
        return lp;
    }

    private TextView text(String content, int sp, int color, boolean strong) {
        TextView view = new TextView(this);
        view.setText(content);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setLineSpacing(dp(3), 1.1f);
        if (strong) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button action(String label, View.OnClickListener action) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setAllCaps(false);
        btn.setTextSize(17);
        btn.setTextColor(C_WHITE);
        btn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        btn.setMinHeight(dp(62));
        btn.setPadding(dp(16), dp(8), dp(16), dp(8));
        btn.setBackground(selectableBackground());
        btn.setFocusable(true);
        btn.setOnFocusChangeListener((v, focused) -> {
            v.animate().scaleX(focused ? 1.018f : 1f)
                    .scaleY(focused ? 1.025f : 1f).setDuration(100).start();
        });
        btn.setOnClickListener(action);
        return btn;
    }

    private void singleButton(String label, View.OnClickListener click) {
        column.addView(action(label, click), spaced(-1, -2, 0, 3, 0, 9));
    }

    private void info(String message, int color) {
        TextView v = text(message, 17, color, false);
        v.setPadding(dp(14), dp(12), dp(14), dp(12));
        v.setBackground(bg(C_CARD, Color.TRANSPARENT));
        column.addView(v, spaced(-1, -2, 0, 9, 0, 13));
    }

    private void showWelcome() {
        startScreen("WELCOME", "WELCOME TO MATHBREAK", "Make TV time count",
                "Solve math in continuous Math Mode. After one correct answer,"
                + " choose a 15, 30 or 45-minute TV interval."
                + " Quiz Mode will interrupt with just one question at a time.");
        info("Designed for Android TV remotes (D-pad + OK).\n"
                + "Please grant 'Display over other apps' before relying on automatic challenges."
                + " Playback pause/resume also needs optional Notification Access and a compatible video app.", C_TEAL);
        singleButton("Start Math Mode", v -> {
            QuizState.prefs(this).edit().putBoolean("onboarded", true).apply();
            launchCurrentMode();
        });
        singleButton("Set up device permissions", v -> showSettings());
    }

    private void showQuestion() {
        boolean isQuiz = QuizState.quiz(this) && QuizState.pending(this);
        startScreen("QUESTION", isQuiz ? "QUIZ MODE • ONE PROBLEM" : "MATH MODE • CONTINUOUS",
                isQuiz ? "One question to unlock TV" : "Time for a math challenge",
                "Problem " + ((QuizState.currentQuestion(this) % bank.size()) + 1)
                        + " of " + bank.size() + "   •   Use ← → and OK to answer");
        TextView p = text(question.prompt, 27, C_WHITE, true);
        p.setPadding(dp(10), dp(15), dp(10), dp(24));
        column.addView(p, spaced(-1, -2, 0, 3, 0, 12));
        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.HORIZONTAL);
        column.addView(choices, spaced(-1, -2, 0, 4, 0, 10));
        Button firstAvailable = null;
        for (int i = 0; i < 5; i++) {
            final int index = i;
            Button b = action((char)('A' + i) + "\n" + question.choices[i], v -> answer(index));
            b.setTextSize(18);
            b.setGravity(Gravity.CENTER);
            b.setMinHeight(dp(115));
            if (wrong[i]) {
                b.setTextColor(C_RED);
                b.setBackground(bg(Color.rgb(75, 33, 51), C_RED));
                b.setEnabled(false);
            } else if (firstAvailable == null) firstAvailable = b;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(118), 1f);
            lp.setMargins(dp(5), 0, dp(5), 0);
            choices.addView(b, lp);
        }
        if (wrongCount != 0) {
            info("Not quite — try another answer. " + (wrongCount >= 2 ? "A hint is available." : ""), C_RED);
        }
        if (showHint) info("Hint: " + question.hint, C_TEAL);
        if (wrongCount >= 2 && !showHint) singleButton("Show hint", v -> { showHint = true; showQuestion(); });
        TextView foot = text(isQuiz
                        ? "The video continues after one correct answer. Back cannot dismiss this question."
                        : "Correct answers unlock the next problem or a timed TV break.", 15, C_MUTED, false);
        column.addView(foot, spaced(-1, -2, 0, 19, 0, 4));
        if (firstAvailable != null) firstAvailable.requestFocus();
    }

    private void answer(int selected) {
        if (!"QUESTION".equals(screen)) return;
        if (selected != question.correct) {
            if (!wrong[selected]) {
                wrong[selected] = true;
                wrongCount++;
            }
            showQuestion();
            return;
        }
        QuizState.markCorrect(this);
        isSolved = true;
        showCorrect();
    }

    private void showCorrect() {
        startScreen("CORRECT", "MATH MODE • UNLOCKED", "✓ Correct answer!",
                "Great work. Choose another challenge, review solutions, or watch TV for a while.");
        info("Answer " + (char)('A' + question.correct) + " — " + question.choices[question.correct], C_TEAL);
        singleButton("Next problem →", v -> nextProblem());
        singleButton("Read solutions", v -> showSolutions());
        if (!TextUtils.isEmpty(question.videoUrl))
            singleButton("Watch video solution", v -> openSolutionVideo());
        singleButton("Switch to Quiz Mode • TV timer", v -> showBreakMenu());
        singleButton("App permissions / settings", v -> showSettings());
    }

    private void nextProblem() {
        isSolved = false;
        wrong = new boolean[5];
        wrongCount = 0;
        showHint = false;
        question = bank.at(QuizState.currentQuestion(this));
        showQuestion();
    }

    private void showSolutions() {
        startScreen("SOLUTIONS", "WORKED EXPLANATIONS", "Why does that answer work?",
                question.id + " • Multiple methods when available");
        for (int i = 0; i < question.solutions.length; i++) {
            TextView title = text("Solution " + (i + 1), 21, C_TEAL, true);
            column.addView(title, spaced(-1, -2, 0, 10, 0, 8));
            info(question.solutions[i], C_WHITE);
        }
        if (!TextUtils.isEmpty(question.source)) {
            TextView attribution = text("Source: " + question.source, 14, C_MUTED, false);
            column.addView(attribution, spaced(-1, -2, 0, 12, 0, 12));
        }
        if (!TextUtils.isEmpty(question.videoUrl))
            singleButton("Open video explanation", v -> openSolutionVideo());
        singleButton("← Back to solved question", v -> showCorrect());
    }

    private void openSolutionVideo() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(question.videoUrl));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No app installed to open video explanations", Toast.LENGTH_LONG).show();
        }
    }

    private void showBreakMenu() {
        if (!isSolved || QuizState.quiz(this)) {
            Toast.makeText(this, "Solve the current math problem first", Toast.LENGTH_SHORT).show();
            return;
        }
        startScreen("BREAK", "CHOOSE YOUR MODE", "Switch to Quiz Mode",
                "Watch TV for an interval, then MathBreak asks exactly one question."
                    + " Once answered correctly, TV returns automatically and the timer starts again.");
        singleButton("15 minutes of TV", v -> beginQuizMode(15));
        singleButton("30 minutes of TV", v -> beginQuizMode(30));
        singleButton("45 minutes of TV", v -> beginQuizMode(45));
        singleButton("Stay in Math Mode", v -> showCorrect());
    }

    private void beginQuizMode(int minutes) {
        QuizScheduler.start(this, minutes);
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            new AlertDialog.Builder(this)
                .setTitle("Allow automatic math challenges")
                .setMessage("Android TV usually requires 'Display over other apps' permission"
                        + " for a normal APK to reopen the quiz automatically."
                        + " Without it, a notification may be the only fallback."
                        + " Allow the permission now?")
                .setPositiveButton("Open permission settings", (d, w) -> openOverlaySettings(true))
                .setNegativeButton("Continue without permission", (d, w) -> finish())
                .setCancelable(false).show();
        } else finish();
    }

    private void showWaiting() {
        int minutes = QuizState.prefs(this).getInt(QuizState.KEY_MINUTES, 30);
        long deadline = QuizState.prefs(this).getLong(QuizState.KEY_NEXT_AT, 0);
        long remaining = Math.max(0, (deadline - System.currentTimeMillis() + 59_999) / 60_000);
        boolean due = QuizState.pending(this);
        startScreen("WAITING", "QUIZ MODE • TV BREAK",
                due ? "Your question is ready" : "Quiz Mode is running",
                due ? "Allow display-over-apps permission to show the question in a smaller overlay."
                    : "MathBreak asks one question after each interval. Your previous video app stays on screen.");
        if (due) singleButton("Show pending quiz overlay", v -> {
            if (QuizOverlayService.launch(this)) finish();
            else showSettings();
        });
        info("Interval: " + minutes + " minutes  •  Next quiz in approximately " + remaining + " minutes", C_TEAL);
        singleButton("Return to TV", v -> finish());
        singleButton("Practice Math Mode now", v -> {
            new AlertDialog.Builder(this).setTitle("Switch to Math Mode?")
                    .setMessage("This cancels the current Quiz Mode timer and starts continuous practice.")
                    .setPositiveButton("Switch", (d, w) -> {
                        QuizState.enterMath(this);
                        launchCurrentMode();
                    }).setNegativeButton("Stay in Quiz Mode", null).show();
        });
        singleButton("Permissions / settings", v -> showSettings());
        if (!due && (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            singleButton("TEST: Trigger one quiz now", v -> {
                QuizState.prefs(this).edit().putBoolean(QuizState.KEY_DUE, true).apply();
                launchCurrentMode();
            });
        }
    }

    private void showSettings() {
        startScreen("SETTINGS", "DEVICE SETUP", "System-wide permissions",
                "MathBreak uses a normal APK. Android TV policies and video player support vary by manufacturer.");
        boolean overlay = Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this);
        info("Display over other apps: " + (overlay ? "ALLOWED" : "NOT ALLOWED")
                + "\nRequired for the best chance of auto-showing a quiz above other apps.",
                overlay ? C_TEAL : C_RED);
        singleButton("Configure display-over-apps permission", v -> openOverlaySettings(false));
        info("Notification access: optional\nEnables best-effort pause/resume through active MediaSession controls.", C_MUTED);
        singleButton("Configure notification listener access", v -> {
            try { startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)); }
            catch (ActivityNotFoundException e) { Toast.makeText(this, "Settings unavailable on this TV", Toast.LENGTH_LONG).show(); }
        });
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            singleButton("Allow quiz fallback notifications", v ->
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 9));
        }
        singleButton("← Back", v -> {
            if (!QuizState.prefs(this).getBoolean("onboarded", false)) showWelcome();
            else if (QuizState.quiz(this)) {
                showWaiting();
            } else if (isSolved) showCorrect();
            else showQuestion();
        });
    }

    private void openOverlaySettings(boolean finishAfter) {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, finishAfter ? 101 : 102);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Overlay settings unavailable on this TV", Toast.LENGTH_LONG).show();
            if (finishAfter) finish();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101) finish();
        else if (requestCode == 102) {
            if (QuizState.quiz(this) && QuizState.pending(this)) launchCurrentMode();
            else showSettings();
        }
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && !"QUESTION".equals(screen)) {
            showSettings();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override public void onBackPressed() {
        switch (screen) {
            case "QUESTION":
                // Cannot prevent the Android Home button / switching apps from a normal APK.
                new AlertDialog.Builder(this).setTitle("Math challenge is still locked")
                        .setMessage("Solve this question to unlock Quiz Mode. Previous problems are not part of the Back history.")
                        .setPositiveButton("Keep solving", null).show();
                break;
            case "SOLUTIONS": showCorrect(); break;
            case "BREAK": showCorrect(); break;
            case "CORRECT": showBreakMenu(); break;
            case "SETTINGS":
                if (!QuizState.prefs(this).getBoolean("onboarded", false)) {
                    showWelcome();
                } else if (QuizState.quiz(this)) {
                    showWaiting();
                } else if (isSolved) showCorrect(); else showQuestion();
                break;
            case "WAITING": finish(); break;
            case "WELCOME": finish(); break;
            default: super.onBackPressed();
        }
    }
}
