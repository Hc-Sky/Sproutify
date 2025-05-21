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

import com.example.sproutify.data.FavoritesManager;
import com.example.sproutify.model.Track;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
    private FavoritesManager favoritesManager;

    // UI Components
    private ImageView coverImageView;
    private TextView titleTextView;
    private TextView artistTextView;
    private TextView lyricsTextView; // TextView pour les paroles
    private SeekBar seekBar;
    private TextView currentTimeTextView;
    private TextView totalTimeTextView;
    private FloatingActionButton playPauseButton;
    private ImageButton prevButton;
    private ImageButton nextButton;
    private FloatingActionButton favoriteButton;
    private MaterialToolbar toolbar;

    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // Initialisation du gestionnaire de favoris
        favoritesManager = FavoritesManager.getInstance(this);

        // Initialisation des composants UI
        coverImageView = findViewById(R.id.playerCoverImage);
        titleTextView = findViewById(R.id.playerTrackTitle);
        artistTextView = findViewById(R.id.playerArtistName);
        lyricsTextView = findViewById(R.id.playerLyricsText); // Initialisation du TextView des paroles
        seekBar = findViewById(R.id.playerSeekBar);
        currentTimeTextView = findViewById(R.id.playerCurrentTime);
        totalTimeTextView = findViewById(R.id.playerTotalTime);
        playPauseButton = findViewById(R.id.playerPlayPauseButton);
        prevButton = findViewById(R.id.playerPrevButton);
        nextButton = findViewById(R.id.playerNextButton);
        favoriteButton = findViewById(R.id.playerFavoriteButton);
        toolbar = findViewById(R.id.playerToolbar);

        // Configuration de la toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        toolbar.setNavigationOnClickListener(view -> finish());

        // Récupération des données transmises
        if (getIntent().hasExtra(EXTRA_TRACK)) {
            currentTrack = getIntent().getParcelableExtra(EXTRA_TRACK);
        }

        if (getIntent().hasExtra(EXTRA_TRACK_LIST)) {
            trackList = getIntent().getParcelableArrayListExtra(EXTRA_TRACK_LIST);
            currentTrackPosition = getIntent().getIntExtra(EXTRA_TRACK_POSITION, 0);
            currentTrack = trackList.get(currentTrackPosition);
        }

        if (currentTrack == null) {
            Toast.makeText(this, "Erreur lors du chargement de la piste", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialisation du MediaPlayer
        mediaPlayer = new MediaPlayer();
        handler = new Handler();

        // Mise à jour de l'interface utilisateur
        updateUI();

        // Chargement et lancement de la piste
        loadAndPlayTrack();

        // Configuration du SeekBar
        setupSeekBar();

        // Configuration des boutons
        setupButtons();

        // Mise à jour de l'état du bouton favori
        updateFavoriteButton();
    }

    private void updateUI() {
        titleTextView.setText(currentTrack.title);
        artistTextView.setText(currentTrack.artist);

        // Affichage des paroles si disponibles
        if (currentTrack.contentLines != null && !currentTrack.contentLines.isEmpty()) {
            lyricsTextView.setText(currentTrack.contentLines);
            lyricsTextView.setVisibility(View.VISIBLE);
        } else {
            lyricsTextView.setText("Aucune parole disponible");
            lyricsTextView.setVisibility(View.VISIBLE);
        }

        // Chargement de l'image
        if (currentTrack.coverUrl != null && !currentTrack.coverUrl.isEmpty()) {
            new LoadImageTask(coverImageView).execute(currentTrack.coverUrl);
        } else {
            coverImageView.setImageResource(R.drawable.ic_launcher_background);
        }
    }

    private void loadAndPlayTrack() {
        isLoading = true;
        playPauseButton.setImageResource(R.drawable.ic_pause);

        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(currentTrack.mp3Url);
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                isLoading = false;
                isPlaying = true;
                mediaPlayer.start();

                // Mettre à jour le temps total
                totalTimeTextView.setText(formatDuration(mediaPlayer.getDuration()));
                seekBar.setMax(mediaPlayer.getDuration());

                // Démarrer la mise à jour du SeekBar
                updateSeekBar();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                playPauseButton.setImageResource(R.drawable.ic_play);

                // Passer à la chanson suivante si possible
                if (trackList.size() > 1) {
                    playNextTrack();
                }
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                isLoading = false;
                isPlaying = false;
                playPauseButton.setImageResource(R.drawable.ic_play);
                Toast.makeText(PlayerActivity.this, "Erreur de lecture", Toast.LENGTH_SHORT).show();
                return true;
            });

        } catch (IOException e) {
            isLoading = false;
            Log.e(TAG, "Erreur lors du chargement de la piste: " + e.getMessage());
            Toast.makeText(this, "Erreur lors du chargement de la piste", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                    currentTimeTextView.setText(formatDuration(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Optionnel
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Optionnel
            }
        });
    }

    private void updateSeekBar() {
        if (mediaPlayer != null && isPlaying) {
            seekBar.setProgress(mediaPlayer.getCurrentPosition());
            currentTimeTextView.setText(formatDuration(mediaPlayer.getCurrentPosition()));

            handler.postDelayed(() -> {
                updateSeekBar();
            }, 1000);
        }
    }

    private void setupButtons() {
        // Bouton lecture/pause
        playPauseButton.setOnClickListener(view -> {
            if (isLoading) return;

            if (isPlaying) {
                pausePlayback();
            } else {
                resumePlayback();
            }
        });

        // Bouton précédent
        prevButton.setOnClickListener(view -> {
            playPreviousTrack();
        });

        // Bouton suivant
        nextButton.setOnClickListener(view -> {
            playNextTrack();
        });

        // Bouton favori
        favoriteButton.setOnClickListener(view -> {
            toggleFavoriteStatus();
        });
    }

    private void playPreviousTrack() {
        if (trackList.size() <= 1) return;

        currentTrackPosition--;
        if (currentTrackPosition < 0) {
            currentTrackPosition = trackList.size() - 1;
        }

        currentTrack = trackList.get(currentTrackPosition);
        updateUI();
        loadAndPlayTrack();
        updateFavoriteButton();
    }

    private void playNextTrack() {
        if (trackList.size() <= 1) return;

        currentTrackPosition++;
        if (currentTrackPosition >= trackList.size()) {
            currentTrackPosition = 0;
        }

        currentTrack = trackList.get(currentTrackPosition);
        updateUI();
        loadAndPlayTrack();
        updateFavoriteButton();
    }

    private void pausePlayback() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            playPauseButton.setImageResource(R.drawable.ic_play);
        }
    }

    private void resumePlayback() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isPlaying = true;
            playPauseButton.setImageResource(R.drawable.ic_pause);
            updateSeekBar();
        }
    }

    private void toggleFavoriteStatus() {
        boolean isFavorite = favoritesManager.toggleFavorite(currentTrack);
        updateFavoriteButton();
    }

    private void updateFavoriteButton() {
        boolean isFavorite = favoritesManager.isFavorite(currentTrack);
        if (isFavorite) {
            favoriteButton.setImageResource(R.drawable.ic_favorite_filled);
        } else {
            favoriteButton.setImageResource(R.drawable.ic_favorite_border);
        }
    }

    private String formatDuration(long duration) {
        return String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(duration),
                TimeUnit.MILLISECONDS.toSeconds(duration) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        pausePlayback();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    // Classe AsyncTask pour charger les images
    private static class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;

        LoadImageTask(ImageView imageView) {
            imageViewReference = new WeakReference<>(imageView);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            try {
                URL url = new URL(params[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                return BitmapFactory.decodeStream(input);
            } catch (IOException e) {
                Log.e(TAG, "Erreur lors du chargement de l'image: " + e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null && imageViewReference.get() != null) {
                imageViewReference.get().setImageBitmap(result);
            }
        }
    }
}
