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

public class MusicService extends Service {
    private static final String TAG = "MusicService";
    private MediaPlayer mediaPlayer;
    private boolean isPrepared = false;
    private OnPlaybackStateChangeListener playbackListener;
    private int currentVolume = 50; // Volume par défaut à 50%

    public interface OnPlaybackStateChangeListener {
        void onPlaybackStateChanged(boolean isPlaying);
        void onError(String errorMessage);
        void onTrackChanged(Track track);
    }

    private final IBinder binder = new MusicBinder();

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: Initialisation du service de musique");
        setupMediaPlayer();
    }

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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: Service démarré");
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: Service lié");
        return binder;
    }

    public void setPlaybackListener(OnPlaybackStateChangeListener listener) {
        this.playbackListener = listener;
    }

    public void playTrack(Track track) {
        try {
            Log.d(TAG, "playTrack: Début de la méthode - Track: " + track.title);
            Log.d(TAG, "playTrack: URL de la piste: " + track.mp3Url);
            
            if (track.mp3Url == null || track.mp3Url.isEmpty()) {
                Log.e(TAG, "playTrack: URL de la piste est null ou vide");
                if (playbackListener != null) {
                    playbackListener.onError("URL de la piste invalide");
                }
                return;
            }

            reset();
            Log.d(TAG, "playTrack: MediaPlayer réinitialisé");

            try {
                Log.d(TAG, "playTrack: Tentative de configuration de la source audio");
                mediaPlayer.setDataSource(track.mp3Url);
                Log.d(TAG, "playTrack: Source audio configurée avec succès");
            } catch (IOException e) {
                Log.e(TAG, "playTrack: Erreur lors de la configuration de la source audio", e);
                if (playbackListener != null) {
                    playbackListener.onError("Erreur lors de la configuration de la source audio: " + e.getMessage());
                }
                return;
            }

            try {
                Log.d(TAG, "playTrack: Début de la préparation asynchrone");
                mediaPlayer.prepareAsync();
                Log.d(TAG, "playTrack: Préparation asynchrone lancée");
            } catch (Exception e) {
                Log.e(TAG, "playTrack: Erreur lors de la préparation", e);
                if (playbackListener != null) {
                    playbackListener.onError("Erreur lors de la préparation: " + e.getMessage());
                }
                return;
            }

        } catch (Exception e) {
            Log.e(TAG, "playTrack: Erreur générale lors de la lecture", e);
            if (playbackListener != null) {
                playbackListener.onError("Erreur générale: " + e.getMessage());
            }
        }
    }

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

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public void togglePlayPause() {
        if (isPlaying()) {
            Log.d(TAG, "togglePlayPause: Mise en pause");
            pause();
        } else {
            Log.d(TAG, "togglePlayPause: Démarrage");
            start();
        }
    }

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

    public void setVolume(int volume) {
        if (volume < 0) volume = 0;
        if (volume > 100) volume = 100;

        currentVolume = volume;

        if (mediaPlayer != null) {
            float volumeLevel = currentVolume / 100f;
            mediaPlayer.setVolume(volumeLevel, volumeLevel);
            Log.d(TAG, "setVolume: Volume réglé à " + volumeLevel);
        }
    }

    public void playNext() {
        Track nextTrack = QueueManager.getInstance().getNextTrack();
        if (nextTrack != null) {
            QueueManager.getInstance().moveToNext();
            playTrack(nextTrack);
        }
    }

    public void playPrevious() {
        Track previousTrack = QueueManager.getInstance().getPreviousTrack();
        if (previousTrack != null) {
            QueueManager.getInstance().moveToPrevious();
            playTrack(previousTrack);
        }
    }

    public void setShuffleMode(boolean shuffle) {
        QueueManager.getInstance().setShuffleMode(shuffle);
    }

    public boolean isShuffleMode() {
        return QueueManager.getInstance().isShuffleMode();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: Destruction du service de musique");
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }
}

