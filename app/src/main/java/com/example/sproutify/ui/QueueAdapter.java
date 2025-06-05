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

/**
 * Adaptateur pour afficher la file d'attente de lecture dans un RecyclerView.
 * Gère l'affichage des morceaux en attente et permet leur réorganisation par glisser-déposer.
 */
public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueViewHolder> {
    private List<Track> queue;
    private int currentIndex;
    private OnStartDragListener dragListener;

    /**
     * Interface pour gérer le début du glisser-déposer d'un élément.
     */
    public interface OnStartDragListener {
        /**
         * Appelé lorsqu'un élément commence à être déplacé.
         * @param viewHolder Le ViewHolder de l'élément en cours de déplacement
         */
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    /**
     * Constructeur de l'adaptateur.
     * 
     * @param queue Liste des morceaux dans la file d'attente
     * @param currentIndex Index du morceau actuellement en lecture
     * @param dragListener Listener pour gérer le glisser-déposer
     */
    public QueueAdapter(List<Track> queue, int currentIndex, OnStartDragListener dragListener) {
        this.queue = queue;
        this.currentIndex = currentIndex;
        this.dragListener = dragListener;
    }

    /**
     * Crée un nouveau ViewHolder pour un élément de la file d'attente.
     * 
     * @param parent Le ViewGroup parent
     * @param viewType Le type de vue
     * @return Un nouveau QueueViewHolder
     */
    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.queue_item, parent, false);
        return new QueueViewHolder(view);
    }

    /**
     * Lie les données d'un morceau à son ViewHolder.
     * Configure l'affichage du titre, de l'artiste, de la pochette et gère le glisser-déposer.
     * 
     * @param holder Le ViewHolder à configurer
     * @param position La position de l'élément dans la liste
     */
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

    /**
     * Retourne le nombre total d'éléments dans la file d'attente.
     * 
     * @return Le nombre d'éléments
     */
    @Override
    public int getItemCount() {
        return queue.size();
    }

    /**
     * Met à jour la file d'attente avec une nouvelle liste de morceaux.
     * 
     * @param newQueue Nouvelle liste de morceaux
     * @param newCurrentIndex Nouvel index du morceau en cours
     */
    public void updateQueue(List<Track> newQueue, int newCurrentIndex) {
        this.queue = newQueue;
        this.currentIndex = newCurrentIndex;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder pour les éléments de la file d'attente.
     * Contient les vues nécessaires pour afficher un morceau.
     */
    static class QueueViewHolder extends RecyclerView.ViewHolder {
        /** Image de la pochette du morceau */
        ImageView coverImageView;
        /** Texte du titre du morceau */
        TextView titleTextView;
        /** Texte du nom de l'artiste */
        TextView artistTextView;
        /** Icône pour le glisser-déposer */
        ImageView dragHandle;

        /**
         * Constructeur du ViewHolder.
         * Initialise les vues à partir du layout.
         * 
         * @param itemView La vue de l'élément
         */
        QueueViewHolder(View itemView) {
            super(itemView);
            coverImageView = itemView.findViewById(R.id.queueItemCover);
            titleTextView = itemView.findViewById(R.id.queueItemTitle);
            artistTextView = itemView.findViewById(R.id.queueItemArtist);
            dragHandle = itemView.findViewById(R.id.queueItemDrag);
        }
    }
} 