package org.mathbreak.tv;

/** Plain content model: intentionally independent of Android UI. */
final class Question {
    final String id;
    final String prompt;
    final String[] choices;
    final int correct;
    final String hint;
    final String[] solutions;
    final String videoUrl;
    final String source;

    Question(String id, String prompt, String[] choices, int correct,
             String hint, String[] solutions, String videoUrl, String source) {
        this.id = id;
        this.prompt = prompt;
        this.choices = choices;
        this.correct = correct;
        this.hint = hint;
        this.solutions = solutions;
        this.videoUrl = videoUrl;
        this.source = source;
    }
}
