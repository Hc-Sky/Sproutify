package com.example.sproutify;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sproutify.model.Track;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PlayerActivity extends AppCompatActivity {

    public static final String EXTRA_TRACK = "track";

    private Track currentTrack;
    private MediaPlayer mediaPlayer;
    private Handler handler;
    private Runnable updateSeekBar;

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

        // Récupération du morceau depuis l'intent
        if (getIntent().hasExtra(EXTRA_TRACK)) {
            currentTrack = getIntent().getParcelableExtra(EXTRA_TRACK);
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
        // Affichage des informations du morceau
        titleTextView.setText(track.title);
        artistTextView.setText(track.artist);

        // Chargement de la pochette avec AsyncTask au lieu de Picasso
        new LoadImageTask(coverImageView).execute(track.coverUrl);

        // Désactiver les boutons jusqu'à ce que le mediaPlayer soit prêt
        playPauseButton.setEnabled(false);
        prevButton.setEnabled(false);
        nextButton.setEnabled(false);

        // Initialisation du MediaPlayer
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        mediaPlayer = new MediaPlayer();
        try {
            // Afficher un message de chargement
            currentTimeTextView.setText("Chargement...");

            mediaPlayer.setDataSource(track.mp3Url);
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                // Initialisation de la seekbar
                seekBar.setMax(mediaPlayer.getDuration());
                totalTimeTextView.setText(formatTime(mediaPlayer.getDuration()));
                currentTimeTextView.setText("0:00");

                // Activer tous les boutons
                playPauseButton.setEnabled(true);
                prevButton.setEnabled(true);
                nextButton.setEnabled(true);

                // Lancement automatique de la lecture
                togglePlayPause();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                playPauseButton.setImageResource(R.drawable.ic_play);
                isPlaying = false;
                seekBar.setProgress(0);
                currentTimeTextView.setText("0:00");
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                // Gérer les erreurs de lecture
                playPauseButton.setEnabled(false);
                currentTimeTextView.setText("Erreur de lecture");
                return true;
            });

        } catch (IOException e) {
            e.printStackTrace();
            currentTimeTextView.setText("Erreur: URL invalide");
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

        // Pour simuler les fonctionnalités précédent/suivant
        // (dans une vraie application, ces boutons navigueraient entre les morceaux)
        prevButton.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                mediaPlayer.seekTo(0);
                seekBar.setProgress(0);
                currentTimeTextView.setText("0:00");
            }
        });

        nextButton.setOnClickListener(v -> {
            // Dans une implémentation réelle, on passerait au morceau suivant
            // Pour l'exemple, on simule la fin du morceau
            if (mediaPlayer != null) {
                mediaPlayer.seekTo(mediaPlayer.getDuration());
                playPauseButton.setImageResource(R.drawable.ic_play);
                isPlaying = false;
            }
        });
    }

    private void togglePlayPause() {
        if (mediaPlayer != null) {
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
        if (mediaPlayer != null) {
            int currentPosition = mediaPlayer.getCurrentPosition();
            seekBar.setProgress(currentPosition);
            currentTimeTextView.setText(formatTime(currentPosition));

            // Mise à jour toutes les 100ms
            updateSeekBar = () -> updateSeekbar();
            handler.postDelayed(updateSeekBar, 100);
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
        if (mediaPlayer != null) {
            handler.removeCallbacks(updateSeekBar);
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    // AsyncTask pour charger les images sans utiliser Picasso
    private static class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        private final ImageView imageView;
        private final WeakReference<ImageView> imageViewReference;

        public LoadImageTask(ImageView imageView) {
            this.imageView = imageView;
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
                if (imageView != null) {
                    imageView.setImageResource(R.drawable.ic_album_placeholder);
                }
            }
        }
    }
}
