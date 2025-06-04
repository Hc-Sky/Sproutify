package com.example.sproutify.data;

import com.example.sproutify.model.Track;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import android.util.Log;

public class QueueManager {
    private static QueueManager instance;
    private List<Track> queue;
    private List<Track> baseList;
    private int currentIndex;
    private boolean isShuffleMode;
    private Random random;
    private int baseListIndex; // Index pour la lecture séquentielle de la liste de base

    private QueueManager() {
        queue = new ArrayList<>();
        baseList = new ArrayList<>();
        currentIndex = -1;
        baseListIndex = 0;
        isShuffleMode = false;
        random = new Random();
    }

    public static synchronized QueueManager getInstance() {
        if (instance == null) {
            instance = new QueueManager();
        }
        return instance;
    }

    public void setBaseList(List<Track> tracks) {
        baseList = new ArrayList<>(tracks);
        baseListIndex = 0;
        
        // Initialiser la file d'attente avec les 10 premiers morceaux
        queue.clear();
        int tracksToAdd = Math.min(10, baseList.size());
        for (int i = 0; i < tracksToAdd; i++) {
            queue.add(baseList.get(i));
        }
        currentIndex = 0; // Commencer à la première piste
    }

    public void setQueue(List<Track> tracks, int startIndex) {
        queue = new ArrayList<>(tracks);
        currentIndex = startIndex;
    }

    public void updateQueueForNewTrack(Track newTrack) {
        Log.d("QueueManager", "updateQueueForNewTrack: Début de la méthode - Track: " + newTrack.title);
        
        if (queue.isEmpty()) {
            Log.d("QueueManager", "updateQueueForNewTrack: File vide, réinitialisation avec les 10 premiers morceaux");
            setBaseList(baseList);
            return;
        }

        // Trouver l'index du nouveau morceau dans la file
        int newIndex = -1;
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).equals(newTrack)) {
                newIndex = i;
                break;
            }
        }

        if (newIndex != -1) {
            Log.d("QueueManager", "updateQueueForNewTrack: Morceau trouvé dans la file à l'index " + newIndex);
            currentIndex = newIndex;
        } else {
            Log.d("QueueManager", "updateQueueForNewTrack: Morceau non trouvé dans la file, ajout au début");
            queue.add(0, newTrack);
            currentIndex = 0;
        }
    }

    /**
     * Ajoute une piste à la fin de la file d'attente
     * @param track La piste à ajouter
     */
    public void addToQueue(Track track) {
        queue.add(track);
    }

    /**
     * Ajoute une piste après la piste en cours de lecture
     * @param track La piste à ajouter
     */
    public void addAfterCurrent(Track track) {
        if (currentIndex >= 0 && currentIndex < queue.size()) {
            queue.add(currentIndex + 1, track);
        } else {
            queue.add(track);
        }
    }

    /**
     * Ajoute une liste de pistes à la fin de la file d'attente
     * @param tracks La liste des pistes à ajouter
     */
    public void addTracksToQueue(List<Track> tracks) {
        queue.addAll(tracks);
    }

    /**
     * Ajoute une liste de pistes après la piste en cours de lecture
     * @param tracks La liste des pistes à ajouter
     */
    public void addTracksAfterCurrent(List<Track> tracks) {
        if (currentIndex >= 0 && currentIndex < queue.size()) {
            queue.addAll(currentIndex + 1, tracks);
        } else {
            queue.addAll(tracks);
        }
    }

    public void removeFromQueue(int position) {
        if (position >= 0 && position < queue.size()) {
            queue.remove(position);
            if (position < currentIndex) {
                currentIndex--;
            }
        }
    }

    public void moveTrack(int fromPosition, int toPosition) {
        if (fromPosition >= 0 && fromPosition < queue.size() &&
            toPosition >= 0 && toPosition < queue.size()) {
            Track track = queue.remove(fromPosition);
            queue.add(toPosition, track);
            
            if (fromPosition == currentIndex) {
                currentIndex = toPosition;
            } else if (fromPosition < currentIndex && toPosition >= currentIndex) {
                currentIndex--;
            } else if (fromPosition > currentIndex && toPosition <= currentIndex) {
                currentIndex++;
            }
        }
    }

    public Track getCurrentTrack() {
        if (currentIndex >= 0 && currentIndex < queue.size()) {
            return queue.get(currentIndex);
        }
        return null;
    }

    public Track getNextTrack() {
        Log.d("QueueManager", "getNextTrack: Début de la méthode");
        Log.d("QueueManager", "getNextTrack: Taille de la file: " + queue.size() + ", Index actuel: " + currentIndex);
        
        // Si la file d'attente n'est pas vide, prendre la piste suivante
        if (!queue.isEmpty() && currentIndex + 1 < queue.size()) {
            Track nextTrack = queue.get(currentIndex + 1);
            Log.d("QueueManager", "getNextTrack: Piste suivante trouvée dans la file - " + nextTrack.title);
            return nextTrack;
        }
        
        // Si la file d'attente est vide, prendre une piste de la liste de base
        if (!baseList.isEmpty()) {
            if (isShuffleMode) {
                Track nextTrack = baseList.get(random.nextInt(baseList.size()));
                Log.d("QueueManager", "getNextTrack: Piste aléatoire trouvée dans la liste de base - " + nextTrack.title);
                return nextTrack;
            } else {
                // Mode séquentiel : prendre la piste suivante de la liste de base
                Track nextTrack = baseList.get(baseListIndex);
                Log.d("QueueManager", "getNextTrack: Piste séquentielle trouvée dans la liste de base - " + nextTrack.title);
                baseListIndex = (baseListIndex + 1) % baseList.size();
                return nextTrack;
            }
        }
        
        Log.d("QueueManager", "getNextTrack: Aucune piste suivante disponible");
        return null;
    }

    public Track getPreviousTrack() {
        // Si on est dans la file d'attente
        if (!queue.isEmpty() && currentIndex - 1 >= 0) {
            return queue.get(currentIndex - 1);
        }
        
        // Si on est dans la liste de base et pas en mode aléatoire
        if (!baseList.isEmpty() && !isShuffleMode) {
            baseListIndex = (baseListIndex - 1 + baseList.size()) % baseList.size();
            return baseList.get(baseListIndex);
        }
        
        return null;
    }

    public void moveToNext() {
        Log.d("QueueManager", "moveToNext: Début de la méthode");
        Log.d("QueueManager", "moveToNext: Index actuel: " + currentIndex + ", Taille de la file: " + queue.size());
        
        if (currentIndex + 1 < queue.size()) {
            currentIndex++;
            Log.d("QueueManager", "moveToNext: Index mis à jour à " + currentIndex);
        } else if (!queue.isEmpty()) {
            // Si on est à la fin de la file d'attente, la vider
            Log.d("QueueManager", "moveToNext: Fin de la file atteinte, vidage de la file");
            queue.clear();
            currentIndex = -1;
        }
    }

    public void moveToPrevious() {
        if (currentIndex - 1 >= 0) {
            currentIndex--;
        }
    }

    public List<Track> getQueue() {
        return new ArrayList<>(queue);
    }

    public List<Track> getBaseList() {
        return new ArrayList<>(baseList);
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void clearQueue() {
        queue.clear();
        currentIndex = -1;
    }

    public void setShuffleMode(boolean shuffle) {
        isShuffleMode = shuffle;
    }

    public boolean isShuffleMode() {
        return isShuffleMode;
    }

    public boolean hasQueue() {
        return !queue.isEmpty();
    }
} 