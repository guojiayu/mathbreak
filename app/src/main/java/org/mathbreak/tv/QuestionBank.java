package org.mathbreak.tv;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class QuestionBank {
    private final List<Question> questions;

    QuestionBank(Context context) throws IOException, JSONException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = context.getAssets().open("questions.json")) {
            byte[] chunk = new byte[4096];
            int n;
            while ((n = in.read(chunk)) != -1) out.write(chunk, 0, n);
        }
        JSONArray array = new JSONArray(new String(out.toByteArray(), StandardCharsets.UTF_8));
        questions = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            JSONArray values = item.getJSONArray("choices");
            if (values.length() != 5) throw new JSONException("Expected five choices for " + i);
            String[] choices = new String[5];
            for (int j = 0; j < 5; j++) choices[j] = values.getString(j);
            JSONArray variants = item.getJSONArray("solutions");
            String[] solutions = new String[variants.length()];
            for (int j = 0; j < variants.length(); j++) solutions[j] = variants.getString(j);
            int correct = item.getInt("correct");
            if (correct < 0 || correct > 4 || solutions.length == 0)
                throw new JSONException("Invalid answer/solution for " + i);
            questions.add(new Question(item.getString("id"), item.getString("prompt"),
                    choices, correct, item.optString("hint"), solutions,
                    item.optString("videoUrl"), item.optString("source")));
        }
        if (questions.isEmpty()) throw new JSONException("Question bank is empty");
    }

    int size() { return questions.size(); }
    Question at(int index) { return questions.get(index % questions.size()); }
}
