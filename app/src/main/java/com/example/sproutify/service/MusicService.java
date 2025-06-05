package com.example.sproutify.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.example.sproutify.data.MusicPlayerState;
import com.example.sproutify.data.QueueManager;
import com.example.sproutify.model.Track;

import java.io.IOException;
import java.util.List;

/**
 * Service de lecture de musique qui gère la lecture des morceaux en arrière-plan.
 * Ce service implémente les fonctionnalités de lecture, pause, navigation et contrôle du volume.
 */
public class MusicService extends Service {
    private static final String TAG = "MusicService";
    private MediaPlayer mediaPlayer;
    private boolean isPrepared = false;
    private OnPlaybackStateChangeListener playbackListener;
    private int currentVolume = 50; // Volume par défaut à 50%

    /**
     * Interface pour notifier les changements d'état de la lecture.
     */
    public interface OnPlaybackStateChangeListener {
        /**
         * Appelé lorsque l'état de lecture change.
         * @param isPlaying true si la lecture est en cours, false sinon
         */
        void onPlaybackStateChanged(boolean isPlaying);

        /**
         * Appelé en cas d'erreur pendant la lecture.
         * @param errorMessage Message d'erreur détaillé
         */
        void onError(String errorMessage);

        /**
         * Appelé lorsque le morceau en cours change.
         * @param track Nouveau morceau en cours de lecture
         */
        void onTrackChanged(Track track);
    }

    private final IBinder binder = new MusicBinder();

    /**
     * Classe Binder pour permettre la liaison avec le service.
     */
    public class MusicBinder extends Binder {
        /**
         * Retourne l'instance du service.
         * @return Instance du MusicService
         */
        public MusicService getService() {
            return MusicService.this;
        }
    }

