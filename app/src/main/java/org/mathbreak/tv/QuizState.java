package org.mathbreak.tv;

import android.content.Context;
import android.content.SharedPreferences;

final class QuizState {
    static final String PREFS = "mathbreak";
    static final String MATH = "MATH";
    static final String QUIZ = "QUIZ";
    static final String KEY_MODE = "mode";
    static final String KEY_DUE = "due";
    static final String KEY_NEXT_AT = "next_at";
    static final String KEY_MINUTES = "minutes";
    static final String KEY_QUESTION = "question";
    static final String KEY_CORRECT = "correct_count";
    static final String KEY_PAUSED_PACKAGE = "paused_package";

    private QuizState() {}

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static boolean quiz(Context context) {
        return QUIZ.equals(prefs(context).getString(KEY_MODE, MATH));
    }

    static boolean pending(Context context) {
        return prefs(context).getBoolean(KEY_DUE, false);
    }

    static int currentQuestion(Context context) {
        return prefs(context).getInt(KEY_QUESTION, 0);
    }

    static void markCorrect(Context context) {
        SharedPreferences p = prefs(context);
        p.edit().putInt(KEY_QUESTION, p.getInt(KEY_QUESTION, 0) + 1)
                .putInt(KEY_CORRECT, p.getInt(KEY_CORRECT, 0) + 1).apply();
    }

    static void enterMath(Context context) {
        prefs(context).edit().putString(KEY_MODE, MATH)
                .putBoolean(KEY_DUE, false).putLong(KEY_NEXT_AT, 0).apply();
        QuizScheduler.cancel(context);
    }
}
