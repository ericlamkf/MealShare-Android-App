package com.example.mealshare.ChatPage;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mealshare.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatRoomActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private TextView toolbarTitle;

    private ChatRoomAdapter adapter;
    private List<ChatMessageModel> messageList;
    private FirebaseFirestore db;

    private String requestId;
    private String currentUserId;
    private String receiverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        // 1. Get Data from Intent
        requestId = getIntent().getStringExtra("requestId");
        String otherUserName = getIntent().getStringExtra("otherUserName");
        String foodName = getIntent().getStringExtra("foodName");
        receiverId = getIntent().getStringExtra("otherUserId");

        if (requestId == null || receiverId == null) {
            Toast.makeText(this, "Error: Chat not found. Missing required info.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 2. Setup UI
        Toolbar toolbar = findViewById(R.id.chat_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        toolbar.setNavigationIcon(R.drawable.outline_arrow_back_24);
        toolbar.setNavigationOnClickListener(v -> finish());

        toolbarTitle = findViewById(R.id.tv_toolbar_name);
        String title = otherUserName;
        if (foodName != null && !foodName.isEmpty()) {
            title += " - " + foodName;
        }
        toolbarTitle.setText(title != null ? title : "Chat");

        messageInput = findViewById(R.id.et_message_input);
        sendButton = findViewById(R.id.btn_send_message);
        recyclerView = findViewById(R.id.recycler_chat_messages);

        // 3. Setup Recycler
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);

        messageList = new ArrayList<>();
        adapter = new ChatRoomAdapter(this, messageList);
        recyclerView.setAdapter(adapter);

        // 4. Setup Firebase
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        loadMessages();

        sendButton.setOnClickListener(v -> sendMessage());

        // Send message on keyboard "Enter" / "Send" press
        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            boolean handled = false;
            if (actionId == EditorInfo.IME_ACTION_SEND || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                sendMessage();
                handled = true;
            }
            return handled;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reset MY unread count to 0 when I open the screen
        if (requestId != null && currentUserId != null) {
            Map<String, Object> resetMap = new HashMap<>();
            resetMap.put("unreadCounts." + currentUserId, 0);

            db.collection("chats").document(requestId).update(resetMap);
        }
    }

    private void loadMessages() {
        db.collection("chats").document(requestId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    if (value != null) {
                        messageList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            messageList.add(doc.toObject(ChatMessageModel.class));
                        }
                        adapter.notifyDataSetChanged();
                        if (!messageList.isEmpty()) {
                            recyclerView.smoothScrollToPosition(messageList.size() - 1);
                        }
                    }
                });
    }

    private void sendMessage() {
        String msgText = messageInput.getText().toString().trim();
        if (TextUtils.isEmpty(msgText)) return;

        messageInput.setText("");

        long timestamp = System.currentTimeMillis();

        // Create Message Object for sub-collection
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("senderId", currentUserId);
        messageMap.put("message", msgText);
        messageMap.put("timestamp", timestamp);

        db.collection("chats").document(requestId).collection("messages")
                .add(messageMap);

        // We use one map called 'updateMap' for everything
        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("lastMessage", msgText);
        updateMap.put("lastMessageTime", timestamp);

        // Only increment if we know who the receiver is
        if (receiverId != null) {
            updateMap.put("unreadCounts." + receiverId, FieldValue.increment(1));
        }

        // Perform the update
        db.collection("chats").document(requestId).update(updateMap);
    }
}
