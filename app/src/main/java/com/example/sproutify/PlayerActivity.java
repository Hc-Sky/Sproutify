package com.example.sproutify;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sproutify.model.Track;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PlayerActivity extends AppCompatActivity {

    private static final String TAG = "PlayerActivity";
    public static final String EXTRA_TRACK = "track";
    public static final String EXTRA_TRACK_LIST = "track_list";
    public static final String EXTRA_TRACK_POSITION = "track_position";

    private Track currentTrack;
    private List<Track> trackList = new ArrayList<>();
    private int currentTrackPosition = 0;
    private MediaPlayer mediaPlayer;
    private Handler handler;
    private Runnable updateSeekBar;
    private boolean isLoading = false;

    // UI Components
    private ImageView coverImageView;
    private TextView titleTextView;
    private TextView artistTextView;
    private SeekBar seekBar;
    private TextView currentTimeTextView;
    private TextView totalTimeTextView;
    private ImageButton playPauseButton;
    private ImageButton prevButton;
    private ImageButton nextButton;
    private MaterialToolbar toolbar;

    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // Initialisation des composants UI
        coverImageView = findViewById(R.id.playerCoverImage);
        titleTextView = findViewById(R.id.playerTrackTitle);
        artistTextView = findViewById(R.id.playerArtistName);
        seekBar = findViewById(R.id.playerSeekBar);
        currentTimeTextView = findViewById(R.id.playerCurrentTime);
        totalTimeTextView = findViewById(R.id.playerTotalTime);
        playPauseButton = findViewById(R.id.playerPlayPauseButton);
        prevButton = findViewById(R.id.playerPrevButton);
        nextButton = findViewById(R.id.playerNextButton);
        toolbar = findViewById(R.id.playerToolbar);

        // Configuration de la toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Récupération du morceau et de la liste des morceaux depuis l'intent
        if (getIntent().hasExtra(EXTRA_TRACK)) {
            currentTrack = getIntent().getParcelableExtra(EXTRA_TRACK);

            // Récupération de la liste complète des morceaux si disponible
            if (getIntent().hasExtra(EXTRA_TRACK_LIST)) {
                trackList = getIntent().getParcelableArrayListExtra(EXTRA_TRACK_LIST);
                currentTrackPosition = getIntent().getIntExtra(EXTRA_TRACK_POSITION, 0);
            } else {
                // Si on n'a qu'un morceau unique, créer une liste avec ce seul morceau
                trackList = new ArrayList<>();
                trackList.add(currentTrack);
                currentTrackPosition = 0;
            }

            setupPlayerWithTrack(currentTrack);
        } else {
            finish(); // Terminer l'activité si aucun morceau n'est fourni
        }

        // Initialisation du handler pour mettre à jour la seekbar
        handler = new Handler();

        // Configuration des listeners
        setupListeners();
    }

    private void setupPlayerWithTrack(Track track) {
        // Marquer comme en cours de chargement
        isLoading = true;

        // Affichage des informations du morceau
        titleTextView.setText(track.title);
        artistTextView.setText(track.artist);

        // Chargement de la pochette avec AsyncTask
        new LoadImageTask(coverImageView).execute(track.coverUrl);

        // Afficher un message de chargement
        currentTimeTextView.setText("Chargement...");
        totalTimeTextView.setText("--:--");

        // Désactiver tous les boutons pendant le chargement
        playPauseButton.setEnabled(false);
        prevButton.setEnabled(false);
        nextButton.setEnabled(false);
        seekBar.setEnabled(false);
        seekBar.setProgress(0);

        // Libérer les ressources MediaPlayer précédentes
        releaseMediaPlayer();

        // Initialisation d'un nouveau MediaPlayer
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(track.mp3Url);

            mediaPlayer.setOnPreparedListener(mp -> {
                if (isFinishing()) return;  // Éviter les crash si l'activité est détruite

                // Initialisation de la seekbar
                seekBar.setMax(mediaPlayer.getDuration());
                totalTimeTextView.setText(formatTime(mediaPlayer.getDuration()));
                currentTimeTextView.setText("0:00");
                seekBar.setEnabled(true);

                // Activer les boutons selon la position dans la liste
                playPauseButton.setEnabled(true);
                prevButton.setEnabled(currentTrackPosition > 0);
                nextButton.setEnabled(currentTrackPosition < trackList.size() - 1);

                // Marquer comme chargé
                isLoading = false;

                // Lancement automatique de la lecture
                togglePlayPause();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                playPauseButton.setImageResource(R.drawable.ic_play);
                isPlaying = false;
                seekBar.setProgress(0);
                currentTimeTextView.setText("0:00");

                // Passer automatiquement à la piste suivante si disponible
                if (currentTrackPosition < trackList.size() - 1) {
                    playNextTrack();
                }
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);

                // Gérer les erreurs de lecture
                if (isFinishing()) return true;  // Ignore les erreurs si l'activité est détruite

                // Afficher message d'erreur
                playPauseButton.setEnabled(false);
                currentTimeTextView.setText("Erreur de lecture");
                Toast.makeText(PlayerActivity.this,
                               "Erreur lors de la lecture de " + track.title,
                               Toast.LENGTH_SHORT).show();

                // Marquer comme chargé pour éviter les blocs
                isLoading = false;

                return true;
            });

            // Préparer le MediaPlayer de manière asynchrone
            mediaPlayer.prepareAsync();

        } catch (IOException e) {
            Log.e(TAG, "Error setting data source", e);
            e.printStackTrace();
            currentTimeTextView.setText("Erreur: URL invalide");
            isLoading = false;
        }
    }

    private void setupListeners() {
        // Listener pour le bouton play/pause
        playPauseButton.setOnClickListener(v -> togglePlayPause());

        // Listener pour la seekbar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                    currentTimeTextView.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Pas besoin d'implémentation
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Pas besoin d'implémentation
            }
        });

        // Gestion du bouton précédent
        prevButton.setOnClickListener(v -> playPreviousTrack());

        // Gestion du bouton suivant
        nextButton.setOnClickListener(v -> playNextTrack());
    }

    private void playPreviousTrack() {
        // Éviter les actions multiples pendant le chargement
        if (isLoading) return;

        if (currentTrackPosition > 0) {
            currentTrackPosition--;
            currentTrack = trackList.get(currentTrackPosition);
            setupPlayerWithTrack(currentTrack);
        } else {
            // Si c'est la première piste, retour au début de la piste
            if (mediaPlayer != null) {
                mediaPlayer.seekTo(0);
                seekBar.setProgress(0);
                currentTimeTextView.setText("0:00");
            }
        }
    }

    private void playNextTrack() {
        // Éviter les actions multiples pendant le chargement
        if (isLoading) return;

        if (currentTrackPosition < trackList.size() - 1) {
            currentTrackPosition++;
            currentTrack = trackList.get(currentTrackPosition);
            setupPlayerWithTrack(currentTrack);
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer != null && !isLoading) {
            if (isPlaying) {
                // Pause
                mediaPlayer.pause();
                playPauseButton.setImageResource(R.drawable.ic_play);
                handler.removeCallbacks(updateSeekBar);
            } else {
                // Play
                mediaPlayer.start();
                playPauseButton.setImageResource(R.drawable.ic_pause);
                updateSeekbar();
            }
            isPlaying = !isPlaying;
        }
    }

    private void updateSeekbar() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            try {
                int currentPosition = mediaPlayer.getCurrentPosition();
                seekBar.setProgress(currentPosition);
                currentTimeTextView.setText(formatTime(currentPosition));

                // Mise à jour toutes les 100ms
                updateSeekBar = () -> updateSeekbar();
                handler.postDelayed(updateSeekBar, 100);
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error updating seekbar", e);
            }
        }
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
                isPlaying = false;
                handler.removeCallbacks(updateSeekBar);
            } catch (Exception e) {
                Log.e(TAG, "Error releasing MediaPlayer", e);
            }
        }
    }

    private String formatTime(int timeInMs) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMs);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMs) -
                TimeUnit.MINUTES.toSeconds(minutes);
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && isPlaying) {
            // Pause automatique quand l'activité est mise en pause
            mediaPlayer.pause();
            handler.removeCallbacks(updateSeekBar);
            // Ne pas changer l'état de isPlaying pour reprendre la lecture au retour
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null && isPlaying) {
            // Reprendre la lecture si l'activité était en train de jouer avant d'être mise en pause
            mediaPlayer.start();
            updateSeekbar();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
    }

    // AsyncTask pour charger les images sans utiliser Picasso
    private static class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;

        public LoadImageTask(ImageView imageView) {
            this.imageViewReference = new WeakReference<>(imageView);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            String imageUrl = params[0];
            Bitmap bitmap = null;
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                bitmap = BitmapFactory.decodeStream(input);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null && imageViewReference != null) {
                ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            } else {
                // En cas d'erreur, afficher une image par défaut
                ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageResource(R.drawable.ic_album_placeholder);
                }
            }
        }
    }
}
