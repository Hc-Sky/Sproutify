package com.example.sproutify.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sproutify.R;
import com.example.sproutify.data.FavoritesManager;
import com.example.sproutify.model.Track;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TracksFragment extends Fragment implements TrackAdapter.OnTrackFavoriteListener {

    private static final String ARG_SHOW_FAVORITES = "show_favorites";
    private static final int FILTER_ALL = 0;
    private static final int FILTER_TITLE = 1;
    private static final int FILTER_ARTIST = 2;
    private static final int FILTER_ALBUM = 3;

    private RecyclerView recyclerView;
    private TextView emptyView;
    private SearchView searchView;
    private ChipGroup filterChipGroup;
    private TrackAdapter adapter;
    private boolean showFavorites;
    private List<Track> allTracks = new ArrayList<>();
    private String currentQuery = "";
    private int currentFilter = FILTER_ALL;
    private boolean showUniqueItems = false;
    private String selectedItem = ""; // Pour stocker l'élément sélectionné (album ou artiste)

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
        searchView = view.findViewById(R.id.searchView);
        filterChipGroup = view.findViewById(R.id.filterChipGroup);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new TrackAdapter(getContext(), new ArrayList<>(),
                (track, position) -> {
                    if (showUniqueItems) {
                        // Si on est en mode unique (album ou artiste), on filtre la liste
                        String item = "";
                        switch (currentFilter) {
                            case FILTER_ALBUM:
                                item = track.album;
                                break;
                            case FILTER_ARTIST:
                                item = track.artist;
                                break;
                        }
                        if (!item.isEmpty()) {
                            selectedItem = item;
                            showUniqueItems = false;
                            adapter.setViewType(TrackAdapter.VIEW_TYPE_TRACK);
                            filterTracks();
                        }
                    } else if (getActivity() != null) {
                        ((TrackAdapter.OnTrackClickListener) getActivity()).onTrackClick(track, position);
                    }
                },
                this);
        recyclerView.setAdapter(adapter);

        // Configuration de la SearchView
        setupSearchView();

        // Configuration des filtres
        setupFilters();

        updateTracks(allTracks);

        return view;
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentQuery = newText.toLowerCase();
                filterTracks();
                return true;
            }
        });
    }

    private void setupFilters() {
        filterChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            selectedItem = ""; // Réinitialiser l'élément sélectionné lors du changement de filtre
            if (checkedId == R.id.filterAll) {
                currentFilter = FILTER_ALL;
                showUniqueItems = false;
                adapter.setViewType(TrackAdapter.VIEW_TYPE_TRACK);
            } else if (checkedId == R.id.filterTitle) {
                currentFilter = FILTER_TITLE;
                showUniqueItems = true;
                adapter.setViewType(TrackAdapter.VIEW_TYPE_TITLE);
            } else if (checkedId == R.id.filterArtist) {
                currentFilter = FILTER_ARTIST;
                showUniqueItems = true;
                adapter.setViewType(TrackAdapter.VIEW_TYPE_ARTIST);
            } else if (checkedId == R.id.filterAlbum) {
                currentFilter = FILTER_ALBUM;
                showUniqueItems = true;
                adapter.setViewType(TrackAdapter.VIEW_TYPE_ALBUM);
            }
            filterTracks();
        });
    }

    private void filterTracks() {
        List<Track> filteredTracks;

        if (showUniqueItems) {
            // Afficher les éléments uniques de la catégorie sélectionnée
            Set<String> uniqueItems = new HashSet<>();
            List<Track> uniqueTracks = new ArrayList<>();

            for (Track track : allTracks) {
                String item = "";
                switch (currentFilter) {
                    case FILTER_TITLE:
                        item = track.title;
                        break;
                    case FILTER_ARTIST:
                        item = track.artist;
                        break;
                    case FILTER_ALBUM:
                        item = track.album;
                        break;
                }

                // Vérifier si l'élément correspond à la recherche
                if (!item.isEmpty() && !uniqueItems.contains(item) && 
                    (currentQuery.isEmpty() || item.toLowerCase().contains(currentQuery))) {
                    uniqueItems.add(item);
                    uniqueTracks.add(track);
                }
            }

            filteredTracks = uniqueTracks;
        } else {
            // Filtrage normal
            filteredTracks = allTracks.stream()
                    .filter(track -> {
                        // Si un élément est sélectionné (album ou artiste), filtrer par cet élément
                        if (!selectedItem.isEmpty()) {
                            switch (currentFilter) {
                                case FILTER_ALBUM:
                                    if (!track.album.equals(selectedItem)) return false;
                                    break;
                                case FILTER_ARTIST:
                                    if (!track.artist.equals(selectedItem)) return false;
                                    break;
                            }
                        }

                        if (currentQuery.isEmpty()) {
                            return true;
                        }
                        switch (currentFilter) {
                            case FILTER_TITLE:
                                return track.title.toLowerCase().contains(currentQuery);
                            case FILTER_ARTIST:
                                return track.artist.toLowerCase().contains(currentQuery);
                            case FILTER_ALBUM:
                                return track.album.toLowerCase().contains(currentQuery);
                            default: // FILTER_ALL
                                return track.title.toLowerCase().contains(currentQuery) ||
                                       track.artist.toLowerCase().contains(currentQuery) ||
                                       track.album.toLowerCase().contains(currentQuery);
                        }
                    })
                    .collect(Collectors.toList());
        }

        if (showFavorites && getContext() != null) {
            filteredTracks = FavoritesManager.getInstance(getContext())
                    .getFavoriteTracks(filteredTracks);
        }

        adapter.updateData(filteredTracks);

        if (filteredTracks.isEmpty()) {
            String message;
            if (currentQuery.isEmpty()) {
                if (!selectedItem.isEmpty()) {
                    message = "Aucun morceau trouvé pour " + selectedItem;
                } else {
                    message = showFavorites ? "Aucun favori" : "Aucun morceau trouvé";
                }
            } else {
                String filterType = "";
                switch (currentFilter) {
                    case FILTER_TITLE:
                        filterType = "titre";
                        break;
                    case FILTER_ARTIST:
                        filterType = "artiste";
                        break;
                    case FILTER_ALBUM:
                        filterType = "album";
                        break;
                }
                message = "Aucun résultat pour \"" + currentQuery + "\"" + 
                         (currentFilter != FILTER_ALL ? " dans les " + filterType + "s" : "");
            }
            emptyView.setText(message);
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    public void updateTracks(List<Track> tracks) {
        allTracks = tracks;
        filterTracks();
    }

    @Override
    public void onTrackFavoriteChanged(Track track) {
        // Mettre à jour la liste quand un favori change (surtout important pour l'onglet favoris)
        updateTracks(allTracks);
    }

    /**
     * Retourne la position du fragment (0 pour Accueil, 1 pour Favoris)
     * @return position du fragment
     */
    public int getFragmentPosition() {
        return showFavorites ? 1 : 0;
    }
}
