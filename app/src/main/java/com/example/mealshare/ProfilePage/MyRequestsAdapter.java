package com.example.mealshare.ProfilePage;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton; // Import ImageButton
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast; // Import Toast

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mealshare.FeedbackFragment;
import com.example.mealshare.R;
import com.google.firebase.firestore.FirebaseFirestore; // Import Firestore

import java.util.List;

public class MyRequestsAdapter extends RecyclerView.Adapter<MyRequestsAdapter.ViewHolder> {

    private Context context;
    private List<RequestModel> requestList;
    private FirebaseFirestore db;

    public MyRequestsAdapter(Context context, List<RequestModel> requestList) {
        this.context = context;
        this.requestList = requestList;
        this.db = FirebaseFirestore.getInstance(); // Initialize DB
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

        holder.foodNameTv.setText(request.getFoodName());
        holder.locationTv.setText("📍 " + request.getLocation());

        String fullId = request.getRequestId();
        String shortId = (fullId != null && fullId.length() > 6) ? fullId.substring(0, 6) : fullId;
        holder.requestIdTv.setText("ID: #" + shortId);

        if (request.getFoodImage() != null && !request.getFoodImage().isEmpty()) {
            Glide.with(context)
                    .load(request.getFoodImage())
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(holder.bgImageView);
        }

        // 🔥 LOGIC: Check Status for Delete Button Visibility
        String status = request.getStatus();

        if ("Completed".equals(status) || "Rejected".equals(status)) {
            // IF DONE: Show Delete Button
            holder.deleteBtn.setVisibility(View.VISIBLE);
            holder.feedbackBtn.setVisibility(View.VISIBLE);

            // Set Text
            if ("Completed".equals(status)) {
                holder.statusTv.setText("Collected");
                holder.statusTv.setBackgroundColor(Color.GRAY);
                holder.feedbackBtn.setVisibility(View.GONE);

                // 🔥 Check if feedback already exists
                db.collection("feedbacks")
                        .document(request.getRequestId())
                        .get()
                        .addOnSuccessListener(doc -> {
                            if (!doc.exists()) {
                                // No feedback yet → show button
                                holder.feedbackBtn.setVisibility(View.VISIBLE);
                            }
                        });

                holder.feedbackBtn.setOnClickListener(v -> {
                    if (feedbackListener != null) {
                        feedbackListener.onFeedbackClick(
                                request.getDonorId(),
                                request.getRequestId()
                        );
                    }
                });
            } else {
                holder.statusTv.setText("Rejected");
                holder.statusTv.setBackgroundColor(Color.RED);
            }

            // Set Delete Action
            holder.deleteBtn.setOnClickListener(v -> deleteRequest(request.getRequestId()));
            holder.feedbackBtn.setOnClickListener(v -> {
                // Use the listener to pass the donorId
                if (feedbackListener != null && request.getDonorId() != null) {
                    feedbackListener.onFeedbackClick(request.getDonorId(), request.getRequestId());
                }
            });


        } else {
            // IF ACTIVE: Hide Delete Button
            holder.deleteBtn.setVisibility(View.GONE);
            holder.feedbackBtn.setVisibility(View.GONE);

            if ("Pending".equals(status)) {
                holder.statusTv.setText("Pending");
                holder.statusTv.setBackgroundColor(Color.parseColor("#FF9800"));
            } else if ("Accepted".equals(status)) {
                holder.statusTv.setText("Ready! ✅");
                holder.statusTv.setBackgroundColor(Color.parseColor("#4CAF50"));
            }
        }
    }

    // 🔥 Helper Method to Delete
    private void deleteRequest(String requestId) {
        if (requestId == null) return;

        db.collection("requests").document(requestId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Request History Removed", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private OnFeedbackButtonClickListener feedbackListener; // Add this

    public interface OnFeedbackButtonClickListener {
        void onFeedbackClick(String donorId, String requestId);
    }

    public void setOnFeedbackButtonClickListener(OnFeedbackButtonClickListener listener) {
        this.feedbackListener = listener;
    }


        @Override
    public int getItemCount() {
        return requestList.size();
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
            deleteBtn = itemView.findViewById(R.id.btn_delete_request); // Find ID
            feedbackBtn = itemView.findViewById(R.id.btn_feedback); // Find ID
        }
    }
}