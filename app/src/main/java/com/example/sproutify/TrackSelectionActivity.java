package com.example.sproutify;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sproutify.data.MusicPlayerState;
import com.example.sproutify.model.Track;
import com.example.sproutify.ui.TrackAdapter;

import java.util.List;

/**
 * Activité de sélection de piste
 * Permet à l'utilisateur de sélectionner une piste à ajouter à la file d'attente
 * ou à jouer immédiatement
 */
public class TrackSelectionActivity extends AppCompatActivity implements TrackAdapter.OnTrackClickListener {
    private RecyclerView recyclerView;
    private TrackAdapter adapter;
    private List<Track> allTracks;

    /**
     * Initialise l'activité de sélection
     * Configure la toolbar et la liste des pistes disponibles
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_selection);

        // Configuration de la toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Sélectionner une piste");

        // Récupération de toutes les pistes
        allTracks = MusicPlayerState.getInstance().getTrackList();

        // Configuration du RecyclerView
        recyclerView = findViewById(R.id.tracksRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new TrackAdapter(this, allTracks, this, null);
        recyclerView.setAdapter(adapter);
    }

    /**
     * Gère le clic sur une piste dans la liste
     * Retourne la piste sélectionnée à l'activité appelante
     */
    @Override
    public void onTrackClick(Track track, int position) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("selected_track", track);
        resultIntent.putExtra("addAfterCurrent", getIntent().getBooleanExtra("addAfterCurrent", false));
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    /**
     * Gère les actions de la toolbar
     * Permet de revenir à l'activité précédente
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 