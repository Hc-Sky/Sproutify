package com.example.sproutify.data;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.sproutify.model.Track;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Télécharge et parse le CSV distant.
 * Utilisation :
 * CsvLoader.fetch("http://edu.info06.net/lyrics/lyrics.csv", list -> { ... });
 */
public final class CsvLoader {

    private static final String TAG = "CsvLoader";

    /**
     * Interface de callback pour notifier le chargement terminé
     * Permet de récupérer la liste des pistes une fois le CSV chargé
     */
    public interface OnCsvLoaded {
        void onResult(List<Track> tracks);
    }

    // Client HTTP pour les requêtes réseau
    private static final OkHttpClient CLIENT = new OkHttpClient();
    // URLs de base pour les ressources
    private static final String BASE_IMG = "http://edu.info06.net/lyrics/images/";
    private static final String BASE_MP3 = "http://edu.info06.net/lyrics/mp3/";

    /**
     * Constructeur privé pour empêcher l'instanciation
     * La classe est utilisée uniquement via ses méthodes statiques
     */
    private CsvLoader() { }

    /**
     * Télécharge et parse le fichier CSV distant
     * @param url URL du fichier CSV à télécharger
     * @param callback Callback appelé avec la liste des pistes une fois le chargement terminé
     */
    public static void fetch(String url, OnCsvLoaded callback) {

        Request req = new Request.Builder().url(url).build();

        CLIENT.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                post(callback, new ArrayList<>());            // renvoie liste vide
            }

            @Override public void onResponse(Call call, Response resp) throws IOException {
                if (!resp.isSuccessful() || resp.body() == null) {
                    post(callback, new ArrayList<>());
                    return;
                }

                String csv = new String(resp.body().bytes(), StandardCharsets.UTF_8);
                List<Track> list = parse(csv);
                post(callback, list);
            }
        });
    }

    /**
     * Parse le contenu CSV brut en liste d'objets Track
     * @param csv Contenu brut du fichier CSV
     * @return Liste des pistes extraites du CSV
     */
    private static List<Track> parse(String csv) {
        List<Track> tracks = new ArrayList<>();
        String[] lines = csv.split("\n");
        for (int i = 1; i < lines.length; i++) { // ignore header
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            String[] p = line.split("#", -1);
            if (p.length < 8) continue;

            String coverUrl = BASE_IMG + p[4];

            // Construction de l'URL MP3 complète avec le nom du fichier (pas l'ID)
            String mp3Url = BASE_MP3 + p[6];

            // Log de l'URL pour débogage
            Log.d(TAG, "Création d'une piste avec URL MP3: " + mp3Url);

            tracks.add(new Track(
                    String.valueOf(i),  // id (using line number as ID)
                    p[0],              // title
                    p[1],              // album
                    p[2],              // artist
                    p[3],              // date
                    coverUrl,          // cover (URL complète)
                    p[5],              // contentLines
                    mp3Url,            // mp3 (URL complète)
                    p[7]               // duration
            ));
        }
        return tracks;
    }

    /**
     * Poste le résultat sur le thread principal
     * @param cb Callback à appeler
     * @param data Données à passer au callback
     */
    private static void post(OnCsvLoaded cb, List<Track> data) {
        new Handler(Looper.getMainLooper()).post(() -> cb.onResult(data));
    }
}
