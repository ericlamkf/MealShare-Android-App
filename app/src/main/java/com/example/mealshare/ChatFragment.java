package com.example.mealshare;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout; // Import for Empty State

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mealshare.ChatPage.ChatListModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    private RecyclerView recyclerView;
    private com.example.mealshare.ChatPage.ChatListAdapter adapter;
    private List<ChatListModel> chatList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Variable for the "No Messages" view
    private LinearLayout emptyStateLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerView = view.findViewById(R.id.recycler_view_chats);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Find the empty state layout (Ensure this ID is in your XML)
        emptyStateLayout = view.findViewById(R.id.layout_empty_chat);

        chatList = new ArrayList<>();
        adapter = new com.example.mealshare.ChatPage.ChatListAdapter(getContext(), chatList);
        recyclerView.setAdapter(adapter);

        loadMyChats();
    }

    private void loadMyChats() {
        if (mAuth.getCurrentUser() == null) return;

        db.collection("chats")
                .whereArrayContains("participants", mAuth.getCurrentUser().getUid())
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("ChatFragment", "Error loading chats", error);
                        return;
                    }

                    if (value != null) {
                        chatList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            ChatListModel model = doc.toObject(ChatListModel.class);

                            // 🔥 CRITICAL FIX: Manually set the ID!
                            if (model != null) {
                                model.setRequestId(doc.getId());
                                chatList.add(model);
                            }
                        }

                        // Toggle Empty State Logic
                        if (chatList.isEmpty()) {
                            recyclerView.setVisibility(View.GONE);
                            if (emptyStateLayout != null) emptyStateLayout.setVisibility(View.VISIBLE);
                        } else {
                            recyclerView.setVisibility(View.VISIBLE);
                            if (emptyStateLayout != null) emptyStateLayout.setVisibility(View.GONE);
                        }

                        adapter.notifyDataSetChanged();
                    }
                });
    }
}