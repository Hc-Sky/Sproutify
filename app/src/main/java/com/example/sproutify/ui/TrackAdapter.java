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

public class TrackAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_TRACK = 0;
    public static final int VIEW_TYPE_ALBUM = 1;
    public static final int VIEW_TYPE_ARTIST = 2;
    public static final int VIEW_TYPE_TITLE = 3;

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
    private int viewType = VIEW_TYPE_TRACK;

    public TrackAdapter(Context ctx, List<Track> data, OnTrackClickListener clickListener,
                       OnTrackFavoriteListener favoriteListener) {
        this.ctx = ctx;
        this.data = data;
        this.clickListener = clickListener;
        this.favoriteListener = favoriteListener;
        this.favoritesManager = FavoritesManager.getInstance(ctx);
    }

    public void setViewType(int viewType) {
        this.viewType = viewType;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return viewType;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(ctx);
        View view;
        
        switch (viewType) {
            case VIEW_TYPE_ALBUM:
                view = inflater.inflate(R.layout.item_album, parent, false);
                return new AlbumViewHolder(view);
            case VIEW_TYPE_ARTIST:
                view = inflater.inflate(R.layout.item_artist, parent, false);
                return new ArtistViewHolder(view);
            case VIEW_TYPE_TITLE:
                view = inflater.inflate(R.layout.item_title, parent, false);
                return new TitleViewHolder(view);
            default:
                view = inflater.inflate(R.layout.item_track, parent, false);
                return new TrackViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Track track = data.get(position);

        if (holder instanceof TrackViewHolder) {
            bindTrackViewHolder((TrackViewHolder) holder, track, position);
        } else if (holder instanceof AlbumViewHolder) {
            bindAlbumViewHolder((AlbumViewHolder) holder, track, position);
        } else if (holder instanceof ArtistViewHolder) {
            bindArtistViewHolder((ArtistViewHolder) holder, track, position);
        } else if (holder instanceof TitleViewHolder) {
            bindTitleViewHolder((TitleViewHolder) holder, track, position);
        }
    }

    private void bindTrackViewHolder(TrackViewHolder h, Track track, int position) {
        h.title.setText(track.title);
        h.artist.setText(track.artist);

        Picasso.get()
                .load(track.coverUrl)
                .placeholder(R.drawable.ic_album_placeholder)
                .into(h.cover);

        updateFavoriteButton(h.favoriteButton, track);

        h.itemView.setOnClickListener(v -> clickListener.onTrackClick(track, position));

        h.favoriteButton.setOnClickListener(v -> {
            boolean isFavorite = favoritesManager.toggleFavorite(track);
            updateFavoriteButton(h.favoriteButton, track);
            favoriteListener.onTrackFavoriteChanged(track);
        });
    }

    private void bindAlbumViewHolder(AlbumViewHolder h, Track track, int position) {
        h.albumName.setText(track.album);

        Picasso.get()
                .load(track.coverUrl)
                .placeholder(R.drawable.ic_album_placeholder)
                .into(h.cover);

        h.itemView.setOnClickListener(v -> clickListener.onTrackClick(track, position));
    }

    private void bindArtistViewHolder(ArtistViewHolder h, Track track, int position) {
        h.artistName.setText(track.artist);

        Picasso.get()
                .load(track.coverUrl)
                .placeholder(R.drawable.ic_album_placeholder)
                .into(h.artistImage);

        h.itemView.setOnClickListener(v -> clickListener.onTrackClick(track, position));
    }

    private void bindTitleViewHolder(TitleViewHolder h, Track track, int position) {
        h.title.setText(track.title);
        h.artist.setText(track.artist);

        Picasso.get()
                .load(track.coverUrl)
                .placeholder(R.drawable.ic_album_placeholder)
                .into(h.cover);

        h.itemView.setOnClickListener(v -> clickListener.onTrackClick(track, position));
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

    static class TrackViewHolder extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView title, artist;
        ImageButton favoriteButton;

        TrackViewHolder(@NonNull View v) {
            super(v);
            cover = v.findViewById(R.id.imageCover);
            title = v.findViewById(R.id.textTitle);
            artist = v.findViewById(R.id.textArtist);
            favoriteButton = v.findViewById(R.id.btnFavorite);
        }
    }

    static class AlbumViewHolder extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView albumName;

        AlbumViewHolder(@NonNull View v) {
            super(v);
            cover = v.findViewById(R.id.imageCover);
            albumName = v.findViewById(R.id.textAlbumName);
        }
    }

    static class ArtistViewHolder extends RecyclerView.ViewHolder {
        ImageView artistImage;
        TextView artistName;

        ArtistViewHolder(@NonNull View v) {
            super(v);
            artistImage = v.findViewById(R.id.imageArtist);
            artistName = v.findViewById(R.id.textArtistName);
        }
    }

    static class TitleViewHolder extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView title, artist;

        TitleViewHolder(@NonNull View v) {
            super(v);
            cover = v.findViewById(R.id.imageCover);
            title = v.findViewById(R.id.textTitle);
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
