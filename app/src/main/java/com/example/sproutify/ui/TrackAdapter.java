package com.example.sproutify.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sproutify.R;
import com.example.sproutify.model.Track;
import com.squareup.picasso.Picasso;

import java.util.List;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackVH> {

    public interface OnTrackClickListener {
        void onTrackClick(Track track, int position);
    }

    private final List<Track> data;
    private final OnTrackClickListener listener;
    private final Context ctx;

    public TrackAdapter(Context ctx, List<Track> data, OnTrackClickListener l) {
        this.ctx = ctx;
        this.data = data;
        this.listener = l;
    }

    @NonNull @Override
    public TrackVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_track, parent, false);
        return new TrackVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackVH h, int pos) {
        Track t = data.get(pos);

        h.title.setText(t.title);
        h.artist.setText(t.artist);
        Picasso.get()
                .load(t.coverUrl)
                .placeholder(R.drawable.ic_album_placeholder)
                .into(h.cover);

        h.itemView.setOnClickListener(v ->
                listener.onTrackClick(t, pos));
    }


    @Override public int getItemCount() { return data.size(); }

    static class TrackVH extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView title, artist;
        TrackVH(@NonNull View v) {
            super(v);
            cover  = v.findViewById(R.id.imageCover);
            title  = v.findViewById(R.id.textTitle);
            artist = v.findViewById(R.id.textArtist);
        }
    }

    public void updateData(List<Track> newData) {
        data.clear();
        data.addAll(newData);
        notifyDataSetChanged();
    }

    public List<Track> getTracks() {
        return data;
    }
}
