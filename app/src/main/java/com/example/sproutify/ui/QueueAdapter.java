package com.example.sproutify.ui;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.sproutify.R;
import com.example.sproutify.data.QueueManager;
import com.example.sproutify.model.Track;

import java.util.List;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueViewHolder> {
    private List<Track> queue;
    private int currentIndex;
    private OnStartDragListener dragListener;

    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public QueueAdapter(List<Track> queue, int currentIndex, OnStartDragListener dragListener) {
        this.queue = queue;
        this.currentIndex = currentIndex;
        this.dragListener = dragListener;
    }

    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.queue_item, parent, false);
        return new QueueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {
        Track track = queue.get(position);
        holder.titleTextView.setText(track.title);
        holder.artistTextView.setText(track.artist);

        // Charger l'image de couverture
        if (track.coverUrl != null && !track.coverUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(track.coverUrl)
                    .placeholder(R.drawable.placeholder_album)
                    .into(holder.coverImageView);
        } else {
            holder.coverImageView.setImageResource(R.drawable.placeholder_album);
        }

        // Mettre en évidence la piste en cours
        holder.itemView.setAlpha(position == currentIndex ? 1.0f : 0.7f);

        // Configurer le drag handle
        holder.dragHandle.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                dragListener.onStartDrag(holder);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return queue.size();
    }

    public void updateQueue(List<Track> newQueue, int newCurrentIndex) {
        this.queue = newQueue;
        this.currentIndex = newCurrentIndex;
        notifyDataSetChanged();
    }

    static class QueueViewHolder extends RecyclerView.ViewHolder {
        ImageView coverImageView;
        TextView titleTextView;
        TextView artistTextView;
        ImageView dragHandle;

        QueueViewHolder(View itemView) {
            super(itemView);
            coverImageView = itemView.findViewById(R.id.queueItemCover);
            titleTextView = itemView.findViewById(R.id.queueItemTitle);
            artistTextView = itemView.findViewById(R.id.queueItemArtist);
            dragHandle = itemView.findViewById(R.id.queueItemDrag);
        }
    }
} 