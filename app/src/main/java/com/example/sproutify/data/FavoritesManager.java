package com.example.sproutify.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.sproutify.model.Track;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Gestionnaire de favoris pour sauvegarder et récupérer les morceaux likés
 */
public class FavoritesManager {
    private static final String TAG = "FavoritesManager";
    private static final String PREFS_NAME = "sproutify_favorites";
    private static final String KEY_FAVORITE_TRACKS = "favorite_tracks";

    private final SharedPreferences sharedPreferences;
    private final Gson gson;
    private final Set<String> favoriteTracks;

    private static FavoritesManager instance;

    public static synchronized FavoritesManager getInstance(Context context) {
        if (instance == null) {
            instance = new FavoritesManager(context.getApplicationContext());
        }
        return instance;
    }

    private FavoritesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        favoriteTracks = loadFavoriteTrackIds();
    }

    /**
     * Ajoute ou supprime un morceau des favoris
     * @param track Morceau à ajouter/supprimer
     * @return true si le morceau est désormais en favoris, false sinon
     */
    public boolean toggleFavorite(Track track) {
        String trackId = track.mp3Url; // Utilisation de l'URL comme identifiant unique

        if (favoriteTracks.contains(trackId)) {
            favoriteTracks.remove(trackId);
            saveFavoriteTrackIds();
            return false;
        } else {
            favoriteTracks.add(trackId);
            saveFavoriteTrackIds();
            return true;
        }
    }

    /**
     * Vérifie si un morceau est en favoris
     * @param track Morceau à vérifier
     * @return true si le morceau est en favoris
     */
    public boolean isFavorite(Track track) {
        return track != null && favoriteTracks.contains(track.mp3Url);
    }

    /**
     * Filtre une liste de morceaux pour ne garder que les favoris
     * @param allTracks Liste complète des morceaux
     * @return Liste des morceaux favoris uniquement
     */
    public List<Track> getFavoriteTracks(List<Track> allTracks) {
        List<Track> favorites = new ArrayList<>();

        for (Track track : allTracks) {
            if (isFavorite(track)) {
                favorites.add(track);
            }
        }

        return favorites;
    }

    private Set<String> loadFavoriteTrackIds() {
        String json = sharedPreferences.getString(KEY_FAVORITE_TRACKS, null);
        if (json != null) {
            try {
                Type type = new TypeToken<Set<String>>(){}.getType();
                return gson.fromJson(json, type);
            } catch (Exception e) {
                Log.e(TAG, "Error loading favorites", e);
            }
        }
        return new HashSet<>();
    }

    private void saveFavoriteTrackIds() {
        String json = gson.toJson(favoriteTracks);
        sharedPreferences.edit().putString(KEY_FAVORITE_TRACKS, json).apply();
    }
}
