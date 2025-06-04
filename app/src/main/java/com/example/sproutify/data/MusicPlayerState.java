package com.example.sproutify.data;

import com.example.sproutify.model.Track;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

/**
 * Classe singleton qui gère l'état du lecteur audio à travers l'application
 */
public class MusicPlayerState extends Observable {
    private static MusicPlayerState instance;

    private Track currentTrack;
    private boolean isPlaying;
    private List<Track> trackList = new ArrayList<>();
    private int currentTrackPosition = 0;
    private List<Track> favorites = new ArrayList<>();

    private MusicPlayerState() {
        // Constructeur privé pour le singleton
        currentTrack = null;
        isPlaying = false;
    }

    public static synchronized MusicPlayerState getInstance() {
        if (instance == null) {
            instance = new MusicPlayerState();
        }
        return instance;
    }

    public Track getCurrentTrack() {
        return currentTrack;
    }

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

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    public boolean hasTrack() {
        return currentTrack != null;
    }

    public List<Track> getTrackList() {
        return trackList;
    }

    public void setTrackList(List<Track> tracks) {
        if (tracks != null) {
            this.trackList = new ArrayList<>(tracks);
        }
    }

    public int getCurrentTrackPosition() {
        return currentTrackPosition;
    }

    public void setCurrentTrackPosition(int position) {
        if (position >= 0 && trackList != null && position < trackList.size()) {
            this.currentTrackPosition = position;
            this.currentTrack = trackList.get(position);
        }
    }

    public void addToFavorites(Track track) {
        if (!favorites.contains(track)) {
            favorites.add(track);
            notifyFavoritesChanged();
        }
    }

    public void removeFromFavorites(Track track) {
        if (favorites.remove(track)) {
            notifyFavoritesChanged();
        }
    }

    public boolean isFavorite(Track track) {
        return favorites.contains(track);
    }

    public List<Track> getFavorites() {
        return new ArrayList<>(favorites);
    }

    private void notifyFavoritesChanged() {
        // Notifier les observateurs du changement
        setChanged();
        notifyObservers();
    }
}
