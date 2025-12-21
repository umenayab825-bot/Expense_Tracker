package com.nayab.expensetracker;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import android.content.SharedPreferences;

public class MainActivity extends AppCompatActivity implements TransactionAdapter.OnItemLongClickListener {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private Databasehelper dbHelper;
    private TransactionAdapter adapter;
    private ArrayList<Transaction> transactionList;
    private TextView txtBalance;
    private BarChart monthlyBarChart;

    private int income;
    private int expenseLimit;
    private int savingAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check budget for first time
        checkFirstTimeBudget();

        dbHelper = new Databasehelper(this);
        transactionList = new ArrayList<>();

        txtBalance = findViewById(R.id.txtBalance);
        monthlyBarChart = findViewById(R.id.monthlyBarChart);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        FloatingActionButton btnAdd = findViewById(R.id.btnAdd);
        FloatingActionButton btnExport = findViewById(R.id.btnExport);
        Switch switchDarkMode = findViewById(R.id.switchDarkMode);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(transactionList, this);
        recyclerView.setAdapter(adapter);

        // Load saved budget values
        SharedPreferences prefs = getSharedPreferences("budget_prefs", MODE_PRIVATE);
        income = prefs.getInt("income", 0);
        expenseLimit = prefs.getInt("expenseLimit", 0);
        savingAmount = prefs.getInt("saving", 0);

        // Load transactions
        refreshData();

        // Add transaction
        btnAdd.setOnClickListener(v -> showAddEditDialog(null));

        // Export transactions
        btnExport.setOnClickListener(v -> checkPermissionAndExport());

