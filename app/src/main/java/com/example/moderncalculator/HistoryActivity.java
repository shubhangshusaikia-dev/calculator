package com.example.moderncalculator;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Displays persisted calculations and lets the user clear them. */
public class HistoryActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private ArrayAdapter<String> adapter;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        prefs = getSharedPreferences("calculator_prefs", MODE_PRIVATE);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, loadHistory());
        ((ListView) findViewById(R.id.historyList)).setAdapter(adapter);
        findViewById(R.id.clearHistoryButton).setOnClickListener(v -> {
            prefs.edit().remove(MainActivity.KEY_HISTORY).apply();
            adapter.clear();
            adapter.notifyDataSetChanged();
        });
    }

    private List<String> loadHistory() {
        Set<String> saved = prefs.getStringSet(MainActivity.KEY_HISTORY, new HashSet<>());
        List<String> rows = new ArrayList<>(saved);
        Collections.sort(rows, Comparator.reverseOrder());
        List<String> cleaned = new ArrayList<>();
        for (String row : rows) cleaned.add(row.substring(row.indexOf('|') + 1));
        return cleaned;
    }
}
