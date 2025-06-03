package com.example.sproutify;

import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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

    // Animations
    private Animation rotateAnimation;
    private Animation pulseAnimation;
    private AnimationDrawable equalizerAnimation;
    private ImageView equalizerView;

    // Animations de transition
    private Animation slideOutLeft;
    private Animation slideInRight;
    private Animation slideOutRight;
    private Animation slideInLeft;

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
        equalizerView = findViewById(R.id.playerEqualizer);

        // Initialisation des animations
        setupAnimations();

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

    private void setupAnimations() {
        // Animation de vue (View Animation) - Rotation de la pochette d'album
        rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_album_art);

        // Animation de vue (View Animation) - Pulsation des boutons
        pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_animation);

        // Animation image par image (Frame Animation) - Égaliseur
        equalizerAnimation = (AnimationDrawable) equalizerView.getDrawable();

        // Animations de transition pour le changement de piste
        slideOutLeft = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);
        slideInRight = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
        slideOutRight = AnimationUtils.loadAnimation(this, R.anim.slide_out_right);
        slideInLeft = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);

        // Configurer les écouteurs d'animation pour enchaîner les animations
        slideOutLeft.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                // Quand l'animation de sortie est terminée, lancer l'animation d'entrée
                animateNextCover();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        slideOutRight.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                // Quand l'animation de sortie est terminée, lancer l'animation d'entrée
                animatePreviousCover();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
    }

    private void setupButtons() {
        // Bouton lecture/pause
        playPauseButton.setOnClickListener(view -> {
            // Appliquer l'animation de pulsation au bouton
            view.startAnimation(pulseAnimation);

            if (isLoading) return;

            if (isPlaying) {
                pausePlayback();
            } else {
                resumePlayback();
            }
        });

        // Bouton précédent
        prevButton.setOnClickListener(view -> {
            // Appliquer l'animation de pulsation au bouton
            view.startAnimation(pulseAnimation);

            playPreviousTrack();
        });

        // Bouton suivant
        nextButton.setOnClickListener(view -> {
            // Appliquer l'animation de pulsation au bouton
            view.startAnimation(pulseAnimation);

            playNextTrack();
        });

        // Bouton favori
        favoriteButton.setOnClickListener(view -> {
            // L'animation est gérée dans toggleFavoriteStatus()
            toggleFavoriteStatus();
        });
    }

    /**
     * Démarre toutes les animations quand la musique joue
     */
    private void startAnimations() {
        // Animation image par image (Frame Animation)
        if (equalizerAnimation != null && !equalizerAnimation.isRunning()) {
            equalizerAnimation.start();
        }

        // On ne démarre plus l'animation de rotation de la pochette
    }

    /**
     * Arrête toutes les animations quand la musique est en pause
     */
    private void stopAnimations() {
        // Animation image par image (Frame Animation)
        if (equalizerAnimation != null && equalizerAnimation.isRunning()) {
            equalizerAnimation.stop();
        }

        // Animation de vue (View Animation)
        if (coverImageView != null) {
            coverImageView.clearAnimation();
        }
    }

    /**
     * Applique l'animation ObjectAnimator sur le bouton favori
     */
    private void animateFavoriteButton() {
        // ObjectAnimator (Property Animation)
        android.animation.AnimatorSet favoriteAnim = (android.animation.AnimatorSet)
            AnimatorInflater.loadAnimator(this, R.animator.favorite_button_animation);
        favoriteAnim.setTarget(favoriteButton);
        favoriteAnim.start();
    }

    private void toggleFavoriteStatus() {
        boolean isFavorite = favoritesManager.toggleFavorite(currentTrack);
        updateFavoriteButton();

        // Ajouter l'animation du bouton favori
        animateFavoriteButton();
    }

    private void pausePlayback() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            playPauseButton.setImageResource(R.drawable.ic_play);
            // Arrêter les animations quand la musique est en pause
            stopAnimations();
        }
    }

    private void resumePlayback() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isPlaying = true;
            playPauseButton.setImageResource(R.drawable.ic_pause);
            updateSeekBar();
            // Démarrer les animations quand la musique joue
            startAnimations();
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

                // Démarrer les animations
                startAnimations();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                playPauseButton.setImageResource(R.drawable.ic_play);

                // Arrêter les animations à la fin de la piste
                stopAnimations();

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

    private void playPreviousTrack() {
        if (trackList.size() <= 1) return;

        animateToPreviousTrack();
    }

    private void playNextTrack() {
        if (trackList.size() <= 1) return;

        animateToNextTrack();
    }

    private void updateFavoriteButton() {
        boolean isFavorite = favoritesManager.isFavorite(currentTrack);
        if (isFavorite) {
            favoriteButton.setImageResource(R.drawable.ic_favorite_filled);
        } else {
            favoriteButton.setImageResource(R.drawable.ic_favorite_border);
        }
    }

    private void updateSeekBar() {
        if (mediaPlayer != null && isPlaying) {
            seekBar.setProgress(mediaPlayer.getCurrentPosition());
            currentTimeTextView.setText(formatDuration(mediaPlayer.getCurrentPosition()));

            handler.postDelayed(this::updateSeekBar, 1000);
        }
    }

    private String formatDuration(long duration) {
        return String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(duration),
                TimeUnit.MILLISECONDS.toSeconds(duration) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
        );
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
            coverImageView.setImageResource(R.drawable.ic_album_placeholder);
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

    /**
     * Démarre l'animation de glissement de la pochette vers la piste suivante
     */
    private void animateToNextTrack() {
        // On arrête la rotation d'abord
        coverImageView.clearAnimation();
        // On lance l'animation de sortie par la gauche
        coverImageView.startAnimation(slideOutLeft);
    }

    /**
     * Démarre l'animation de glissement de la pochette vers la piste précédente
     */
    private void animateToPreviousTrack() {
        // On arrête la rotation d'abord
        coverImageView.clearAnimation();
        // On lance l'animation de sortie par la droite
        coverImageView.startAnimation(slideOutRight);
    }

    /**
     * Appelée après que l'animation de sortie vers la gauche soit terminée
     * pour afficher la nouvelle pochette qui arrive par la droite
     */
    private void animateNextCover() {
        // On passe à la piste suivante (sans animation pour éviter une boucle)
        changeToNextTrack();
        // On démarre l'animation d'entrée par la droite
        coverImageView.startAnimation(slideInRight);
    }

    /**
     * Appelée après que l'animation de sortie vers la droite soit terminée
     * pour afficher la nouvelle pochette qui arrive par la gauche
     */
    private void animatePreviousCover() {
        // On passe à la piste précédente (sans animation pour éviter une boucle)
        changeToPreviousTrack();
        // On démarre l'animation d'entrée par la gauche
        coverImageView.startAnimation(slideInLeft);
    }

    /**
     * Change à la piste suivante sans animation (utilisée par l'animation)
     */
    private void changeToNextTrack() {
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

    /**
     * Change à la piste précédente sans animation (utilisée par l'animation)
     */
    private void changeToPreviousTrack() {
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

    @Override
    public void finish() {
        super.finish();
        // Ajouter une animation de transition lors du retour
        overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_up);
    }
}
