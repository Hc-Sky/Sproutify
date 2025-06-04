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

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            bound = true;
            updateMiniPlayer();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
            bound = false;
        }
    };

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

    private void initializeMusicService() {
        // Démarrer et lier le service
        Intent intent = new Intent(this, MusicService.class);
        startService(intent);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

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

    private void loadData() {
        // Téléchargement du CSV
        String csvUrl = "http://edu.info06.net/lyrics/lyrics.csv";
        CsvLoader.fetch(csvUrl, this::updateTracks);
        Log.d(TAG, "onCreate: CSV fetch initiated");
    }

    private void updateTracks(List<Track> tracks) {
        allTracks = tracks;
        MusicPlayerState.getInstance().setTrackList(tracks);
        updateAllFragments();
    }

    // Méthode pour mettre à jour tous les fragments
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

    private void playNextTrack() {
        if (bound && musicService != null) {
            musicService.playNext();
            updateMiniPlayer();
        }
    }

    /**
     * Initialise le mini lecteur audio
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
     * Vérifie s'il y a une musique en cours et met à jour le mini lecteur
     */
    private void checkAndUpdateMiniPlayer() {
        try {
            MusicPlayerState playerState = MusicPlayerState.getInstance();

            if (miniPlayerLayout == null) {
                Log.e(TAG, "checkAndUpdateMiniPlayer: miniPlayerLayout is null");
                return;
            }

            if (playerState.hasTrack()) {
                // Il y a une piste en cours, afficher le mini lecteur
                updateMiniPlayerUI(playerState.getCurrentTrack(), playerState.isPlaying());
                miniPlayerLayout.setVisibility(View.VISIBLE);
            } else {
                // Pas de piste en cours, cacher le mini lecteur
                miniPlayerLayout.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "checkAndUpdateMiniPlayer: Error updating mini player", e);
        }
    }

    /**
     * Met à jour l'interface du mini lecteur
     */
    private void updateMiniPlayerUI(Track track, boolean isPlaying) {
        // Mettre à jour les informations textuelles
        miniPlayerTitle.setText(track.title);
        miniPlayerArtist.setText(track.artist);

        // Mettre à jour l'icône du bouton play/pause
        if (isPlaying) {
            miniPlayerPlayPause.setImageResource(R.drawable.ic_pause);
        } else {
            miniPlayerPlayPause.setImageResource(R.drawable.ic_play);
        }

        // Charger l'image de couverture
        if (track.coverUrl != null && !track.coverUrl.isEmpty()) {
            new LoadImageTask(miniPlayerCover).execute(track.coverUrl);
        } else {
            miniPlayerCover.setImageResource(R.drawable.placeholder_album);
        }
    }

    /**
     * Alterne entre lecture et pause
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

    private void updateMiniPlayer() {
        if (bound && musicService != null) {
            Track currentTrack = MusicPlayerState.getInstance().getCurrentTrack();
            if (currentTrack != null) {
                miniPlayerLayout.setVisibility(View.VISIBLE);
                miniPlayerTitle.setText(currentTrack.title);
                miniPlayerArtist.setText(currentTrack.artist);
                miniPlayerPlayPause.setImageResource(
                    musicService.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play
                );
            } else {
                miniPlayerLayout.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Ouvre le lecteur audio complet avec la piste en cours
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
     * Classe AsyncTask pour charger des images à partir d'URLs
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bound) {
            unbindService(connection);
            bound = false;
        }
    }
}
