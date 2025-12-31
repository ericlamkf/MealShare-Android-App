package com.example.mealshare.ProfilePage;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
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
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.List;

public class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.ViewHolder> {

    private Context context;
    private List<RequestModel> requestList;
    private FirebaseFirestore db;

    public RequestsAdapter(Context context, List<RequestModel> requestList) {
        this.context = context;
        this.requestList = requestList;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_request_received, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RequestModel request = requestList.get(position);

        holder.foodNameTv.setText(request.getFoodName());
        holder.requesterNameTv.setText("Requester ID: " + request.getRequesterId());
        holder.statusTv.setText(request.getStatus());

        if (request.getFoodImage() != null && !request.getFoodImage().isEmpty()) {
            Glide.with(context)
                    .load(request.getFoodImage())
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(holder.foodBgImageView);
        }

        // Logic for Completed vs Pending
        if ("Completed".equals(request.getStatus())) {
            // SHOW DELETE, HIDE CONFIRM
            holder.statusTv.setText("Collected");
            holder.statusTv.setBackgroundColor(Color.GRAY);
            holder.confirmBtn.setVisibility(View.GONE);
            holder.deleteBtn.setVisibility(View.VISIBLE);

            holder.deleteBtn.setOnClickListener(v -> {
                deleteRequest(request.getRequestId());
            });

        } else {
            // SHOW CONFIRM, HIDE DELETE
            holder.statusTv.setText("Pending");
            holder.statusTv.setBackgroundColor(Color.parseColor("#F57C00"));
            holder.confirmBtn.setVisibility(View.VISIBLE);
            holder.deleteBtn.setVisibility(View.GONE);

            holder.confirmBtn.setOnClickListener(v -> {
                confirmHandover(request, holder.getBindingAdapterPosition());
            });
        }
    }

    private void deleteRequest(String requestId) {
        if (requestId == null) {
            Toast.makeText(context, "Error: Request ID is null", Toast.LENGTH_SHORT).show();
            return;
        }

        // We need to fetch the request first to get the mealId, so we can decrement requestedQuantity
        db.collection("requests").document(requestId).get().addOnSuccessListener(requestSnap -> {
            if (requestSnap.exists()) {
                String mealId = requestSnap.getString("mealId");
                String status = requestSnap.getString("status");

                // Only decrement requestedQuantity if the request was PENDING (not yet completed)
                // If it was Completed, the requestedQuantity was already decremented during confirmation
                boolean shouldDecrement = "Pending".equals(status) || "Accepted".equals(status);

                db.collection("requests").document(requestId).delete().addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Request Removed", Toast.LENGTH_SHORT).show();
                    
                    if (shouldDecrement && mealId != null) {
                        decrementRequestedQuantity(mealId);
                    }
                });
            } else {
                // Just try to delete if snapshot failed
                 db.collection("requests").document(requestId).delete();
            }
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

    private void confirmHandover(RequestModel request, int position) {
        if (request.getMealId() == null || request.getRequestId() == null) {
            Toast.makeText(context, "Error: Missing ID. Reload page.", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference mealRef = db.collection("meals").document(request.getMealId());
        DocumentReference requestRef = db.collection("requests").document(request.getRequestId());

        db.runTransaction(transaction -> {
            DocumentSnapshot mealSnapshot = transaction.get(mealRef);

            if (!mealSnapshot.exists()) {
                throw new FirebaseFirestoreException("Meal deleted!", FirebaseFirestoreException.Code.ABORTED);
            }

            // Safe parsing for Quantity (String vs Long)
            long currentQty = 0;
            Object qtyObj = mealSnapshot.get("quantity");

            if (qtyObj instanceof Long) {
                currentQty = (Long) qtyObj;
            } else if (qtyObj instanceof String) {
                try {
                    currentQty = Long.parseLong((String) qtyObj);
                } catch (NumberFormatException e) {
                    currentQty = 0;
                }
            }
            
            Long currentReqQtyLong = mealSnapshot.getLong("requestedQuantity");
            long currentReqQty = (currentReqQtyLong != null) ? currentReqQtyLong : 0;

            if (currentQty <= 0) {
                throw new FirebaseFirestoreException("Out of stock!", FirebaseFirestoreException.Code.ABORTED);
            }

            long newQty = currentQty - 1;
            long newReqQty = (currentReqQty > 0) ? currentReqQty - 1 : 0;
            
            transaction.update(mealRef, "quantity", String.valueOf(newQty));
            transaction.update(mealRef, "requestedQuantity", newReqQty); // Decrement requested count too
            transaction.update(requestRef, "status", "Completed");

            // FIX: Mark as Out of Stock if quantity hits 0
            if (newQty == 0) {
                transaction.update(mealRef, "status", "Out of stock");
            }

            return null;

        }).addOnSuccessListener(result -> {
            Toast.makeText(context, "Handover Recorded.", Toast.LENGTH_SHORT).show();
            // Local update for instant feedback
            request.setStatus("Completed");
            notifyItemChanged(position);

        }).addOnFailureListener(e -> {
            Toast.makeText(context, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView foodBgImageView;
        ImageButton deleteBtn; // Ensure XML has this ID
        TextView statusTv, foodNameTv, requesterNameTv;
        Button confirmBtn; // Ensure XML has these IDs;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            deleteBtn = itemView.findViewById(R.id.btn_delete_request);
            foodBgImageView = itemView.findViewById(R.id.iv_req_food_bg);
            statusTv = itemView.findViewById(R.id.tv_req_status);
            foodNameTv = itemView.findViewById(R.id.tv_req_food_name);
            requesterNameTv = itemView.findViewById(R.id.tv_req_user_name);
            confirmBtn = itemView.findViewById(R.id.btn_confirm_handover);
        }
    }
}