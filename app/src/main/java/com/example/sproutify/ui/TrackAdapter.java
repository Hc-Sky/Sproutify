package com.example.sproutify.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sproutify.R;
import com.example.sproutify.data.FavoritesManager;
import com.example.sproutify.model.Track;
import com.squareup.picasso.Picasso;

import java.util.List;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackVH> {

    public interface OnTrackClickListener {
        void onTrackClick(Track track, int position);
    }

    public interface OnTrackFavoriteListener {
        void onTrackFavoriteChanged(Track track);
    }

    private final List<Track> data;
    private final OnTrackClickListener clickListener;
    private final OnTrackFavoriteListener favoriteListener;
    private final Context ctx;
    private final FavoritesManager favoritesManager;

    public TrackAdapter(Context ctx, List<Track> data, OnTrackClickListener clickListener,
                       OnTrackFavoriteListener favoriteListener) {
        this.ctx = ctx;
        this.data = data;
        this.clickListener = clickListener;
        this.favoriteListener = favoriteListener;
        this.favoritesManager = FavoritesManager.getInstance(ctx);
    }

    @NonNull @Override
    public TrackVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_track, parent, false);
        return new TrackVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackVH h, int pos) {
        Track track = data.get(pos);

        h.title.setText(track.title);
        h.artist.setText(track.artist);

        // Chargement de la pochette avec Picasso
        Picasso.get()
                .load(track.coverUrl)
                .placeholder(R.drawable.ic_album_placeholder)
                .into(h.cover);

        // Mise à jour de l'état du bouton favori
        updateFavoriteButton(h.favoriteButton, track);

        // Configuration des listeners
        h.itemView.setOnClickListener(v -> clickListener.onTrackClick(track, pos));

        h.favoriteButton.setOnClickListener(v -> {
            boolean isFavorite = favoritesManager.toggleFavorite(track);
            updateFavoriteButton(h.favoriteButton, track);
            favoriteListener.onTrackFavoriteChanged(track);
        });
    }

    private void updateFavoriteButton(ImageButton button, Track track) {
        if (favoritesManager.isFavorite(track)) {
            button.setImageResource(R.drawable.ic_favorite_filled);
            button.setColorFilter(ctx.getResources().getColor(R.color.favorite_active, ctx.getTheme()));
        } else {
            button.setImageResource(R.drawable.ic_favorite_border);
            button.setColorFilter(ctx.getResources().getColor(R.color.favorite_button_tint, ctx.getTheme()));
        }
    }

    @Override public int getItemCount() { return data.size(); }

    static class TrackVH extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView title, artist;
        ImageButton favoriteButton;

        TrackVH(@NonNull View v) {
            super(v);
            cover = v.findViewById(R.id.imageCover);
            title = v.findViewById(R.id.textTitle);
            artist = v.findViewById(R.id.textArtist);
            favoriteButton = v.findViewById(R.id.btnFavorite);
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
