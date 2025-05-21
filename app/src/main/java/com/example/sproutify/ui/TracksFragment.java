package com.example.sproutify.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sproutify.R;
import com.example.sproutify.data.FavoritesManager;
import com.example.sproutify.model.Track;

import java.util.ArrayList;
import java.util.List;

public class TracksFragment extends Fragment implements TrackAdapter.OnTrackFavoriteListener {

    private static final String ARG_SHOW_FAVORITES = "show_favorites";

    private RecyclerView recyclerView;
    private TextView emptyView;
    private TrackAdapter adapter;
    private boolean showFavorites;
    private List<Track> allTracks = new ArrayList<>();

    public TracksFragment() {
        // Required empty public constructor
    }

    public static TracksFragment newInstance(boolean showFavorites) {
        TracksFragment fragment = new TracksFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_SHOW_FAVORITES, showFavorites);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            showFavorites = getArguments().getBoolean(ARG_SHOW_FAVORITES);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tracks, container, false);

        recyclerView = view.findViewById(R.id.recyclerTracks);
        emptyView = view.findViewById(R.id.emptyView);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new TrackAdapter(getContext(), new ArrayList<>(),
                (track, position) -> {
                    if (getActivity() != null) {
                        ((TrackAdapter.OnTrackClickListener) getActivity()).onTrackClick(track, position);
                    }
                },
                this);
        recyclerView.setAdapter(adapter);

        updateTracks(allTracks);

        return view;
    }

    public void updateTracks(List<Track> tracks) {
        allTracks = tracks;
        if (showFavorites && getContext() != null) {
            // En mode favoris, filtrer pour ne montrer que les favoris
            List<Track> favorites = FavoritesManager.getInstance(getContext())
                    .getFavoriteTracks(tracks);
            adapter.updateData(favorites);

            // Afficher un message si aucun favori
            if (favorites.isEmpty()) {
                emptyView.setText("Aucun favori");
                emptyView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        } else {
            // En mode normal, montrer tous les morceaux
            adapter.updateData(tracks);

            if (tracks.isEmpty()) {
                emptyView.setText("Aucun morceau trouvé");
                emptyView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onTrackFavoriteChanged(Track track) {
        // Mettre à jour la liste quand un favori change (surtout important pour l'onglet favoris)
        updateTracks(allTracks);
    }
}
