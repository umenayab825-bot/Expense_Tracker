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

// *** CHART IMPORTS ***
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

    private static final int PERMISSION_REQUEST_CODE = 100; // Request code for storage permission
    private Databasehelper dbHelper;
    private TransactionAdapter adapter;
    private ArrayList<Transaction> transactionList;
    private TextView txtBalance; // TextView to show total balance
    private BarChart monthlyBarChart; // Bar chart for monthly expenses

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if budget is set for first time
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

        // Load transactions and update UI
        refreshData();

        // Add transaction button click
        btnAdd.setOnClickListener(v -> showAddEditDialog(null));

        // Export button click
        btnExport.setOnClickListener(v -> checkPermissionAndExport());

        // Dark mode switch listener (currently only shows a Toast)
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(MainActivity.this, isChecked ? "Dark Mode ON" : "Dark Mode OFF", Toast.LENGTH_SHORT).show();
        });
    }

    // Check if the user has set the budget for the first time
    private void checkFirstTimeBudget() {
        SharedPreferences prefs = getSharedPreferences("budget_prefs", MODE_PRIVATE);
        boolean isSet = prefs.getBoolean("budget_set", false);

        if (!isSet) {
            showBudgetDialog();
        }
    }

    // Show dialog to set monthly budget
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
                    int income = Integer.parseInt(edtIncome.getText().toString());
                    int expenseP = Integer.parseInt(edtExpensePercent.getText().toString());
                    int savingP = Integer.parseInt(edtSavingPercent.getText().toString());

                    int expenseLimit = income * expenseP / 100;
                    int savingAmount = income * savingP / 100;

                    SharedPreferences.Editor editor =
                            getSharedPreferences("budget_prefs", MODE_PRIVATE).edit();

                    editor.putInt("income", income);
                    editor.putInt("expenseLimit", expenseLimit);
                    editor.putInt("saving", savingAmount);
                    editor.putBoolean("budget_set", true);
                    editor.apply();
                })
                .show();
    }

    // --- CHART DATA LOADING ---
    private void loadMonthlyChartData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Query to get top 5 expense categories
        String query = "SELECT " + Databasehelper.COL_CATEGORY + ", SUM(" + Databasehelper.COL_AMOUNT + ") " +
                "FROM " + Databasehelper.TABLE_NAME +
                " WHERE " + Databasehelper.COL_TYPE + " = 'Expense' " +
                "GROUP BY " + Databasehelper.COL_CATEGORY +
                " ORDER BY SUM(" + Databasehelper.COL_AMOUNT + ") DESC LIMIT 5";

        Cursor cursor = db.rawQuery(query, null);

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> categoryLabels = new ArrayList<>();

        int xPos = 0;
        if (cursor.moveToFirst()) {
            do {
                String category = cursor.getString(0);
                float totalAmount = cursor.getFloat(1);

                entries.add(new BarEntry(xPos, totalAmount));
                categoryLabels.add(category);
                xPos++;
            } while (cursor.moveToNext());
        }
        cursor.close();

        // Setup chart dataset
        BarDataSet dataSet = new BarDataSet(entries, "Expense by Category");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        monthlyBarChart.setData(barData);

        // Configure X-axis labels
        monthlyBarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(categoryLabels));
        monthlyBarChart.getXAxis().setGranularity(1f);
        monthlyBarChart.getXAxis().setDrawGridLines(false);
        monthlyBarChart.getXAxis().setLabelCount(categoryLabels.size());
        monthlyBarChart.getXAxis().setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);

        monthlyBarChart.getDescription().setEnabled(false);
        monthlyBarChart.setFitBars(true);
        monthlyBarChart.animateY(1000);
        monthlyBarChart.getLegend().setEnabled(true);

        monthlyBarChart.invalidate(); // Refresh chart
    }

    // --- DATA REFRESHING AND BALANCE CALCULATION ---
    private void refreshData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        transactionList.clear();
        int totalIncome = 0;
        int totalExpense = 0;

        // Load all transactions from DB
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

                Transaction transaction = new Transaction(id, type, title, amount, category, date, note);
                transactionList.add(transaction);

                if (type.equals("Income")) {
                    totalIncome += amount;
                } else if (type.equals("Expense")) {
                    totalExpense += amount;
                }
            } while (cursor.moveToNext());
        }

        int total = totalIncome - totalExpense;
        cursor.close();

        // Update balance TextView
        txtBalance.setText("Rs. " + total);
        adapter.notifyDataSetChanged();

        // Refresh chart
        loadMonthlyChartData();
    }

    // --- CRUD DIALOG HANDLING ---
    private void showAddEditDialog(Transaction transaction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_edit, null);
        builder.setView(dialogView);

        EditText editTitle = dialogView.findViewById(R.id.editTitle);
        EditText editAmount = dialogView.findViewById(R.id.editAmount);
        EditText editDate = dialogView.findViewById(R.id.editDate);
        EditText editNote = dialogView.findViewById(R.id.editNote);
        Spinner spinnerType = dialogView.findViewById(R.id.spinnerType);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerCategory);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        TextView txtDialogTitle = dialogView.findViewById(R.id.txtDialogTitle);

        // Set dialog title based on add/edit
        if (transaction == null) {
            txtDialogTitle.setText("Add New Transaction");
        } else {
            txtDialogTitle.setText("Edit Transaction");
        }

        // Setup spinners
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this, R.array.transaction_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(this, R.array.expense_categories, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        // Date picker for transaction date
        final Calendar calendar = Calendar.getInstance();
        editDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this,
                    (view, year, monthOfYear, dayOfMonth) -> {
                        calendar.set(year, monthOfYear, dayOfMonth);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
                        editDate.setText(sdf.format(calendar.getTime()));
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        // If editing, populate existing data
        if (transaction != null) {
            editTitle.setText(transaction.getTitle());
            editAmount.setText(String.valueOf(transaction.getAmount()));
            editDate.setText(transaction.getDate());
            editNote.setText(transaction.getNote());

            if (transaction.getType().equals("Income")) {
                spinnerType.setSelection(typeAdapter.getPosition("Income"));
            } else {
                spinnerType.setSelection(typeAdapter.getPosition("Expense"));
            }
            spinnerCategory.setSelection(categoryAdapter.getPosition(transaction.getCategory()));
        } else {
            // Set current date for new transaction
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
            editDate.setText(sdf.format(calendar.getTime()));
        }

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

            // Add or update transaction
            if (transaction == null) {
                dbHelper.addTransaction(type, title, amount, category, date, note);
            } else {
                dbHelper.updateTransaction(transaction.getId(), type, title, amount, category, date, note);
            }

            refreshData();

            // Check if expense exceeds limit
            if (type.equals("Expense")) {
                checkExpenseLimit();
            }

            dialog.dismiss();
        });

        dialog.show();
    }

    // --- EXPENSE LIMIT CHECK ---
    private void checkExpenseLimit() {
        SharedPreferences prefs = getSharedPreferences("budget_prefs", MODE_PRIVATE);
        int expenseLimit = prefs.getInt("expenseLimit", 0);
        int savingAmount = prefs.getInt("saving", 0);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT SUM(" + Databasehelper.COL_AMOUNT + ") FROM "
                        + Databasehelper.TABLE_NAME +
                        " WHERE " + Databasehelper.COL_TYPE + "='Expense'", null);

        int totalExpense = 0;
        if (c.moveToFirst() && !c.isNull(0)) {
            totalExpense = c.getInt(0);
        }
        c.close();

        int remainingExpense = expenseLimit - totalExpense;
        int remainingSaving = savingAmount - (totalExpense - expenseLimit);

        // Show warnings if limits exceeded
        if (totalExpense > expenseLimit) {
            Toast.makeText(this,
                    "⚠️ Expense limit cross ho chuki hai!\nSaving use ho rahi hai!",
                    Toast.LENGTH_LONG).show();
        }

        if (remainingSaving > 0 && totalExpense > expenseLimit) {
            Toast.makeText(this,
                    "Ab sirf Rs. " + remainingSaving + " saving reh gayi hai",
                    Toast.LENGTH_LONG).show();
        }

        if (remainingSaving <= 0 && totalExpense > expenseLimit) {
            Toast.makeText(this,
                    "❌ Aap ne poori saving bhi khatam kar di hai!",
                    Toast.LENGTH_LONG).show();
        }
    }

    // --- LONG CLICK POPUP MENU ---
    @Override
    public void onItemLongClick(Transaction transaction, View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.menu_options, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_edit) {
                showAddEditDialog(transaction);
                return true;
            } else if (itemId == R.id.action_delete) {
                confirmDelete(transaction.getId());
                return true;
            }
            return false;
        });
        popup.show();
    }

    // Confirm deletion of transaction
    private void confirmDelete(int id) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to delete this transaction?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    dbHelper.deleteTransaction(id);
                    refreshData();
                    Toast.makeText(MainActivity.this, "Transaction Deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    // --- EXPORT LOGIC ---
    private void checkPermissionAndExport() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        } else {
            exportDataToCSV();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportDataToCSV();
            } else {
                Toast.makeText(this, "Storage permission denied. Cannot export data.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Export transaction data to CSV file
    private void exportDataToCSV() {
        if (!isExternalStorageWritable()) {
            Toast.makeText(this, "External storage not writable.", Toast.LENGTH_SHORT).show();
            return;
        }

        File folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!folder.exists()) {
            folder.mkdirs();
        }

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

    // Check if external storage is available
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
}
