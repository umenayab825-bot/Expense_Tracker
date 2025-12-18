package com.nayab.expensetracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private ArrayList<Transaction> transactionList;
    private OnItemLongClickListener listener;

    public interface OnItemLongClickListener {
        void onItemLongClick(Transaction transaction, View view);
    }

    public TransactionAdapter(ArrayList<Transaction> transactionList, OnItemLongClickListener listener) {
        this.transactionList = transactionList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // row_transaction.xml is used here
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction currentItem = transactionList.get(position);

        holder.txtTitle.setText(currentItem.getTitle());
        holder.txtAmount.setText("Rs. " + currentItem.getAmount());
        holder.txtDate.setText(currentItem.getDate());
        holder.txtCategory.setText(currentItem.getCategory());

        // Set text color based on type
        int color;
        Context context = holder.itemView.getContext();
        if (currentItem.getType().equals("Income")) {
            // Use a green color resource for Income
            color = context.getResources().getColor(android.R.color.holo_green_dark);
        } else {
            // Use a red color resource for Expense
            color = context.getResources().getColor(android.R.color.holo_red_dark);
        }
        holder.txtAmount.setTextColor(color);

        // Long click listener for Edit/Delete Popup Menu
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onItemLongClick(currentItem, v);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        public TextView txtTitle;
        public TextView txtAmount;
        public TextView txtDate;
        public TextView txtCategory;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ensure these IDs match those in row_transaction.xml
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtAmount = itemView.findViewById(R.id.txtAmount);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtCategory = itemView.findViewById(R.id.txtCategory);
        }
    }
}