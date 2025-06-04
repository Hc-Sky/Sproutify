package com.example.sproutify;

import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
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

import com.bumptech.glide.Glide;
import com.example.sproutify.data.FavoritesManager;
import com.example.sproutify.data.MusicPlayerState;
import com.example.sproutify.model.Track;
import com.example.sproutify.service.MusicService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.PlaybackException;

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
    public static final String EXTRA_TRACK = "extra_track";
    public static final String EXTRA_TRACK_LIST = "extra_track_list";
    public static final String EXTRA_TRACK_POSITION = "extra_track_position";
    private static final String ACTION_PLAYER_CONTROL = "com.example.sproutify.PLAYER_CONTROL";
    private static boolean isActive = false;

    private Track currentTrack;
    private List<Track> trackList = new ArrayList<>();
    private int currentTrackPosition = 0;
    private Handler handler;
    private Runnable updateSeekBar;
    private boolean isLoading = false;
    private FavoritesManager favoritesManager;
    private boolean isPlaying = false;
    private MusicService musicService;
    private boolean bound = false;

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
    private ImageButton previousButton;
    private ImageButton nextButton;
    private FloatingActionButton favoriteButton;
    private MaterialToolbar toolbar;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected: Service connecté");
            try {
                MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
                musicService = binder.getService();
                bound = true;
                Log.d(TAG, "onServiceConnected: Service lié avec succès");
                
                // Mettre à jour l'interface utilisateur une fois le service lié
                runOnUiThread(() -> {
                    updateUI();
                    setupSeekBar();
                    setupButtons(); // Réinitialiser les boutons après la connexion
                    
                    // Si une piste est déjà sélectionnée, la jouer
                    if (currentTrack != null) {
                        Log.d(TAG, "onServiceConnected: Piste déjà sélectionnée, démarrage de la lecture");
                        loadAndPlayTrack();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "onServiceConnected: Erreur lors de la connexion au service", e);
                bound = false;
                musicService = null;
                Toast.makeText(PlayerActivity.this, "Erreur de connexion au service de musique", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected: Service déconnecté");
            bound = false;
            musicService = null;
            
            // Tenter de reconnecter le service
            new Handler().postDelayed(() -> {
                if (!bound) {
                    Log.d(TAG, "onServiceDisconnected: Tentative de reconnexion au service");
                    Intent intent = new Intent(PlayerActivity.this, MusicService.class);
                    bindService(intent, connection, Context.BIND_AUTO_CREATE);
                }
            }, 1000);
        }
    };

    public static boolean isActive() {
        return isActive;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isActive = true;
        setContentView(R.layout.activity_player);

        // Initialisation du Handler
        handler = new Handler(Looper.getMainLooper());

        // Initialisation des composants UI
        coverImageView = findViewById(R.id.coverImage);
        titleTextView = findViewById(R.id.playerTrackTitle);
        artistTextView = findViewById(R.id.playerArtistName);
        lyricsTextView = findViewById(R.id.playerLyricsText);
        seekBar = findViewById(R.id.playerSeekBar);
        currentTimeTextView = findViewById(R.id.playerCurrentTime);
        totalTimeTextView = findViewById(R.id.playerTotalTime);
        playPauseButton = findViewById(R.id.playerPlayPauseButton);
        previousButton = findViewById(R.id.playerPrevButton);
        nextButton = findViewById(R.id.playerNextButton);
        favoriteButton = findViewById(R.id.playerFavoriteButton);
        toolbar = findViewById(R.id.playerToolbar);
        equalizerView = findViewById(R.id.playerEqualizer);

        // Ajouter un listener long click pour le test audio
        playPauseButton.setOnLongClickListener(view -> {
            testAudioPlayback();
            return true;
        });

        // Initialisation des animations
        setupAnimations();

        // Configuration de la toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        toolbar.setNavigationOnClickListener(view -> finish());

        // Initialisation du gestionnaire de favoris
        favoritesManager = FavoritesManager.getInstance(this);

        // Configuration des écouteurs d'événements
        setupEventListeners();

        // Récupération des données
        Track track = getIntent().getParcelableExtra(EXTRA_TRACK);
        ArrayList<Track> tracks = getIntent().getParcelableArrayListExtra(EXTRA_TRACK_LIST);
        int position = getIntent().getIntExtra(EXTRA_TRACK_POSITION, 0);

        if (track != null && tracks != null && !tracks.isEmpty()) {
            trackList = new ArrayList<>(tracks); // Créer une nouvelle liste pour éviter les problèmes de référence
            currentTrackPosition = position;
            currentTrack = track;
            
            // Mettre à jour l'état global
            MusicPlayerState.getInstance().setTrackList(trackList);
            MusicPlayerState.getInstance().setCurrentTrackPosition(currentTrackPosition);
            MusicPlayerState.getInstance().setCurrentTrack(currentTrack);
            MusicPlayerState.getInstance().setPlaying(true);
            
            Log.d(TAG, "onCreate: Liste des pistes initialisée avec " + trackList.size() + " pistes");
            Log.d(TAG, "onCreate: Position actuelle: " + currentTrackPosition);
            
            updateUI();
            loadAndPlayTrack();
        } else {
            Log.e(TAG, "onCreate: Données invalides - track: " + (track != null) + ", tracks: " + (tracks != null));
            Toast.makeText(this, "Erreur: Impossible de charger la liste des pistes", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Configuration du SeekBar
        setupSeekBar();

        // Configuration des boutons
        setupButtons();

        // Mise à jour de l'état du bouton favori
        updateFavoriteButton();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: Début de la liaison du service");
        // Lier le service de musique
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        
        // Vérifier l'état du service après un court délai
        new Handler().postDelayed(() -> {
            Log.d(TAG, "onStart: Vérification de l'état du service - bound: " + bound + ", musicService: " + (musicService != null));
            if (!bound || musicService == null) {
                Toast.makeText(this, "Problème de connexion au service de musique", Toast.LENGTH_SHORT).show();
            }
        }, 1000);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: Début de la déliaison du service");
        // Délier le service de musique
        if (bound) {
            unbindService(connection);
            bound = false;
            Log.d(TAG, "onStop: Service délié");
        }

        // Arrêter les mises à jour du seekBar
        if (handler != null && updateSeekBar != null) {
            handler.removeCallbacks(updateSeekBar);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActive = false;
        if (handler != null && updateSeekBar != null) {
            handler.removeCallbacks(updateSeekBar);
        }
        if (coverImageView != null) {
            coverImageView.clearAnimation();
        }
    }

    private void setupEventListeners() {
        // Configuration des boutons
        // Suppression des listeners redondants qui entrent en conflit avec setupButtons()
        // playPauseButton.setOnClickListener(view -> togglePlayPause());
        // previousButton.setOnClickListener(view -> playPreviousTrack());
        // nextButton.setOnClickListener(view -> playNextTrack());
        // favoriteButton.setOnClickListener(view -> toggleFavorite());

        // Ces listeners sont maintenant définis uniquement dans setupButtons()

        // Configuration de la barre de progression
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && bound && musicService != null) {
                    musicService.seekTo(progress);
                    updateCurrentTime(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (handler != null) {
                    handler.removeCallbacks(updateSeekBar);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (handler != null) {
                    startSeekBarUpdates();
                }
            }
        });
    }

    private void setupAnimations() {
        // Animation de vue (View Animation) - Rotation de la pochette d'album
        rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_album_art);
        rotateAnimation.setRepeatCount(Animation.INFINITE);

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

        // Application des animations
        playPauseButton.setOnClickListener(v -> {
            if (isPlaying) {
                coverImageView.clearAnimation();
                equalizerView.setVisibility(View.GONE);
            } else {
                coverImageView.startAnimation(rotateAnimation);
                equalizerView.setVisibility(View.VISIBLE);
            }
            isPlaying = !isPlaying;
            updatePlayPauseButton();
        });
        
        favoriteButton.setOnClickListener(v -> {
            toggleFavorite();
        });
    }

    private void setupButtons() {
        // Vérification de l'état du service au démarrage
        Log.d(TAG, "setupButtons: État initial - bound: " + bound + ", musicService: " + (musicService != null));

        playPauseButton.setOnClickListener(view -> {
            Log.d(TAG, "PlayPause button clicked - bound: " + bound + ", musicService: " + (musicService != null));
            if (!bound || musicService == null) {
                Log.e(TAG, "setupButtons: Service non disponible");
                return;
            }
            
            try {
                view.startAnimation(pulseAnimation);
                togglePlayPause();
                Log.d(TAG, "PlayPause action completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error in playPause action", e);
                Toast.makeText(this, "Erreur lors de la lecture/pause", Toast.LENGTH_SHORT).show();
            }
        });

        previousButton.setOnClickListener(view -> {
            Log.d(TAG, "Previous button clicked - bound: " + bound + ", musicService: " + (musicService != null));
            if (!bound || musicService == null) {
                Log.e(TAG, "setupButtons: Service non disponible");
                return;
            }
            
            try {
                view.startAnimation(pulseAnimation);
                playPreviousTrack();
                Log.d(TAG, "Previous action completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error in previous action", e);
                Toast.makeText(this, "Erreur lors du changement de piste", Toast.LENGTH_SHORT).show();
            }
        });

        nextButton.setOnClickListener(view -> {
            Log.d(TAG, "Next button clicked - bound: " + bound + ", musicService: " + (musicService != null));
            if (!bound || musicService == null) {
                Log.e(TAG, "setupButtons: Service non disponible");
                return;
            }
            
            try {
                view.startAnimation(pulseAnimation);
                playNextTrack();
                Log.d(TAG, "Next action completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error in next action", e);
                Toast.makeText(this, "Erreur lors du changement de piste", Toast.LENGTH_SHORT).show();
            }
        });

        favoriteButton.setOnClickListener(view -> {
            toggleFavorite();
        });
    }

    private void updatePlayPauseButton() {
        if (!bound || musicService == null) {
            Log.e(TAG, "updatePlayPauseButton: Service non disponible");
            return;
        }

        try {
            boolean isPlaying = musicService.isPlaying();
            Log.d(TAG, "updatePlayPauseButton: État de lecture - " + isPlaying);
            
            runOnUiThread(() -> {
                if (playPauseButton != null) {
                    playPauseButton.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
                }
                
                if (isPlaying) {
                    startAnimations();
                    startSeekBarUpdates();
                } else {
                    stopAnimations();
                    if (handler != null && updateSeekBar != null) {
                        handler.removeCallbacks(updateSeekBar);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "updatePlayPauseButton: Erreur lors de la mise à jour du bouton", e);
        }
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

    private void toggleFavorite() {
        boolean isFavorite = favoritesManager.toggleFavorite(currentTrack);
        updateFavoriteButton();

        // Ajouter l'animation du bouton favori
        animateFavoriteButton();
    }

    private void loadAndPlayTrack() {
        try {
            Log.d(TAG, "loadAndPlayTrack: Début de la méthode");
            
            if (!bound || musicService == null) {
                Log.e(TAG, "loadAndPlayTrack: Service non lié ou null");
                return;
            }

            if (isLoading) {
                Log.d(TAG, "loadAndPlayTrack: Déjà en cours de chargement");
                return;
            }
            
            if (currentTrack == null) {
                Log.e(TAG, "loadAndPlayTrack: Aucune piste sélectionnée");
                return;
            }
            
            Log.d(TAG, "loadAndPlayTrack: Track à jouer - Titre: " + currentTrack.title);
            Log.d(TAG, "loadAndPlayTrack: URL: " + currentTrack.mp3Url);
            
            isLoading = true;
            Log.d(TAG, "loadAndPlayTrack: État de chargement mis à true");

            // Mettre à jour l'interface avant de commencer la lecture
            updateUI();

            // Réinitialiser le lecteur
            musicService.reset();
            Log.d(TAG, "loadAndPlayTrack: Lecteur réinitialisé");

            // Configurer les listeners pour MediaPlayer
            musicService.setPlaybackListener(new MusicService.OnPlaybackStateChangeListener() {
                @Override
                public void onPlaybackStateChanged(boolean playing) {
                    Log.d(TAG, "onPlaybackStateChanged: État de lecture changé - isPlaying: " + playing);
                    runOnUiThread(() -> {
                        isLoading = false;
                        if (playing) {
                            // Lecture prête
                            Log.d(TAG, "onPlaybackStateChanged: Démarrage des animations");
                            startAnimations();

                            // Initialiser la barre de progression
                            int duration = musicService.getDuration();
                            Log.d(TAG, "onPlaybackStateChanged: Durée de la piste: " + duration);
                            if (seekBar != null) {
                                seekBar.setMax(duration);
                            }
                            if (totalTimeTextView != null) {
                                totalTimeTextView.setText(formatDuration(duration));
                            }
                            startSeekBarUpdates();

                            // Mettre à jour l'interface utilisateur
                            updatePlayPauseButton();
                            MusicPlayerState.getInstance().setPlaying(true);
                            Log.d(TAG, "onPlaybackStateChanged: Interface utilisateur mise à jour");
                        } else {
                            // Lecture en pause ou terminée
                            Log.d(TAG, "onPlaybackStateChanged: Arrêt des animations");
                            stopAnimations();
                            MusicPlayerState.getInstance().setPlaying(false);
                            
                            // Vérifier si la lecture est terminée (et non en pause)
                            if (musicService != null && musicService.getCurrentPosition() >= musicService.getDuration() - 100) {
                                Log.d(TAG, "onPlaybackStateChanged: Lecture terminée, passage à la piste suivante");
                                playNextTrack();
                            } else {
                                Log.d(TAG, "onPlaybackStateChanged: Lecture en pause");
                                updatePlayPauseButton();
                            }
                        }
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "onError: Erreur de lecture - " + errorMessage);
                    runOnUiThread(() -> {
                        isLoading = false;
                        MusicPlayerState.getInstance().setPlaying(false);

                        // Afficher un message d'erreur à l'utilisateur
                        Toast.makeText(PlayerActivity.this,
                            "Impossible de lire ce fichier audio: " + errorMessage,
                            Toast.LENGTH_SHORT).show();

                        // Réinitialiser l'interface utilisateur
                        stopAnimations();
                        updatePlayPauseButton();
                        Log.d(TAG, "onError: Interface utilisateur réinitialisée");
                    });
                }

                @Override
                public void onTrackChanged(Track track) {
                    Log.d(TAG, "onTrackChanged: Nouvelle piste - " + track.title);
                    runOnUiThread(() -> {
                        currentTrack = track;
                        updateUI();
                        Log.d(TAG, "onTrackChanged: Interface mise à jour pour la nouvelle piste");
                    });
                }
            });

            // Lire la piste
            Log.d(TAG, "loadAndPlayTrack: Démarrage de la lecture");
            musicService.playTrack(currentTrack);

        } catch (Exception e) {
            Log.e(TAG, "loadAndPlayTrack: Erreur lors du chargement de la piste", e);
            isLoading = false;
            MusicPlayerState.getInstance().setPlaying(false);
            Toast.makeText(this, "Erreur lors du chargement de la piste: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            updatePlayPauseButton();
        }
    }

    private void playTrack(Track track) {
        if (bound && musicService != null) {
            currentTrack = track;
            currentTrackPosition = trackList.indexOf(track);
            MusicPlayerState.getInstance().setCurrentTrack(currentTrack);
            MusicPlayerState.getInstance().setPlaying(true);
            updateUI();
            musicService.playTrack(track);
        }
    }

    private void playPreviousTrack() {
        if (!bound || musicService == null) {
            Log.e(TAG, "playPreviousTrack: Service non disponible");
            return;
        }

        if (trackList == null || trackList.isEmpty()) {
            Log.e(TAG, "playPreviousTrack: Liste des pistes vide");
            Toast.makeText(this, "Aucune piste disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Log.d(TAG, "playPreviousTrack: Lecture piste précédente");
            Log.d(TAG, "playPreviousTrack: Position actuelle: " + currentTrackPosition + ", Taille liste: " + trackList.size());
            
            // Déclenche l'animation de sortie de la pochette actuelle
            coverImageView.startAnimation(slideOutRight);

            if (currentTrackPosition > 0) {
                currentTrackPosition--;
            } else {
                currentTrackPosition = trackList.size() - 1;
            }
            
            currentTrack = trackList.get(currentTrackPosition);
            if (currentTrack == null) {
                throw new IllegalStateException("Piste non trouvée à la position " + currentTrackPosition);
            }
            
            Log.d(TAG, "playPreviousTrack: Nouvelle piste - " + currentTrack.title);
            
            MusicPlayerState.getInstance().setCurrentTrack(currentTrack);
            MusicPlayerState.getInstance().setCurrentTrackPosition(currentTrackPosition);
            
            // Mettre à jour l'état du bouton favori
            updateFavoriteButton();
            
            // Jouer la piste - L'interface sera mise à jour une fois l'animation terminée
            musicService.playTrack(currentTrack);
            Log.d(TAG, "playPreviousTrack: Piste précédente chargée et en cours de lecture");
        } catch (Exception e) {
            Log.e(TAG, "playPreviousTrack: Erreur lors du changement de piste", e);
            Toast.makeText(this, "Erreur lors du changement de piste: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void playNextTrack() {
        if (!bound || musicService == null) {
            Log.e(TAG, "playNextTrack: Service non disponible");
            return;
        }

        if (trackList == null || trackList.isEmpty()) {
            Log.e(TAG, "playNextTrack: Liste des pistes vide");
            Toast.makeText(this, "Aucune piste disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Log.d(TAG, "playNextTrack: Lecture piste suivante");
            Log.d(TAG, "playNextTrack: Position actuelle: " + currentTrackPosition + ", Taille liste: " + trackList.size());
            
            // Déclenche l'animation de sortie de la pochette actuelle
            coverImageView.startAnimation(slideOutLeft);

            if (currentTrackPosition < trackList.size() - 1) {
                currentTrackPosition++;
            } else {
                currentTrackPosition = 0;
            }
            
            currentTrack = trackList.get(currentTrackPosition);
            if (currentTrack == null) {
                throw new IllegalStateException("Piste non trouvée à la position " + currentTrackPosition);
            }
            
            Log.d(TAG, "playNextTrack: Nouvelle piste - " + currentTrack.title);
            
            MusicPlayerState.getInstance().setCurrentTrack(currentTrack);
            MusicPlayerState.getInstance().setCurrentTrackPosition(currentTrackPosition);
            
            // Mettre à jour l'état du bouton favori
            updateFavoriteButton();
            
            // Jouer la piste - L'interface sera mise à jour une fois l'animation terminée
            musicService.playTrack(currentTrack);
            Log.d(TAG, "playNextTrack: Piste suivante chargée et en cours de lecture");
        } catch (Exception e) {
            Log.e(TAG, "playNextTrack: Erreur lors du changement de piste", e);
            Toast.makeText(this, "Erreur lors du changement de piste: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateFavoriteButton() {
        boolean isFavorite = favoritesManager.isFavorite(currentTrack);
        if (isFavorite) {
            favoriteButton.setImageResource(R.drawable.ic_favorite_filled);
            favoriteButton.setColorFilter(getColor(R.color.favorite_button_tint));
        } else {
            favoriteButton.setImageResource(R.drawable.ic_favorite_border);
            favoriteButton.setColorFilter(getColor(R.color.primary));
        }
    }

    private void updateSeekBar() {
        if (bound && musicService != null && musicService.isPlaying()) {
            int currentPosition = musicService.getCurrentPosition();
            seekBar.setProgress(currentPosition);
            currentTimeTextView.setText(formatDuration(currentPosition));
            totalTimeTextView.setText(formatDuration(musicService.getDuration()));
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
        if (currentTrack == null) {
            Log.e(TAG, "updateUI: Aucune piste sélectionnée");
            return;
        }

        Log.d(TAG, "updateUI: Mise à jour de l'interface pour la piste - " + currentTrack.title);
        
        runOnUiThread(() -> {
            try {
                // Mise à jour du titre et de l'artiste
                if (titleTextView != null) {
                    titleTextView.setText(currentTrack.title);
                    Log.d(TAG, "updateUI: Titre mis à jour - " + currentTrack.title);
                }
                
                if (artistTextView != null) {
                    artistTextView.setText(currentTrack.artist);
                    Log.d(TAG, "updateUI: Artiste mis à jour - " + currentTrack.artist);
                }

                // Affichage des paroles si disponibles
                if (lyricsTextView != null) {
                    if (currentTrack.contentLines != null && !currentTrack.contentLines.isEmpty()) {
                        lyricsTextView.setText(currentTrack.contentLines);
                        lyricsTextView.setVisibility(View.VISIBLE);
                        Log.d(TAG, "updateUI: Paroles mises à jour");
                    } else {
                        lyricsTextView.setText("Aucune parole disponible");
                        lyricsTextView.setVisibility(View.VISIBLE);
                        Log.d(TAG, "updateUI: Aucune parole disponible");
                    }
                }

                // Chargement de l'image
                if (coverImageView != null) {
                    if (currentTrack.coverUrl != null && !currentTrack.coverUrl.isEmpty()) {
                        Log.d(TAG, "updateUI: Chargement de l'image de couverture - " + currentTrack.coverUrl);
                        // Utiliser la même approche que dans MainActivity
                        new LoadImageTask(coverImageView).execute(currentTrack.coverUrl);
                    } else {
                        coverImageView.setImageResource(R.drawable.album_placeholder);
                        Log.d(TAG, "updateUI: URL de couverture vide, image par défaut affichée");
                    }
                } else {
                    Log.e(TAG, "updateUI: coverImageView est null");
                }

                // Mise à jour du bouton favori
                updateFavoriteButton();

                // Mise à jour de l'état de lecture
                updatePlayPauseButton();

                // Mise à jour de la barre de progression
                if (bound && musicService != null) {
                    int duration = musicService.getDuration();
                    if (seekBar != null) {
                        seekBar.setMax(duration);
                        Log.d(TAG, "updateUI: Durée maximale de la barre de progression mise à jour - " + duration);
                    }
                    
                    if (totalTimeTextView != null) {
                        totalTimeTextView.setText(formatDuration(duration));
                    }
                    
                    if (currentTimeTextView != null) {
                        currentTimeTextView.setText(formatDuration(musicService.getCurrentPosition()));
                    }
                }

                Log.d(TAG, "updateUI: Mise à jour de l'interface terminée avec succès");
            } catch (Exception e) {
                Log.e(TAG, "updateUI: Erreur lors de la mise à jour de l'interface", e);
            }
        });
    }

    private void setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && bound && musicService != null) {
                    Log.d(TAG, "onProgressChanged: Changement de position à " + progress);
                    musicService.seekTo(progress);
                    currentTimeTextView.setText(formatDuration(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.d(TAG, "onStartTrackingTouch: Début du suivi");
                if (handler != null && updateSeekBar != null) {
                    handler.removeCallbacks(updateSeekBar);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d(TAG, "onStopTrackingTouch: Fin du suivi");
                if (bound && musicService != null && musicService.isPlaying()) {
                    startSeekBarUpdates();
                }
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
        Log.d(TAG, "animateNextCover: Début de l'animation d'entrée de la nouvelle pochette");

        // Mise à jour de l'interface avec la nouvelle piste
        updateUI();

        // Démarrer l'animation d'entrée par la droite
        coverImageView.startAnimation(slideInRight);
    }

    /**
     * Appelée après que l'animation de sortie vers la droite soit terminée
     * pour afficher la nouvelle pochette qui arrive par la gauche
     */
    private void animatePreviousCover() {
        Log.d(TAG, "animatePreviousCover: Début de l'animation d'entrée de la nouvelle pochette");

        // Mise à jour de l'interface avec la nouvelle piste
        updateUI();

        // Démarrer l'animation d'entrée par la gauche
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
        MusicPlayerState.getInstance().setCurrentTrack(currentTrack);
        playTrack(currentTrack);
        updateUI();
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
        MusicPlayerState.getInstance().setCurrentTrack(currentTrack);
        playTrack(currentTrack);
        updateUI();
        updateFavoriteButton();
    }

    /**
     * Méthode de test pour vérifier si la lecture audio fonctionne
     * avec une URL de test connue.
     */
    private void testAudioPlayback() {
        try {
            Log.d(TAG, "testAudioPlayback: Début du test audio");
            Toast.makeText(this, "Test de lecture audio avec MediaPlayer...", Toast.LENGTH_SHORT).show();

            if (!bound || musicService == null) {
                Log.e(TAG, "testAudioPlayback: Service non lié ou null");
                Toast.makeText(this, "Service de musique non lié", Toast.LENGTH_SHORT).show();
                return;
            }

            // URL d'un fichier audio de test public et fiable
            String testAudioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3";
            Log.d(TAG, "testAudioPlayback: URL de test: " + testAudioUrl);

            // Créer une piste de test avec tous les champs requis
            Track testTrack = new Track(
                "test_id",           // id
                "Test Audio",        // title
                "Test Album",        // album
                "Test Artist",       // artist
                "2024",             // date
                "",                 // coverUrl
                "",                 // contentLines
                testAudioUrl,       // mp3Url
                "180"               // duration
            );
            Log.d(TAG, "testAudioPlayback: Piste de test créée");

            // Configurer le service pour lire ce fichier audio de test
            musicService.reset();
            Log.d(TAG, "testAudioPlayback: Service réinitialisé");

            // Configurer les écouteurs pour ce test spécifique
            musicService.setPlaybackListener(new MusicService.OnPlaybackStateChangeListener() {
                @Override
                public void onPlaybackStateChanged(boolean playing) {
                    Log.d(TAG, "testAudioPlayback - onPlaybackStateChanged: isPlaying = " + playing);
                    runOnUiThread(() -> {
                        if (playing) {
                            Toast.makeText(PlayerActivity.this, "Audio de test prêt, lecture en cours", Toast.LENGTH_SHORT).show();
                            MusicPlayerState.getInstance().setPlaying(true);
                            playPauseButton.setImageResource(R.drawable.ic_pause);
                            Log.d(TAG, "testAudioPlayback: Lecture démarrée avec succès");
                        }
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "testAudioPlayback - onError: " + errorMessage);
                    runOnUiThread(() -> {
                        Toast.makeText(PlayerActivity.this, "Erreur audio de test: " + errorMessage, Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onTrackChanged(Track track) {
                    Log.d(TAG, "testAudioPlayback - onTrackChanged: " + track.title);
                    runOnUiThread(() -> {
                        currentTrack = track;
                        updateUI();
                    });
                }
            });

            // Démarrer la lecture du fichier de test
            Log.d(TAG, "testAudioPlayback: Démarrage de la lecture de test");
            musicService.playTrack(testTrack);

        } catch (Exception e) {
            Log.e(TAG, "testAudioPlayback: Erreur lors du test audio", e);
            Toast.makeText(this, "Erreur de test audio: " + e.getMessage(), Toast.LENGTH_LONG).show();
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

    @Override
    public void finish() {
        super.finish();
        // Ajouter une animation de transition lors du retour
        overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_up);
    }

    private void togglePlayPause() {
        if (!bound || musicService == null) {
            Log.e(TAG, "togglePlayPause: Service non disponible");
            return;
        }

        try {
            Log.d(TAG, "togglePlayPause: État actuel - isPlaying: " + musicService.isPlaying());
            musicService.togglePlayPause();
            
            // Mettre à jour l'interface après un court délai pour s'assurer que l'état est bien changé
            new Handler().postDelayed(() -> {
                boolean isPlaying = musicService.isPlaying();
                Log.d(TAG, "togglePlayPause: Nouvel état - isPlaying: " + isPlaying);
                
                runOnUiThread(() -> {
                    if (playPauseButton != null) {
                        playPauseButton.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
                    }
                    
                    if (isPlaying) {
                        startAnimations();
                        startSeekBarUpdates();
                    } else {
                        stopAnimations();
                        if (handler != null && updateSeekBar != null) {
                            handler.removeCallbacks(updateSeekBar);
                        }
                    }
                });
            }, 100);
        } catch (Exception e) {
            Log.e(TAG, "togglePlayPause: Erreur lors du changement d'état", e);
            Toast.makeText(this, "Erreur lors de la lecture/pause", Toast.LENGTH_SHORT).show();
        }
    }

    private void startSeekBarUpdates() {
        if (handler != null) {
            handler.removeCallbacks(updateSeekBar);
            updateSeekBar = new Runnable() {
                @Override
                public void run() {
                    if (bound && musicService != null && musicService.isPlaying()) {
                        int currentPosition = musicService.getCurrentPosition();
                        seekBar.setProgress(currentPosition);
                        currentTimeTextView.setText(formatDuration(currentPosition));
                        handler.postDelayed(this, 1000);
                    }
                }
            };
            handler.post(updateSeekBar);
        }
    }

    private void updateCurrentTime(int position) {
        if (bound && musicService != null) {
            int duration = musicService.getDuration();
            currentTimeTextView.setText(formatDuration(position));
            totalTimeTextView.setText(formatDuration(duration));
        }
    }
}
