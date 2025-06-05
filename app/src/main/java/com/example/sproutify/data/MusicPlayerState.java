package com.example.sproutify.data;

import com.example.sproutify.model.Track;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

/**
 * Classe singleton qui gère l'état global du lecteur audio
 * Maintient la synchronisation de l'état entre les différentes parties de l'application
 */
public class MusicPlayerState extends Observable {
    private static MusicPlayerState instance;

    private Track currentTrack;
    private boolean isPlaying;
    private List<Track> trackList = new ArrayList<>();
    private int currentTrackPosition = 0;
    private List<Track> favorites = new ArrayList<>();

    /**
     * Constructeur privé pour le pattern Singleton
     * Initialise l'état initial du lecteur
     */
    private MusicPlayerState() {
        // Constructeur privé pour le singleton
        currentTrack = null;
        isPlaying = false;
    }

    /**
     * Obtient l'instance unique du MusicPlayerState
     * @return Instance unique du MusicPlayerState
     */
    public static synchronized MusicPlayerState getInstance() {
        if (instance == null) {
            instance = new MusicPlayerState();
        }
        return instance;
    }

    /**
     * Récupère la piste actuellement en lecture
     * @return Piste en cours ou null si aucune
     */
    public Track getCurrentTrack() {
        return currentTrack;
    }

    /**
     * Définit la piste en cours de lecture
     * Met à jour la position dans la liste si la piste existe
     * @param track Nouvelle piste en cours
     */
    public void setCurrentTrack(Track track) {
        this.currentTrack = track;

        // Mettre à jour la position si la piste existe dans la liste
        if (track != null && trackList != null && !trackList.isEmpty()) {
            for (int i = 0; i < trackList.size(); i++) {
                Track listTrack = trackList.get(i);
                if (listTrack.title.equals(track.title) &&
                    listTrack.artist.equals(track.artist)) {
                    currentTrackPosition = i;
                    break;
                }
            }
        }
    }

    /**
     * Vérifie si une lecture est en cours
     * @return true si une lecture est active
     */
    public boolean isPlaying() {
        return isPlaying;
    }

    /**
     * Définit l'état de lecture
     * @param playing true pour indiquer une lecture en cours
     */
    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    /**
     * Vérifie si une piste est actuellement sélectionnée
     * @return true si une piste est en cours
     */
    public boolean hasTrack() {
        return currentTrack != null;
    }

    /**
     * Récupère la liste complète des pistes
     * @return Liste des pistes disponibles
     */
    public List<Track> getTrackList() {
        return trackList;
    }

    /**
     * Définit la liste complète des pistes
     * @param tracks Nouvelle liste de pistes
     */
    public void setTrackList(List<Track> tracks) {
        if (tracks != null) {
            this.trackList = new ArrayList<>(tracks);
        }
    }

    /**
     * Récupère la position actuelle dans la liste
     * @return Index de la piste en cours
     */
    public int getCurrentTrackPosition() {
        return currentTrackPosition;
    }

    /**
     * Définit la position actuelle dans la liste
     * Met à jour la piste en cours en conséquence
     * @param position Nouvelle position dans la liste
     */
    public void setCurrentTrackPosition(int position) {
        if (position >= 0 && trackList != null && position < trackList.size()) {
            this.currentTrackPosition = position;
            this.currentTrack = trackList.get(position);
        }
    }

    /**
     * Ajoute une piste aux favoris
     * Notifie les observateurs du changement
     * @param track Piste à ajouter aux favoris
     */
    public void addToFavorites(Track track) {
        if (!favorites.contains(track)) {
            favorites.add(track);
            notifyFavoritesChanged();
        }
    }

    /**
     * Retire une piste des favoris
     * Notifie les observateurs du changement
     * @param track Piste à retirer des favoris
     */
    public void removeFromFavorites(Track track) {
        if (favorites.remove(track)) {
            notifyFavoritesChanged();
        }
    }

    /**
     * Vérifie si une piste est dans les favoris
     * @param track Piste à vérifier
     * @return true si la piste est en favoris
     */
    public boolean isFavorite(Track track) {
        return favorites.contains(track);
    }

    /**
     * Récupère la liste des favoris
     * @return Copie de la liste des pistes favorites
     */
    public List<Track> getFavorites() {
        return new ArrayList<>(favorites);
    }

    /**
     * Notifie les observateurs d'un changement dans les favoris
     * Utilise le pattern Observer pour la synchronisation
     */
    private void notifyFavoritesChanged() {
        // Notifier les observateurs du changement
        setChanged();
        notifyObservers();
    }
}
