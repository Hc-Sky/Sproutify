package com.example.sproutify;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sproutify.data.CsvLoader;
import com.example.sproutify.model.Track;
import com.example.sproutify.ui.TrackAdapter;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private TrackAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // AppBar
        MaterialToolbar bar = findViewById(R.id.topAppBar);
        setSupportActionBar(bar);

        // Liste
        recycler = findViewById(R.id.recyclerTracks);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setHasFixedSize(true);

        List<Track> tracks = CsvLoader.loadTracks(this);
        adapter = new TrackAdapter(this, tracks, (track, img) -> {
            // TODO : ouvrir LyricsActivity avec transition partagée + parcelable
        });
        recycler.setAdapter(adapter);
    }
}
