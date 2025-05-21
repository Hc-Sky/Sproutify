package com.example.sproutify;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sproutify.data.CsvLoader;
import com.example.sproutify.model.Track;
import com.example.sproutify.ui.TrackAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TrackAdapter adapter;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSupportActionBar(findViewById(R.id.topAppBar));

        RecyclerView rv = findViewById(R.id.recyclerTracks);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setHasFixedSize(true);

        adapter = new TrackAdapter(this, new ArrayList<>(), (t, img) -> {
            // TODO : ouverture LyricsActivity
        });
        rv.setAdapter(adapter);

        // ---- Téléchargement du CSV ----
        String csvUrl = "http://edu.info06.net/lyrics/lyrics.csv";
        CsvLoader.fetch(csvUrl, tracks -> adapter.updateData(tracks));
    }
}
