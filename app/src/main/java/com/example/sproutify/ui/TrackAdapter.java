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

/**
 * Adaptateur pour afficher les morceaux dans un RecyclerView avec différents types de vues.
 * Supporte l'affichage des morceaux sous forme de liste, d'albums, d'artistes ou de titres.
 */
public class TrackAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /** Type de vue pour l'affichage standard d'un morceau */
    public static final int VIEW_TYPE_TRACK = 0;
    /** Type de vue pour l'affichage en mode album */
    public static final int VIEW_TYPE_ALBUM = 1;
    /** Type de vue pour l'affichage en mode artiste */
    public static final int VIEW_TYPE_ARTIST = 2;
    /** Type de vue pour l'affichage en mode titre */
    public static final int VIEW_TYPE_TITLE = 3;

    /**
     * Interface pour gérer les clics sur un morceau.
     */
    public interface OnTrackClickListener {
        /**
         * Appelé lorsqu'un morceau est cliqué.
         * @param track Le morceau cliqué
         * @param position La position du morceau dans la liste
         */
        void onTrackClick(Track track, int position);
    }

    /**
     * Interface pour gérer les changements d'état des favoris.
     */
    public interface OnTrackFavoriteListener {
        /**
         * Appelé lorsque l'état des favoris d'un morceau change.
         * @param track Le morceau modifié
         */
        void onTrackFavoriteChanged(Track track);
    }

    private final List<Track> data;
    private final OnTrackClickListener clickListener;
    private final OnTrackFavoriteListener favoriteListener;
    private final Context ctx;
    private final FavoritesManager favoritesManager;
    private int viewType = VIEW_TYPE_TRACK;

    /**
     * Constructeur de l'adaptateur.
     * 
     * @param ctx Le contexte de l'application
     * @param data La liste des morceaux à afficher
     * @param clickListener Listener pour les clics sur les morceaux
     * @param favoriteListener Listener pour les changements d'état des favoris
     */
    public TrackAdapter(Context ctx, List<Track> data, OnTrackClickListener clickListener,
                       OnTrackFavoriteListener favoriteListener) {
        this.ctx = ctx;
        this.data = data;
        this.clickListener = clickListener;
        this.favoriteListener = favoriteListener;
        this.favoritesManager = FavoritesManager.getInstance(ctx);
    }

    /**
     * Définit le type de vue à utiliser pour l'affichage.
     * 
     * @param viewType Le type de vue (TRACK, ALBUM, ARTIST ou TITLE)
     */
    public void setViewType(int viewType) {
        this.viewType = viewType;
        notifyDataSetChanged();
    }

    /**
     * Retourne le type de vue pour une position donnée.
     * 
     * @param position La position dans la liste
     * @return Le type de vue à utiliser
     */
    @Override
    public int getItemViewType(int position) {
        return viewType;
    }

    /**
     * Crée un nouveau ViewHolder en fonction du type de vue.
     * 
     * @param parent Le ViewGroup parent
     * @param viewType Le type de vue à créer
     * @return Un nouveau ViewHolder approprié
     */
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

    /**
     * Lie les données d'un morceau à son ViewHolder.
     * 
     * @param holder Le ViewHolder à configurer
     * @param position La position de l'élément dans la liste
     */
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

    /**
     * Configure un ViewHolder pour l'affichage standard d'un morceau.
     * 
     * @param h Le ViewHolder à configurer
     * @param track Le morceau à afficher
     * @param position La position dans la liste
     */
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

    /**
     * Configure un ViewHolder pour l'affichage en mode album.
     * 
     * @param h Le ViewHolder à configurer
     * @param track Le morceau à afficher
     * @param position La position dans la liste
     */
    private void bindAlbumViewHolder(AlbumViewHolder h, Track track, int position) {
        h.albumName.setText(track.album);

        Picasso.get()
                .load(track.coverUrl)
                .placeholder(R.drawable.ic_album_placeholder)
                .into(h.cover);

        h.itemView.setOnClickListener(v -> clickListener.onTrackClick(track, position));
    }

    /**
     * Configure un ViewHolder pour l'affichage en mode artiste.
     * 
     * @param h Le ViewHolder à configurer
     * @param track Le morceau à afficher
     * @param position La position dans la liste
     */
    private void bindArtistViewHolder(ArtistViewHolder h, Track track, int position) {
        h.artistName.setText(track.artist);

        Picasso.get()
                .load(track.coverUrl)
                .placeholder(R.drawable.ic_album_placeholder)
                .into(h.artistImage);

        h.itemView.setOnClickListener(v -> clickListener.onTrackClick(track, position));
    }

    /**
     * Configure un ViewHolder pour l'affichage en mode titre.
     * 
     * @param h Le ViewHolder à configurer
     * @param track Le morceau à afficher
     * @param position La position dans la liste
     */
    private void bindTitleViewHolder(TitleViewHolder h, Track track, int position) {
        h.title.setText(track.title);
        h.artist.setText(track.artist);

        Picasso.get()
                .load(track.coverUrl)
                .placeholder(R.drawable.ic_album_placeholder)
                .into(h.cover);

        h.itemView.setOnClickListener(v -> clickListener.onTrackClick(track, position));
    }

    /**
     * Met à jour l'apparence du bouton favori en fonction de l'état du morceau.
     * 
     * @param button Le bouton à mettre à jour
     * @param track Le morceau concerné
     */
    private void updateFavoriteButton(ImageButton button, Track track) {
        if (favoritesManager.isFavorite(track)) {
            button.setImageResource(R.drawable.ic_favorite_filled);
            button.setColorFilter(ctx.getResources().getColor(R.color.favorite_active, ctx.getTheme()));
        } else {
            button.setImageResource(R.drawable.ic_favorite_border);
            button.setColorFilter(ctx.getResources().getColor(R.color.favorite_button_tint, ctx.getTheme()));
        }
    }

    /**
     * Retourne le nombre total d'éléments dans la liste.
     * 
     * @return Le nombre d'éléments
     */
    @Override public int getItemCount() { return data.size(); }

    /**
     * ViewHolder pour l'affichage standard d'un morceau.
     */
    static class TrackViewHolder extends RecyclerView.ViewHolder {
        /** Image de la pochette */
        ImageView cover;
        /** Texte du titre */
        TextView title;
        /** Texte de l'artiste */
        TextView artist;
        /** Bouton favori */
        ImageButton favoriteButton;

        /**
         * Constructeur du ViewHolder.
         * 
         * @param v La vue de l'élément
         */
        TrackViewHolder(@NonNull View v) {
            super(v);
            cover = v.findViewById(R.id.imageCover);
            title = v.findViewById(R.id.textTitle);
            artist = v.findViewById(R.id.textArtist);
            favoriteButton = v.findViewById(R.id.btnFavorite);
        }
    }

    /**
     * ViewHolder pour l'affichage en mode album.
     */
    static class AlbumViewHolder extends RecyclerView.ViewHolder {
        /** Image de la pochette */
        ImageView cover;
        /** Texte du nom de l'album */
        TextView albumName;

        /**
         * Constructeur du ViewHolder.
         * 
         * @param v La vue de l'élément
         */
        AlbumViewHolder(@NonNull View v) {
            super(v);
            cover = v.findViewById(R.id.imageCover);
            albumName = v.findViewById(R.id.textAlbumName);
        }
    }

    /**
     * ViewHolder pour l'affichage en mode artiste.
     */
    static class ArtistViewHolder extends RecyclerView.ViewHolder {
        /** Image de l'artiste */
        ImageView artistImage;
        /** Texte du nom de l'artiste */
        TextView artistName;

        /**
         * Constructeur du ViewHolder.
         * 
         * @param v La vue de l'élément
         */
        ArtistViewHolder(@NonNull View v) {
            super(v);
            artistImage = v.findViewById(R.id.imageArtist);
            artistName = v.findViewById(R.id.textArtistName);
        }
    }

    /**
     * ViewHolder pour l'affichage en mode titre.
     */
    static class TitleViewHolder extends RecyclerView.ViewHolder {
        /** Image de la pochette */
        ImageView cover;
        /** Texte du titre */
        TextView title;
        /** Texte de l'artiste */
        TextView artist;

        /**
         * Constructeur du ViewHolder.
         * 
         * @param v La vue de l'élément
         */
        TitleViewHolder(@NonNull View v) {
            super(v);
            cover = v.findViewById(R.id.imageCover);
            title = v.findViewById(R.id.textTitle);
            artist = v.findViewById(R.id.textArtist);
        }
    }

    /**
     * Met à jour la liste des morceaux avec de nouvelles données.
     * 
     * @param newData Nouvelle liste de morceaux
     */
    public void updateData(List<Track> newData) {
        data.clear();
        data.addAll(newData);
        notifyDataSetChanged();
    }

    /**
     * Retourne la liste actuelle des morceaux.
     * 
     * @return La liste des morceaux
     */
    public List<Track> getTracks() {
        return data;
    }
}
