package com.example.letschat.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.letschat.R;
import com.example.letschat.common.Common;
import com.example.letschat.databinding.RecentChatRowBinding;
import com.example.letschat.model.ChatroomModel;
import com.example.letschat.model.UserModel;
import com.example.letschat.utils.AndroidUtil;
import com.example.letschat.utils.FirebaseUtil;
import com.example.letschat.view.chat.ChatActivity;
import com.example.letschat.view.display.ViewImageActivity;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

public class RecyclerChatsAdapter extends FirestoreRecyclerAdapter<ChatroomModel, RecyclerChatsAdapter.ViewHolder> {

    private Context context;
    private LayoutInflater layoutInflater;
    private int lastPosition = -1;

    public RecyclerChatsAdapter(@NonNull FirestoreRecyclerOptions<ChatroomModel> options, Context context) {
        super(options);
        this.context = context;
        this.layoutInflater = LayoutInflater.from(context);
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.recent_chat_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull ChatroomModel model) {
        FirebaseUtil.getOtherUserFromChatroom(model.getUserIds()).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            UserModel otherUserModel = task.getResult().toObject(UserModel.class);

                            if (otherUserModel == null) {
                                return;
                            }

                            FirebaseUtil.getChatroomMessageReference(model.getChatRoomId())
                                    .whereEqualTo("senderId", otherUserModel.getUserId())
                                    .whereEqualTo("read", false)
                                    .get()
                                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                        @Override
                                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                            int unreadMessages = queryDocumentSnapshots.size();
                                            if (unreadMessages == 0) {
                                                holder.binding.tvMessageCount.setVisibility(View.GONE);
                                            }
                                            else {
                                                holder.binding.tvMessageCount.setText(String.valueOf(unreadMessages));
                                                holder.binding.tvMessageCount.setVisibility(View.VISIBLE);
                                            }
                                        }
                                    });

                            boolean messageSentByMe = model.getLastMessageSenderId().equals(FirebaseUtil.currentUserId());

                            if ( messageSentByMe ) {
                                holder.binding.tvMessage.setText("You: " + model.getLastMessage());
                            }
                            else {
                                holder.binding.tvMessage.setText(model.getLastMessage());
                            }
                            try {
                                Glide.with(context).load(otherUserModel.getProfileImage())
                                        .placeholder(R.drawable.person_placeholder_360x360)
                                        .into(holder.binding.civProfile);
                            }
                            catch (NullPointerException e) {
                                Glide.with(context).load(R.drawable.person_placeholder_360x360)
                                        .into(holder.binding.civProfile);
                            }

                            holder.binding.tvUsername.setText(otherUserModel.getUserName());
                            holder.binding.tvDatetime.setText(FirebaseUtil.formatTimestamp(model.getLastMessageTimestamp()));
                            // holder.binding.tvMessageCount.setText(model.getMessageCount());

                            holder.binding.civProfile.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    AndroidUtil.transitToViewImage(context, holder.binding.civProfile,
                                            otherUserModel.getProfileImage(), otherUserModel.getUserName());
                                }
                            });

                            holder.itemView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent(context, ChatActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    AndroidUtil.passUserModelAsIntent(intent, otherUserModel);
                                    context.startActivity(intent);
                                }
                            });

                            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                                @Override
                                public boolean onLongClick(View v) {
                                    // TODO: Chat Selection Functionality
                                    return true;
                                }
                            });

                            setAnimation(holder.itemView);
                        }
                    }
                });
    }


    public class ViewHolder extends RecyclerView.ViewHolder {

        RecentChatRowBinding binding;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = RecentChatRowBinding.bind(itemView);
        }
    }

    private void setAnimation(View view) {
        Animation slideInLeft = AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left);
        view.startAnimation(slideInLeft);
    }
}