        // Dark mode (just toast)
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) ->
                Toast.makeText(MainActivity.this, isChecked ? "Dark Mode ON" : "Dark Mode OFF", Toast.LENGTH_SHORT).show());
    }

    private void checkFirstTimeBudget() {
        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String email = userPrefs.getString("logged_in_email", "");

        if (email.isEmpty()) return;

        SharedPreferences budgetPrefs = getSharedPreferences("budget_prefs", MODE_PRIVATE);

        // 🔑 Email specific key
        boolean isSet = budgetPrefs.getBoolean("budget_set_" + email, false);

        if (!isSet) {
            showBudgetDialog();
        }
    }


    private void showBudgetDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_budget, null);

        EditText edtIncome = view.findViewById(R.id.edtIncome);
        EditText edtExpensePercent = view.findViewById(R.id.edtExpensePercent);
        EditText edtSavingPercent = view.findViewById(R.id.edtSavingPercent);

        new AlertDialog.Builder(this)
                .setTitle("Set Monthly Budget")
                .setCancelable(false)
                .setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    income = Integer.parseInt(edtIncome.getText().toString());
                    int expenseP = Integer.parseInt(edtExpensePercent.getText().toString());
                    int savingP = Integer.parseInt(edtSavingPercent.getText().toString());

                    expenseLimit = income * expenseP / 100;
                    savingAmount = income * savingP / 100;

                    SharedPreferences.Editor editor =
                            getSharedPreferences("budget_prefs", MODE_PRIVATE).edit();
                    editor.putInt("income", income);
                    editor.putInt("expenseLimit", expenseLimit);
                    editor.putInt("saving", savingAmount);
                    SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                    String email = userPrefs.getString("logged_in_email", "");

                    editor.putBoolean("budget_set_" + email, true);

                    editor.apply();
                })
                .show();
    }

    private void refreshData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        transactionList.clear();

        int totalIncome = 0;
        int totalExpense = 0;

        Cursor cursor = db.query(Databasehelper.TABLE_NAME, null, null, null, null, null, Databasehelper.COL_ID + " DESC");

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(Databasehelper.COL_ID));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(Databasehelper.COL_TYPE));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(Databasehelper.COL_TITLE));
                int amount = cursor.getInt(cursor.getColumnIndexOrThrow(Databasehelper.COL_AMOUNT));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(Databasehelper.COL_CATEGORY));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(Databasehelper.COL_DATE));
                String note = cursor.getString(cursor.getColumnIndexOrThrow(Databasehelper.COL_NOTE));

                Transaction t = new Transaction(id, type, title, amount, category, date, note);
                transactionList.add(t);

                if (type.equals("Income")) totalIncome += amount;
                else if (type.equals("Expense")) totalExpense += amount;

            } while (cursor.moveToNext());
        }
        cursor.close();

        int totalBalance = totalIncome - totalExpense;
        txtBalance.setText("Rs. " + totalBalance);
        adapter.notifyDataSetChanged();

        loadMonthlyChartData();
    }

    private void loadMonthlyChartData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT " + Databasehelper.COL_CATEGORY + ", SUM(" + Databasehelper.COL_AMOUNT + ") " +
                "FROM " + Databasehelper.TABLE_NAME +
                " WHERE " + Databasehelper.COL_TYPE + "='Expense' " +
                "GROUP BY " + Databasehelper.COL_CATEGORY +
                " ORDER BY SUM(" + Databasehelper.COL_AMOUNT + ") DESC LIMIT 5";

        Cursor cursor = db.rawQuery(query, null);

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        int x = 0;
        if (cursor.moveToFirst()) {
            do {
                String category = cursor.getString(0);
                float total = cursor.getFloat(1);
                entries.add(new BarEntry(x, total));
                labels.add(category);
                x++;
            } while (cursor.moveToNext());
        }
        cursor.close();

        BarDataSet dataSet = new BarDataSet(entries, "Expense by Category");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        monthlyBarChart.setData(barData);

        monthlyBarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        monthlyBarChart.getXAxis().setGranularity(1f);
        monthlyBarChart.getXAxis().setDrawGridLines(false);
        monthlyBarChart.getXAxis().setLabelCount(labels.size());
        monthlyBarChart.getXAxis().setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);

        monthlyBarChart.getDescription().setEnabled(false);
        monthlyBarChart.setFitBars(true);
        monthlyBarChart.animateY(1000);
        monthlyBarChart.getLegend().setEnabled(true);

        monthlyBarChart.invalidate();
    }

    private void showAddEditDialog(Transaction transaction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_edit, null);
        builder.setView(dialogView);

        EditText editTitle = dialogView.findViewById(R.id.editTitle);
        EditText editAmount = dialogView.findViewById(R.id.editAmount);
        EditText editDate = dialogView.findViewById(R.id.editDate);
        EditText editNote = dialogView.findViewById(R.id.editNote);
        Spinner spinnerType = dialogView.findViewById(R.id.spinnerType);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerCategory);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        TextView txtDialogTitle = dialogView.findViewById(R.id.txtDialogTitle);

        txtDialogTitle.setText(transaction == null ? "Add New Transaction" : "Edit Transaction");

        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this, R.array.transaction_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(this, R.array.expense_categories, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        final Calendar calendar = Calendar.getInstance();
        editDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
                        editDate.setText(sdf.format(calendar.getTime()));
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        if (transaction != null) {
            editTitle.setText(transaction.getTitle());
            editAmount.setText(String.valueOf(transaction.getAmount()));
            editDate.setText(transaction.getDate());
            editNote.setText(transaction.getNote());

            spinnerType.setSelection(typeAdapter.getPosition(transaction.getType()));
            spinnerCategory.setSelection(categoryAdapter.getPosition(transaction.getCategory()));
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
            editDate.setText(sdf.format(calendar.getTime()));
        }

        // ✅ Only ONE dialog variable
        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String title = editTitle.getText().toString();
            String amountStr = editAmount.getText().toString();
            String type = spinnerType.getSelectedItem().toString();
            String category = spinnerCategory.getSelectedItem().toString();
            String date = editDate.getText().toString();
            String note = editNote.getText().toString();

            if (title.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(this, "Title and Amount are required!", Toast.LENGTH_SHORT).show();
                return;
            }

            int amount = Integer.parseInt(amountStr);

            if (type.equals("Expense")) {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor c = db.rawQuery(
                        "SELECT SUM(" + Databasehelper.COL_AMOUNT + ") FROM " +
                                Databasehelper.TABLE_NAME +
                                " WHERE " + Databasehelper.COL_TYPE + "='Expense'", null);

                int totalExpense = 0;
                if (c.moveToFirst() && !c.isNull(0)) totalExpense = c.getInt(0);
                c.close();

                int remainingExpenseLimit = expenseLimit - totalExpense;
                int remainingSaving = savingAmount - Math.max(0, totalExpense - expenseLimit);

                if (amount <= remainingExpenseLimit) {
                    // OK
                } else if (amount <= remainingExpenseLimit + remainingSaving) {
                    Toast.makeText(this,
                            "⚠️ Expense limit exceeded!\nUsing saving. Remaining saving: Rs. " +
                                    (remainingSaving - amount + remainingExpenseLimit),
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this,
                            "❌ Salary and saving exhausted!\nExpense cannot be added.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }

            if (transaction == null) {
                dbHelper.addTransaction(type, title, amount, category, date, note);
            } else {
                dbHelper.updateTransaction(transaction.getId(), type, title, amount, category, date, note);
            }

            refreshData();
            dialog.dismiss();
        });

        dialog.show();
    }





    private void checkExpenseLimit() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT SUM(" + Databasehelper.COL_AMOUNT + ") FROM " +
                        Databasehelper.TABLE_NAME +
                        " WHERE " + Databasehelper.COL_TYPE + "='Expense'", null);

        int totalExpense = 0;
        if (c.moveToFirst() && !c.isNull(0)) totalExpense = c.getInt(0);
        c.close();

        if (totalExpense <= expenseLimit) return;

        int excess = totalExpense - expenseLimit;
        int remainingSaving = savingAmount - excess;

        if (remainingSaving > 0) {
            Toast.makeText(this,
                    "⚠️ Expense limit exceeded!\nRemaining saving: Rs. " + remainingSaving,
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this,
                    "❌ All savings exhausted!",
                    Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onItemLongClick(Transaction transaction, View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.menu_options, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_edit) {
                showAddEditDialog(transaction);
                return true;
            } else if (id == R.id.action_delete) {
                confirmDelete(transaction.getId());
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void confirmDelete(int id) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to delete this transaction?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    dbHelper.deleteTransaction(id);
                    refreshData();
                    Toast.makeText(this, "Transaction Deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void checkPermissionAndExport() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        } else {
            exportDataToCSV();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportDataToCSV();
            } else {
                Toast.makeText(this, "Storage permission denied. Cannot export data.", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void exportDataToCSV() {
        if (!isExternalStorageWritable()) {
            Toast.makeText(this, "External storage not writable.", Toast.LENGTH_SHORT).show();
            return;
        }

        File folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!folder.exists()) folder.mkdirs();

        String fileName = "expense_tracker_export_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Calendar.getInstance().getTime()) + ".csv";
        File file = new File(folder, fileName);

        try {
            FileWriter writer = new FileWriter(file);
            writer.append("ID,Type,Title,Amount,Category,Date,Note\n");

            for (Transaction t : transactionList) {
                writer.append(String.valueOf(t.getId())).append(",");
                writer.append(t.getType()).append(",");
                writer.append(t.getTitle()).append(",");
                writer.append(String.valueOf(t.getAmount())).append(",");
                writer.append(t.getCategory()).append(",");
                writer.append(t.getDate()).append(",");
                writer.append(t.getNote().replace(",", " ")).append("\n");
            }

            writer.flush();
            writer.close();
            Toast.makeText(this, "Data exported successfully to Downloads/" + fileName, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean isExternalStorageWritable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }
}
