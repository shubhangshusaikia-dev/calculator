package com.example.moderncalculator;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.HashSet;
import java.util.Set;

/** Controller: connects the XML views to the CalculatorEngine model. */
public class MainActivity extends AppCompatActivity {
    private static final String PREFS = "calculator_prefs";
    private static final String KEY_EXPRESSION = "last_expression";
    private static final String KEY_RESULT = "last_result";
    public static final String KEY_HISTORY = "history";

    private final CalculatorEngine engine = new CalculatorEngine();
    private TextView expressionText, resultText;
    private HorizontalScrollView expressionScroll;
    private String expression = "";
    private SharedPreferences prefs;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        expressionText = findViewById(R.id.expressionText);
        resultText = findViewById(R.id.resultText);
        expressionScroll = findViewById(R.id.expressionScroll);
        expression = prefs.getString(KEY_EXPRESSION, "");
        resultText.setText(prefs.getString(KEY_RESULT, ""));
        updateExpression();
        wireButtons();
    }

    private void wireButtons() {
        int[] numberIds = {R.id.btn0,R.id.btn1,R.id.btn2,R.id.btn3,R.id.btn4,R.id.btn5,R.id.btn6,R.id.btn7,R.id.btn8,R.id.btn9};
        for (int id : numberIds) findViewById(id).setOnClickListener(v -> appendNumber(((MaterialButton) v).getText().toString()));
        findViewById(R.id.btnDot).setOnClickListener(v -> appendDot());
        findViewById(R.id.btnPlus).setOnClickListener(v -> appendOperator("+"));
        findViewById(R.id.btnMinus).setOnClickListener(v -> appendOperator("-"));
        findViewById(R.id.btnMultiply).setOnClickListener(v -> appendOperator("×"));
        findViewById(R.id.btnDivide).setOnClickListener(v -> appendOperator("÷"));
        findViewById(R.id.btnPercent).setOnClickListener(v -> appendPercent());
        findViewById(R.id.btnSign).setOnClickListener(v -> toggleSign());
        findViewById(R.id.btnAC).setOnClickListener(v -> clearAll());
        findViewById(R.id.btnDel).setOnClickListener(v -> deleteLast());
        findViewById(R.id.btnEquals).setOnClickListener(v -> calculate());
        findViewById(R.id.copyButton).setOnClickListener(v -> copyResult());
        findViewById(R.id.historyButton).setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
    }

    private void appendNumber(String value) {
        feedback();
        if ("0".equals(expression)) expression = value; else expression += value;
        preview(); updateExpression();
    }

    private void appendDot() {
        feedback();
        if (currentNumber().contains(".")) return;
        expression += currentNumber().isEmpty() || endsWithOperator() ? "0." : ".";
        updateExpression();
    }

    private void appendOperator(String op) {
        feedback();
        if (expression.isEmpty()) return;
        if (endsWithOperator()) expression = expression.substring(0, expression.length() - 1) + op; else expression += op;
        resultText.setText(""); updateExpression();
    }

    private void appendPercent() {
        feedback();
        if (!expression.isEmpty() && !endsWithOperator() && !expression.endsWith("%")) expression += "%";
        preview(); updateExpression();
    }

    private void toggleSign() {
        feedback();
        int start = currentNumberStart();
        if (start < expression.length() && expression.charAt(start) == '-') expression = expression.substring(0, start) + expression.substring(start + 1);
        else expression = expression.substring(0, start) + "-" + expression.substring(start);
        updateExpression();
    }

    private void clearAll() { feedback(); expression = ""; resultText.setText(""); updateExpression(); saveLast(); }
    private void deleteLast() { feedback(); if (!expression.isEmpty()) expression = expression.substring(0, expression.length() - 1); preview(); updateExpression(); }

    private void calculate() {
        feedback();
        if (expression.isEmpty() || endsWithOperator()) return;
        String result = engine.evaluate(expression);
        resultText.setText(result);
        if (!"Error".equals(result)) addHistory(expression + " = " + result);
        saveLast();
    }

    private void preview() {
        if (!expression.isEmpty() && !endsWithOperator()) resultText.setText(engine.evaluate(expression));
    }

    private void updateExpression() {
        expressionText.setText(expression.isEmpty() ? "0" : expression);
        expressionScroll.post(() -> expressionScroll.fullScroll(View.FOCUS_RIGHT));
        saveLast();
    }

    private boolean endsWithOperator() { return expression.matches(".*[+\\-×÷]"); }
    private String currentNumber() { int start = currentNumberStart(); return expression.substring(start).replace("%", ""); }
    private int currentNumberStart() {
        for (int i = expression.length() - 1; i >= 0; i--) {
            char c = expression.charAt(i);
            boolean operator = c == '+' || c == '-' || c == '×' || c == '÷';
            boolean unaryMinus = c == '-' && (i == 0 || "+-×÷".indexOf(expression.charAt(i - 1)) >= 0);
            if (operator && !unaryMinus) return i + 1;
        }
        return 0;
    }

    private void saveLast() { prefs.edit().putString(KEY_EXPRESSION, expression).putString(KEY_RESULT, resultText.getText().toString()).apply(); }
    private void addHistory(String item) { Set<String> set = new HashSet<>(prefs.getStringSet(KEY_HISTORY, new HashSet<>())); set.add(System.currentTimeMillis() + "|" + item); prefs.edit().putStringSet(KEY_HISTORY, set).apply(); }

    private void copyResult() {
        String value = resultText.getText().toString();
        if (value.isEmpty()) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Calculator result", value));
        Toast.makeText(this, "Result copied", Toast.LENGTH_SHORT).show();
    }

    private void feedback() {
        View root = expressionText;
        root.animate().scaleX(0.98f).scaleY(0.98f).setDuration(50).withEndAction(() -> root.animate().scaleX(1f).scaleY(1f).setDuration(50)).start();
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }
}