    /**
     * Initialise le service et configure le MediaPlayer.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: Initialisation du service de musique");
        setupMediaPlayer();
    }

    /**
     * Configure le MediaPlayer avec les listeners nécessaires.
     */
    private void setupMediaPlayer() {
        Log.d(TAG, "setupMediaPlayer: Début de la configuration");
        try {
            mediaPlayer = new MediaPlayer();
            Log.d(TAG, "setupMediaPlayer: MediaPlayer créé avec succès");
            
            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "onPrepared: MediaPlayer est prêt à jouer");
                isPrepared = true;
                try {
                    Log.d(TAG, "onPrepared: Démarrage de la lecture");
                    mediaPlayer.start();
                    Log.d(TAG, "onPrepared: Lecture démarrée avec succès");
                } catch (Exception e) {
                    Log.e(TAG, "onPrepared: Erreur lors du démarrage de la lecture", e);
                    if (playbackListener != null) {
                        playbackListener.onError("Erreur lors du démarrage de la lecture: " + e.getMessage());
                    }
                    return;
                }
                if (playbackListener != null) {
                    Log.d(TAG, "onPrepared: Notification du listener");
                    playbackListener.onPlaybackStateChanged(true);
                }
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "onCompletion: Lecture terminée");
                isPrepared = false;
                if (playbackListener != null) {
                    Log.d(TAG, "onCompletion: Notification du listener");
                    playbackListener.onPlaybackStateChanged(false);
                }
                playNext();
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "onError: Erreur de lecture - what: " + what + ", extra: " + extra);
                isPrepared = false;
                if (playbackListener != null) {
                    Log.d(TAG, "onError: Notification du listener");
                    playbackListener.onError("Erreur de lecture: " + what);
                }
                return false;
            });

            Log.d(TAG, "setupMediaPlayer: Configuration terminée avec succès");
        } catch (Exception e) {
            Log.e(TAG, "setupMediaPlayer: Erreur lors de la configuration", e);
        }
    }

    /**
     * Gère le démarrage du service.
     * @param intent Intent de démarrage
     * @param flags Flags de démarrage
     * @param startId ID de démarrage
     * @return Mode de démarrage du service
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: Service démarré");
        return START_NOT_STICKY;
    }

    /**
     * Gère la liaison avec le service.
     * @param intent Intent de liaison
     * @return IBinder pour la liaison
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: Service lié");
        return binder;
    }

    /**
     * Définit le listener pour les changements d'état de lecture.
     * @param listener Listener à définir
     */
    public void setPlaybackListener(OnPlaybackStateChangeListener listener) {
        this.playbackListener = listener;
    }

    /**
     * Joue un morceau spécifique.
     * @param track Morceau à jouer
     */
    public void playTrack(Track track) {
        if (track == null) {
            Log.e(TAG, "playTrack: Track is null");
            return;
        }

        Log.d(TAG, "playTrack: Début de la lecture de " + track.title);
        
        try {
            // Mettre à jour le MusicPlayerState
            MusicPlayerState.getInstance().setCurrentTrack(track);
            MusicPlayerState.getInstance().setPlaying(true);

            // Préparer le MediaPlayer
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, Uri.parse(track.mp3Url));
            mediaPlayer.prepareAsync();
            isPrepared = false;

            mediaPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                mp.start();
                if (playbackListener != null) {
                    playbackListener.onPlaybackStateChanged(true);
                    playbackListener.onTrackChanged(track);
                }
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                if (playbackListener != null) {
                    playbackListener.onPlaybackStateChanged(false);
                }
                playNext();
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                String errorMessage = "Erreur de lecture: " + what;
                Log.e(TAG, errorMessage);
                if (playbackListener != null) {
                    playbackListener.onError(errorMessage);
                }
                return false;
            });

        } catch (IOException e) {
            Log.e(TAG, "playTrack: Erreur lors de la préparation de la piste", e);
            if (playbackListener != null) {
                playbackListener.onError("Erreur lors de la préparation de la piste: " + e.getMessage());
            }
        }
    }

    /**
     * Réinitialise le MediaPlayer.
     */
    public void reset() {
        if (mediaPlayer != null) {
            Log.d(TAG, "reset: Début de la réinitialisation");
            try {
                mediaPlayer.reset();
                Log.d(TAG, "reset: MediaPlayer réinitialisé avec succès");
                isPrepared = false;
            } catch (Exception e) {
                Log.e(TAG, "reset: Erreur lors de la réinitialisation", e);
            }
        } else {
            Log.w(TAG, "reset: MediaPlayer est null");
        }
    }

    /**
     * Démarre la lecture.
     */
    public void start() {
        if (mediaPlayer != null && isPrepared) {
            try {
                mediaPlayer.start();
                Log.d(TAG, "start: MediaPlayer démarré");
                if (playbackListener != null) {
                    playbackListener.onPlaybackStateChanged(true);
                }
            } catch (Exception e) {
                Log.e(TAG, "start: Erreur de démarrage", e);
            }
        }
    }

    /**
     * Met la lecture en pause.
     */
    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            try {
                mediaPlayer.pause();
                Log.d(TAG, "pause: MediaPlayer mis en pause");
                if (playbackListener != null) {
                    playbackListener.onPlaybackStateChanged(false);
                }
            } catch (Exception e) {
                Log.e(TAG, "pause: Erreur lors de la mise en pause", e);
            }
        }
    }

    /**
     * Change la position de lecture.
     * @param position Nouvelle position en millisecondes
     */
    public void seekTo(int position) {
        if (mediaPlayer != null && isPrepared) {
            try {
                mediaPlayer.seekTo(position);
                Log.d(TAG, "seekTo: Position définie à " + position);
            } catch (Exception e) {
                Log.e(TAG, "seekTo: Erreur lors du changement de position", e);
            }
        }
    }

    /**
     * Vérifie si la lecture est en cours.
     * @return true si la lecture est en cours, false sinon
     */
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    /**
     * Bascule entre lecture et pause.
     */
    public void togglePlayPause() {
        if (isPlaying()) {
            Log.d(TAG, "togglePlayPause: Mise en pause");
            pause();
        } else {
            Log.d(TAG, "togglePlayPause: Démarrage");
            start();
        }
    }

    /**
     * Obtient la position actuelle de lecture.
     * @return Position actuelle en millisecondes
     */
    public int getCurrentPosition() {
        if (mediaPlayer != null && isPrepared) {
            try {
                return mediaPlayer.getCurrentPosition();
            } catch (Exception e) {
                Log.e(TAG, "getCurrentPosition: Erreur", e);
                return 0;
            }
        }
        return 0;
    }

    /**
     * Obtient la durée totale du morceau.
     * @return Durée en millisecondes
     */
    public int getDuration() {
        if (mediaPlayer != null && isPrepared) {
            try {
                return mediaPlayer.getDuration();
            } catch (Exception e) {
                Log.e(TAG, "getDuration: Erreur", e);
                return 0;
            }
        }
        return 0;
    }

    /**
     * Définit le volume de lecture.
     * @param volume Volume entre 0 et 100
     */
    public void setVolume(int volume) {
        if (mediaPlayer != null) {
            try {
                float volumeFloat = volume / 100f;
                mediaPlayer.setVolume(volumeFloat, volumeFloat);
                currentVolume = volume;
                Log.d(TAG, "setVolume: Volume défini à " + volume);
            } catch (Exception e) {
                Log.e(TAG, "setVolume: Erreur lors du changement de volume", e);
            }
        }
    }

    /**
     * Joue le morceau suivant dans la file d'attente.
     */
    public void playNext() {
        Track nextTrack = QueueManager.getInstance().getNextTrack();
        if (nextTrack != null) {
            playTrack(nextTrack);
        } else {
            Log.d(TAG, "playNext: Pas de morceau suivant disponible");
            if (playbackListener != null) {
                playbackListener.onPlaybackStateChanged(false);
            }
        }
    }

    /**
     * Joue le morceau précédent dans la file d'attente.
     */
    public void playPrevious() {
        Track previousTrack = QueueManager.getInstance().getPreviousTrack();
        if (previousTrack != null) {
            playTrack(previousTrack);
        } else {
            Log.d(TAG, "playPrevious: Pas de morceau précédent disponible");
            if (playbackListener != null) {
                playbackListener.onPlaybackStateChanged(false);
            }
        }
    }

    /**
     * Active ou désactive le mode aléatoire.
     * @param shuffle true pour activer, false pour désactiver
     */
    public void setShuffleMode(boolean shuffle) {
        QueueManager.getInstance().setShuffleMode(shuffle);
    }

    /**
     * Vérifie si le mode aléatoire est activé.
     * @return true si le mode aléatoire est activé
     */
    public boolean isShuffleMode() {
        return QueueManager.getInstance().isShuffleMode();
    }

    /**
     * Nettoie les ressources lors de la destruction du service.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}

