package com.example.mealshare.ProfilePage;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mealshare.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class MyRequestsAdapter extends RecyclerView.Adapter<MyRequestsAdapter.ViewHolder> {

    private Context context;
    private List<RequestModel> requestList;
    private FirebaseFirestore db;
    private OnFeedbackButtonClickListener feedbackListener;

    public MyRequestsAdapter(Context context, List<RequestModel> requestList) {
        this.context = context;
        this.requestList = requestList;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_request_sent, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RequestModel request = requestList.get(position);

        // --- Basic Info ---
        holder.foodNameTv.setText(request.getFoodName());
        holder.locationTv.setText("📍 " + request.getLocation());
        String fullId = request.getRequestId();
        String shortId = (fullId != null && fullId.length() > 6) ? fullId.substring(0, 6) : fullId;
        holder.requestIdTv.setText("ID: #" + shortId);
        if (request.getFoodImage() != null && !request.getFoodImage().isEmpty()) {
            Glide.with(context).load(request.getFoodImage()).centerCrop().placeholder(R.drawable.ic_launcher_background).into(holder.bgImageView);
        }

        // --- Reset UI for Recycler View ---
        holder.deleteBtn.setVisibility(View.GONE);
        holder.feedbackBtn.setVisibility(View.GONE);
        holder.deleteBtn.setOnClickListener(null); // Clear previous listeners
        holder.feedbackBtn.setOnClickListener(null); // Clear previous listeners

        // --- Status-Based Logic ---
        String status = request.getStatus();

        if ("Pending".equals(status)) {
            holder.statusTv.setText("Pending");
            holder.statusTv.setBackgroundColor(Color.parseColor("#FF9800"));
            holder.deleteBtn.setVisibility(View.VISIBLE);
            holder.deleteBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel); // 'X' icon
            holder.deleteBtn.setOnClickListener(v -> deleteRequest(request.getRequestId(), true, request.getMealId()));

        } else if ("Accepted".equals(status)) {
            holder.statusTv.setText("Ready! ✅");
            holder.statusTv.setBackgroundColor(Color.parseColor("#4CAF50"));

        } else if ("Completed".equals(status)) {
            holder.statusTv.setText("Collected");
            holder.statusTv.setBackgroundColor(Color.GRAY);
            holder.deleteBtn.setVisibility(View.VISIBLE);
            holder.deleteBtn.setImageResource(android.R.drawable.ic_menu_delete); // Trash icon
            holder.deleteBtn.setOnClickListener(v -> deleteRequest(request.getRequestId(), false, null));
            checkAndShowFeedbackButton(holder, request);

        } else if ("Rejected".equals(status)) {
            holder.statusTv.setText("Rejected");
            holder.statusTv.setBackgroundColor(Color.RED);
            holder.deleteBtn.setVisibility(View.VISIBLE);
            holder.deleteBtn.setImageResource(android.R.drawable.ic_menu_delete); // Trash icon
            holder.deleteBtn.setOnClickListener(v -> deleteRequest(request.getRequestId(), false, null));
        } else {
            holder.statusTv.setText(status != null ? status : "Unknown");
        }
    }

    private void checkAndShowFeedbackButton(ViewHolder holder, RequestModel request) {
        db.collection("feedbacks").document(request.getRequestId()).get().addOnSuccessListener(doc -> {
            if (holder.getBindingAdapterPosition() != RecyclerView.NO_POSITION) { // Check if view is still valid
                if (!doc.exists()) {
                    holder.feedbackBtn.setVisibility(View.VISIBLE);
                    holder.feedbackBtn.setOnClickListener(v -> {
                        if (feedbackListener != null) {
                            feedbackListener.onFeedbackClick(request.getDonorId(), request.getRequestId());
                        }
                    });
                }
            }
        });
    }

    private void deleteRequest(String requestId, boolean isCancel, String mealId) {
        if (requestId == null) return;

        db.collection("requests").document(requestId).delete().addOnSuccessListener(aVoid -> {
            String msg = isCancel ? "Request Cancelled" : "Request History Removed";
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            if (isCancel && mealId != null) {
                decrementRequestedQuantity(mealId);
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void decrementRequestedQuantity(String mealId) {
        DocumentReference mealRef = db.collection("meals").document(mealId);
        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(mealRef);
            Long currentReqQty = snapshot.getLong("requestedQuantity");
            if (currentReqQty != null && currentReqQty > 0) {
                transaction.update(mealRef, "requestedQuantity", currentReqQty - 1);
            }
            return null;
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public interface OnFeedbackButtonClickListener {
        void onFeedbackClick(String donorId, String requestId);
    }

    public void setOnFeedbackButtonClickListener(OnFeedbackButtonClickListener listener) {
        this.feedbackListener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView bgImageView;
        TextView foodNameTv, locationTv, statusTv, requestIdTv;
        ImageButton deleteBtn;
        Button feedbackBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            bgImageView = itemView.findViewById(R.id.iv_sent_food_bg);
            foodNameTv = itemView.findViewById(R.id.tv_sent_food_name);
            locationTv = itemView.findViewById(R.id.tv_sent_location);
            statusTv = itemView.findViewById(R.id.tv_sent_status);
            requestIdTv = itemView.findViewById(R.id.tv_req_id_display);
            deleteBtn = itemView.findViewById(R.id.btn_delete_request);
            feedbackBtn = itemView.findViewById(R.id.btn_feedback);
        }
    }
}