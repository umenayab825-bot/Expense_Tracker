package com.nayab.expensetracker;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Databasehelper dbHelper;
    private TransactionAdapter adapter;
    private List<Transaction> transactionList;
    private TextView txtBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtBalance = findViewById(R.id.txtBalance);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        FloatingActionButton btnAdd = findViewById(R.id.btnAdd);

        dbHelper = new Databasehelper(this);
        transactionList = new ArrayList<>();

        adapter = new TransactionAdapter(transactionList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> showAddTransactionDialog());

        refreshData();
        // Dark Mode Switch Initialization
        Switch switchDarkMode = findViewById(R.id.switchDarkMode);

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Modern UI logic from proposal [cite: 22, 39]
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
    }

    private void showAddTransactionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Naya Transaction Add Karein");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText inputTitle = new EditText(this);
        inputTitle.setHint("Kahan kharch kia? (e.g. Lunch)");
        layout.addView(inputTitle);

        final EditText inputAmount = new EditText(this);
        inputAmount.setHint("Raqam?");
        inputAmount.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(inputAmount);

        // SPINNER IMPLEMENTATION
        final Spinner categorySpinner = new Spinner(this);
        String[] categories = {"Food", "Transport", "Shopping", "Education", "Salary", "Other"};
        // ArrayAdapter fix here: changed SpinnerAdapter to String
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(spinnerAdapter);
        layout.addView(categorySpinner);

        builder.setView(layout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String title = inputTitle.getText().toString();
            String amountStr = inputAmount.getText().toString();
            String cat = categorySpinner.getSelectedItem().toString();

            if (!title.isEmpty() && !amountStr.isEmpty()) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                // Proposal objective: Record income vs expense
                values.put(Databasehelper.COL_TYPE, cat.equals("Salary") ? "Income" : "Expense");
                values.put(Databasehelper.COL_TITLE, title);
                values.put(Databasehelper.COL_AMOUNT, Integer.parseInt(amountStr));
                values.put(Databasehelper.COL_CATEGORY, cat);
                db.insert(Databasehelper.TABLE_NAME, null, values);
                refreshData();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void refreshData() {
        transactionList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + Databasehelper.TABLE_NAME, null);

        int total = 0;
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String type = cursor.getString(1);
                String title = cursor.getString(2);
                int amount = cursor.getInt(3);
                String category = cursor.getString(4);

                transactionList.add(new Transaction(id, type, title, amount, category, "", ""));

                if (type.equalsIgnoreCase("Income")) total += amount;
                else total -= amount;
            } while (cursor.moveToNext());
        }
        cursor.close();
        // Dashboard requirement: Display balance update
        txtBalance.setText("Rs. " + total);
        adapter.notifyDataSetChanged();
    }

    public void deleteTransaction(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(Databasehelper.TABLE_NAME, Databasehelper.COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        refreshData();
    }
}