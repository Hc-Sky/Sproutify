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
 * Gestionnaire de favoris pour sauvegarder et récupérer les morceaux likés.
 * Cette classe utilise le pattern Singleton pour assurer une instance unique
 * et utilise SharedPreferences pour la persistance des données.
 */
public class FavoritesManager {
    private static final String TAG = "FavoritesManager";
    private static final String PREFS_NAME = "sproutify_favorites";
    private static final String KEY_FAVORITE_TRACKS = "favorite_tracks";

    private final SharedPreferences sharedPreferences;
    private final Gson gson;
    private final Set<String> favoriteTracks;

    private static FavoritesManager instance;

    /**
     * Obtient l'instance unique du FavoritesManager (pattern Singleton).
     * Si l'instance n'existe pas, elle est créée avec le contexte fourni.
     * 
     * @param context Le contexte de l'application
     * @return L'instance unique de FavoritesManager
     */
    public static synchronized FavoritesManager getInstance(Context context) {
        if (instance == null) {
            instance = new FavoritesManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Constructeur privé du FavoritesManager.
     * Initialise les préférences partagées, le parser JSON et charge les favoris existants.
     * 
     * @param context Le contexte de l'application
     */
    private FavoritesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        favoriteTracks = loadFavoriteTrackIds();
    }

    /**
     * Bascule l'état d'un morceau dans les favoris.
     * Si le morceau est déjà en favoris, il est retiré, sinon il est ajouté.
     * 
     * @param track Le morceau à ajouter ou retirer des favoris
     * @return true si le morceau est maintenant en favoris, false s'il a été retiré
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
     * Vérifie si un morceau est présent dans les favoris.
     * 
     * @param track Le morceau à vérifier
     * @return true si le morceau est en favoris, false sinon
     */
    public boolean isFavorite(Track track) {
        return track != null && favoriteTracks.contains(track.mp3Url);
    }

    /**
     * Filtre une liste de morceaux pour ne conserver que ceux qui sont en favoris.
     * 
     * @param allTracks La liste complète des morceaux à filtrer
     * @return Une nouvelle liste contenant uniquement les morceaux favoris
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

    /**
     * Charge les identifiants des morceaux favoris depuis les préférences partagées.
     * En cas d'erreur de lecture, retourne un ensemble vide.
     * 
     * @return Un ensemble contenant les identifiants des morceaux favoris
     */
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

    /**
     * Sauvegarde les identifiants des morceaux favoris dans les préférences partagées.
     * La sauvegarde est effectuée de manière asynchrone.
     */
    private void saveFavoriteTrackIds() {
        String json = gson.toJson(favoriteTracks);
        sharedPreferences.edit().putString(KEY_FAVORITE_TRACKS, json).apply();
    }
}
