package com.nayab.expensetracker;

public class Transaction {
    private int id;
    private String type;
    private String title;
    private int amount;
    private String category;
    private String date;
    private String note;

    public Transaction(int id, String type, String title, int amount, String category, String date, String note) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.note = note;
    }

    // Getters (Zaroori hain)
    public int getId() { return id; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public int getAmount() { return amount; }
    public String getCategory() { return category; }
    public String getDate() { return date; }
    public String getNote() { return note; }

    // Setters (Agar zaroorat ho to)
    public void setTitle(String title) { this.title = title; }
    public void setAmount(int amount) { this.amount = amount; }
}