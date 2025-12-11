package com.nayab.expensetracker;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

// Agar aap delete feature implement karna chahte hain, toh yeh interface use hoga
// public interface OnItemClickListener {
//     void onItemLongClick(int transactionId);
// }

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private final List<Transaction> transactionList;
    // private final OnItemClickListener listener; // Delete feature ke liye

    public TransactionAdapter(List<Transaction> transactionList) {
        this.transactionList = transactionList;
        // this.listener = listener; // Agar delete listener use karna hai
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // row_transaction.xml ko connect karein
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);

        holder.textTitle.setText(transaction.getTitle());
        holder.textCategory.setText(transaction.getCategory());
        holder.textAmount.setText("Rs. " + transaction.getAmount());

        String category = transaction.getCategory();
        String type = transaction.getType();

        // 1. Icon Logic: Category ke mutabiq icon set karna
        // Zaroori hai ke aapke drawable folder mein yeh files (google_food.png, etc.) maujood hon.
        if (category.equalsIgnoreCase("Food")) {
            holder.imgCategory.setImageResource(R.drawable.resturant_icon_removebg_preview);
        } else if (category.equalsIgnoreCase("Transport")) {
            holder.imgCategory.setImageResource(R.drawable.bus_icon_removebg_preview);
        } else if (category.equalsIgnoreCase("Salary")) {
            holder.imgCategory.setImageResource(R.drawable.icom_icon_removebg_preview);
        } else {
            // Default icon for Shopping, Education, Other
            holder.imgCategory.setImageResource(R.drawable.catagorie_icon_removebg_preview);
        }

        // 2. Color Logic: Income/Expense ke mutabiq color set karna
        if (type.equalsIgnoreCase("Income")) {
            holder.textAmount.setTextColor(Color.parseColor("#008000")); // Green
        } else {
            holder.textAmount.setTextColor(Color.parseColor("#D80000")); // Red
        }

        // Agar aapne delete feature implement kiya hai:
        // holder.itemView.setOnLongClickListener(v -> {
        //     listener.onItemLongClick(transaction.getId());
        //     return true;
        // });
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    // ViewHolder class: TextViews aur ImageView ko initialize karna
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textTitle, textAmount, textCategory;
        public ImageView imgCategory;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textAmount = itemView.findViewById(R.id.textAmount);
            textCategory = itemView.findViewById(R.id.textCategory);
            imgCategory = itemView.findViewById(R.id.imgCategory);
        }
    }
}