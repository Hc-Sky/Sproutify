package com.example.sproutify;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.sproutify.data.CsvLoader;
import com.example.sproutify.model.Track;
import com.example.sproutify.ui.MainPagerAdapter;
import com.example.sproutify.ui.TrackAdapter;
import com.example.sproutify.ui.TracksFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TrackAdapter.OnTrackClickListener {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private MainPagerAdapter pagerAdapter;
    private List<Track> allTracks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configuration de la barre d'outils
        setSupportActionBar(findViewById(R.id.topAppBar));

        // Initialisation des composants UI
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);

        // Configuration de l'adaptateur pour le ViewPager
        pagerAdapter = new MainPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Ajout d'un listener pour détecter les changements d'onglets
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // Mise à jour de l'onglet sélectionné (important pour l'onglet favoris)
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

        // Téléchargement du CSV
        String csvUrl = "http://edu.info06.net/lyrics/lyrics.csv";
        CsvLoader.fetch(csvUrl, this::updateTracks);
    }

    private void updateTracks(List<Track> tracks) {
        allTracks = tracks;
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
    protected void onResume() {
        super.onResume();

        // Mettre à jour les fragments car l'état des favoris peut avoir changé
        if (allTracks != null && !allTracks.isEmpty()) {
            updateCurrentFragment();
        }
    }
}
