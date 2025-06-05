package com.example.sproutify;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.sproutify.data.CsvLoader;
import com.example.sproutify.data.MusicPlayerState;
import com.example.sproutify.model.Track;
import com.example.sproutify.service.MusicService;
import com.example.sproutify.ui.MainPagerAdapter;
import com.example.sproutify.ui.TrackAdapter;
import com.example.sproutify.ui.TracksFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Activité principale de l'application Sproutify
 * Gère l'interface utilisateur principale, le lecteur de musique et la navigation
 */
public class MainActivity extends AppCompatActivity implements TrackAdapter.OnTrackClickListener, TrackAdapter.OnTrackFavoriteListener {
    private static final String TAG = "MainActivity";

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private MainPagerAdapter pagerAdapter;
    private List<Track> allTracks = new ArrayList<>();

    // Mini Player components
    private View miniPlayerLayout;
    private ImageView miniPlayerCover;
    private TextView miniPlayerTitle;
    private TextView miniPlayerArtist;
    private FloatingActionButton miniPlayerPlayPause;
    private FloatingActionButton miniPlayerNext;
    private Handler handler;
    private Runnable playerStatusChecker;

    private MusicService musicService;
    private boolean bound = false;

    /**
     * Connection au service de musique
     * Gère la liaison avec le service de lecture de musique et met à jour l'interface utilisateur
     * en fonction des changements d'état de la lecture
     */
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            bound = true;
            
            // Ajouter le listener pour les changements de piste
            musicService.setPlaybackListener(new MusicService.OnPlaybackStateChangeListener() {
                @Override
                public void onPlaybackStateChanged(boolean playing) {
                    runOnUiThread(() -> {
                        MusicPlayerState.getInstance().setPlaying(playing);
                        updateMiniPlayer();
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Playback error: " + errorMessage);
                }

                @Override
                public void onTrackChanged(Track track) {
                    runOnUiThread(() -> {
                        MusicPlayerState.getInstance().setCurrentTrack(track);
                        updateMiniPlayer();
                    });
                }
            });
            
            // Mise à jour initiale
            updateMiniPlayer();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
            bound = false;
        }
    };

    /**
     * Initialise les composants de l'interface utilisateur
     * Configure la toolbar, le ViewPager, les onglets et le mini lecteur
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisation du Handler
        handler = new Handler(Looper.getMainLooper());

        // Initialisation de la Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(true);
            }
        }

        // Initialisation des vues
        initializeViews();
        
        // Initialisation du service de musique
        initializeMusicService();
        
        // Configuration des écouteurs d'événements
        setupEventListeners();
        
        // Chargement des données
        loadData();
    }

    /**
     * Initialise toutes les vues de l'interface utilisateur
     * Configure les listeners pour le mini lecteur et gère les erreurs potentielles
     */
    private void initializeViews() {
        try {
            // Initialisation des vues principales
            viewPager = findViewById(R.id.viewPager);
            tabLayout = findViewById(R.id.tabLayout);
            miniPlayerLayout = findViewById(R.id.miniPlayer);
            miniPlayerCover = findViewById(R.id.miniPlayerCoverImage);
            miniPlayerTitle = findViewById(R.id.miniPlayerTrackTitle);
            miniPlayerArtist = findViewById(R.id.miniPlayerArtistName);
            miniPlayerPlayPause = findViewById(R.id.miniPlayerPlayPauseButton);
            miniPlayerNext = findViewById(R.id.miniPlayerNextButton);

            // Configuration du mini player
            if (miniPlayerLayout != null) {
                miniPlayerLayout.setOnClickListener(v -> openFullPlayer());
                if (miniPlayerPlayPause != null) {
                    miniPlayerPlayPause.setOnClickListener(v -> togglePlayPause());
                }
                if (miniPlayerNext != null) {
                    miniPlayerNext.setOnClickListener(v -> playNextTrack());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during view initialization", e);
        }
    }

    /**
     * Démarre le vérificateur périodique de l'état du lecteur
     * Met à jour l'interface toutes les secondes pour refléter l'état actuel de la lecture
     */
    private void startPlayerStatusChecker() {
        if (handler != null) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (handler != null) {
                        checkAndUpdateMiniPlayer();
                        handler.postDelayed(this, 1000);
                    }
                }
            }, 1000);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (handler != null) {
            startPlayerStatusChecker();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * Initialise et démarre le service de musique
     * Établit la connexion avec le service pour la lecture audio
     */
    private void initializeMusicService() {
        // Démarrer et lier le service
        Intent intent = new Intent(this, MusicService.class);
        startService(intent);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Configure les écouteurs d'événements pour le ViewPager et les onglets
     * Gère la navigation entre les différentes sections de l'application
     */
    private void setupEventListeners() {
        // Configuration de l'adaptateur pour le ViewPager
        pagerAdapter = new MainPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        Log.d(TAG, "onCreate: ViewPager adapter set");

        // Ajout d'un listener pour détecter les changements d'onglets
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                Log.d(TAG, "onPageSelected: Page " + position + " selected");
                updateCurrentFragment();
            }
        });

