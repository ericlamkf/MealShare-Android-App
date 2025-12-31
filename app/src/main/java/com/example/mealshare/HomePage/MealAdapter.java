package com.example.mealshare.HomePage;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mealshare.R;

import java.util.List;

public class MealAdapter extends RecyclerView.Adapter<MealAdapter.MealViewHolder> {

    private List<Meal> mealList;
    private Context context;

    public interface OnMealClickListener {
        void onMealClick(Meal meal);
    }

    private OnMealClickListener listener;

    public MealAdapter(Context context, List<Meal> mealList, OnMealClickListener listener) {
        this.context = context;
        this.mealList = mealList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_meal_card, parent, false);
        return new MealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        Meal meal = mealList.get(position);

        holder.foodName.setText(meal.getFoodName());
        holder.quantity.setText(meal.getQuantity() + " available");
        holder.requestedFood.setText("(" + meal.getRequestedQuantity() + " requested)"); // Update requested food
        holder.location.setText(meal.getLocation()); // You might want to shorten this string

        // Load Image using Glide
        if (meal.getImageUrl() != null && !meal.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(meal.getImageUrl())
                    .centerCrop()
                    .placeholder(R.drawable.food_placeholder) // Create a grey placeholder drawable
                    .into(holder.foodImage);
        }

        holder.itemView.setOnClickListener(v -> {
            listener.onMealClick(meal); // Trigger the interface
        });
    }

    @Override
    public int getItemCount() {
        return mealList.size();
    }

    // Helper method to update data
    public void updateList(List<Meal> newList) {
        mealList = newList;
        notifyDataSetChanged();
    }

    public static class MealViewHolder extends RecyclerView.ViewHolder {
        TextView foodName, quantity, location, requestedFood; // Add requestedFood
        ImageView foodImage;

        public MealViewHolder(@NonNull View itemView) {
            super(itemView);
            foodName = itemView.findViewById(R.id.tv_food_name);
            quantity = itemView.findViewById(R.id.tv_quantity);
            requestedFood = itemView.findViewById(R.id.tv_requested_food); // Find requested food view
            location = itemView.findViewById(R.id.tv_location);
            foodImage = itemView.findViewById(R.id.iv_food_image);
        }
    }
}