        // Configuration des onglets avec TabLayoutMediator
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Accueil");
                    break;
                case 1:
                    tab.setText("Favoris");
                    break;
            }
        }).attach();
        Log.d(TAG, "onCreate: TabLayout configured");
    }

    /**
     * Charge les données des pistes depuis le serveur
     * Télécharge le fichier CSV contenant les informations des pistes
     */
    private void loadData() {
        // Téléchargement du CSV
        String csvUrl = "http://edu.info06.net/lyrics/lyrics.csv";
        CsvLoader.fetch(csvUrl, this::updateTracks);
        Log.d(TAG, "onCreate: CSV fetch initiated");
    }

    /**
     * Met à jour la liste des pistes dans l'application
     * Synchronise les données avec l'état global du lecteur
     */
    private void updateTracks(List<Track> tracks) {
        allTracks = tracks;
        MusicPlayerState.getInstance().setTrackList(tracks);
        updateAllFragments();
    }

    /**
     * Met à jour tous les fragments de l'application
     * Assure la cohérence des données affichées dans toutes les sections
     */
    private void updateAllFragments() {
        for (int i = 0; i < getSupportFragmentManager().getFragments().size(); i++) {
            if (getSupportFragmentManager().getFragments().get(i) instanceof TracksFragment) {
                ((TracksFragment) getSupportFragmentManager().getFragments().get(i))
                        .updateTracks(allTracks);
            }
        }
    }

    // Méthode pour mettre à jour uniquement le fragment actuellement visible
    private void updateCurrentFragment() {
        int currentPosition = viewPager.getCurrentItem();
        for (int i = 0; i < getSupportFragmentManager().getFragments().size(); i++) {
            if (getSupportFragmentManager().getFragments().get(i) instanceof TracksFragment &&
                    ((TracksFragment) getSupportFragmentManager().getFragments().get(i)).getFragmentPosition() == currentPosition) {
                ((TracksFragment) getSupportFragmentManager().getFragments().get(i))
                        .updateTracks(allTracks);
                break;
            }
        }
    }

    /**
     * Gère le clic sur une piste dans la liste
     * Démarre la lecture de la piste sélectionnée
     */
    @Override
    public void onTrackClick(Track track, int position) {
        // Lancement de PlayerActivity avec le morceau sélectionné
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_TRACK, track);
        intent.putParcelableArrayListExtra(PlayerActivity.EXTRA_TRACK_LIST, new ArrayList<>(allTracks));
        intent.putExtra(PlayerActivity.EXTRA_TRACK_POSITION, position);
        startActivity(intent);
        // Ajouter une animation de transition
        overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down);
    }

    /**
     * Gère les changements d'état des favoris
     * Met à jour l'interface utilisateur en fonction des modifications des favoris
     */
    @Override
    public void onTrackFavoriteChanged(Track track) {
        boolean isFavorite = MusicPlayerState.getInstance().isFavorite(track);
        if (isFavorite) {
            MusicPlayerState.getInstance().removeFromFavorites(track);
        } else {
            MusicPlayerState.getInstance().addToFavorites(track);
        }
        updateCurrentFragment();
    }

    /**
     * Passe à la piste suivante dans la liste de lecture
     * Gère la lecture en boucle et la fin de la liste
     */
    private void playNextTrack() {
        if (bound && musicService != null) {
            musicService.playNext();
            updateMiniPlayer();
        }
    }

    /**
     * Initialise le mini lecteur
     * Configure l'apparence et le comportement du lecteur compact
     */
    private void initializeMiniPlayer() {
        try {
            // Récupération des vues avec les IDs corrects
            miniPlayerLayout = findViewById(R.id.miniPlayer);
            if (miniPlayerLayout == null) {
                Log.e(TAG, "initializeMiniPlayer: miniPlayerLayout is null");
                return;
            }

            miniPlayerCover = findViewById(R.id.miniPlayerCoverImage);
            miniPlayerTitle = findViewById(R.id.miniPlayerTrackTitle);
            miniPlayerArtist = findViewById(R.id.miniPlayerArtistName);
            miniPlayerPlayPause = findViewById(R.id.miniPlayerPlayPauseButton);

            if (miniPlayerCover == null || miniPlayerTitle == null || 
                miniPlayerArtist == null || miniPlayerPlayPause == null) {
                Log.e(TAG, "initializeMiniPlayer: One or more mini player components are null");
                return;
            }

            // Configurer le clic sur le bouton play/pause
            miniPlayerPlayPause.setOnClickListener(v -> togglePlayPause());

            // Configurer le clic sur le mini lecteur pour ouvrir le lecteur complet
            miniPlayerLayout.setOnClickListener(v -> openFullPlayer());

            Log.d(TAG, "initializeMiniPlayer: Mini player initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "initializeMiniPlayer: Error initializing mini player", e);
        }
    }

    /**
     * Vérifie et met à jour l'état du mini lecteur
     * Synchronise l'interface avec l'état actuel de la lecture
     */
    private void checkAndUpdateMiniPlayer() {
        if (bound && musicService != null) {
            updateMiniPlayer();
        }
    }

    /**
     * Met à jour l'interface du mini lecteur
     * Affiche les informations de la piste en cours et l'état de lecture
     */
    private void updateMiniPlayer() {
        if (!bound || musicService == null) {
            return;
        }

        Track currentTrack = MusicPlayerState.getInstance().getCurrentTrack();
        if (currentTrack == null) {
            miniPlayerLayout.setVisibility(View.GONE);
            return;
        }

        // Afficher le mini lecteur
        miniPlayerLayout.setVisibility(View.VISIBLE);

        // Mettre à jour les informations textuelles
        miniPlayerTitle.setText(currentTrack.title);
        miniPlayerArtist.setText(currentTrack.artist);

        // Mettre à jour l'icône play/pause
        boolean isPlaying = musicService.isPlaying();
        miniPlayerPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);

        // Charger l'image de couverture
        if (currentTrack.coverUrl != null && !currentTrack.coverUrl.isEmpty()) {
            new LoadImageTask(miniPlayerCover).execute(currentTrack.coverUrl);
        } else {
            miniPlayerCover.setImageResource(R.drawable.placeholder_album);
        }

        Log.d(TAG, "Mini player updated - Track: " + currentTrack.title + ", Playing: " + isPlaying);
    }

    /**
     * Bascule entre la lecture et la pause
     * Gère l'état de lecture de la piste actuelle
     */
    private void togglePlayPause() {
        if (bound && musicService != null) {
            musicService.togglePlayPause();
            updateMiniPlayer();
        } else {
            // Si le service n'est pas lié, on lance PlayerActivity
            Track currentTrack = MusicPlayerState.getInstance().getCurrentTrack();
            if (currentTrack != null) {
                openFullPlayer();
            }
        }
    }

    /**
     * Ouvre le lecteur complet
     * Lance l'activité du lecteur détaillé
     */
    private void openFullPlayer() {
        Track currentTrack = MusicPlayerState.getInstance().getCurrentTrack();
        if (currentTrack != null) {
            Intent intent = new Intent(this, PlayerActivity.class);
            intent.putExtra(PlayerActivity.EXTRA_TRACK, currentTrack);
            intent.putExtra(PlayerActivity.EXTRA_TRACK_LIST, new ArrayList<>(MusicPlayerState.getInstance().getTrackList()));
            intent.putExtra(PlayerActivity.EXTRA_TRACK_POSITION, MusicPlayerState.getInstance().getCurrentTrackPosition());
            if (bound && musicService != null) {
                intent.putExtra(PlayerActivity.EXTRA_CURRENT_POSITION, musicService.getCurrentPosition());
            }
            startActivity(intent);
        }
    }

    /**
     * Tâche asynchrone pour le chargement des images
     * Gère le téléchargement et le cache des images de couverture
     */
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
                Log.e("MainActivity", "Erreur lors du chargement de l'image: " + e.getMessage());
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

    /**
     * Nettoie les ressources lors de la destruction de l'activité
     * Déconnecte le service et libère les ressources
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bound) {
            unbindService(connection);
            bound = false;
        }
    }
}
