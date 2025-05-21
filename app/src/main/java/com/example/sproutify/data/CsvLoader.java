package com.example.sproutify.data;

import android.os.Handler;
import android.os.Looper;

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
 * Utilisation :
 * CsvLoader.fetch("http://edu.info06.net/lyrics/lyrics.csv", list -> { ... });
 */
public final class CsvLoader {

    public interface OnCsvLoaded {
        void onResult(List<Track> tracks);
    }

    private static final OkHttpClient CLIENT = new OkHttpClient();
    private static final String BASE_IMG = "http://edu.info06.net/lyrics/images/";

    private CsvLoader() { }

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

    /* Parse CSV brut ; délimiteur #, première ligne = header */
    private static List<Track> parse(String csv) {
        List<Track> tracks = new ArrayList<>();
        String[] lines = csv.split("\n");
        for (int i = 1; i < lines.length; i++) { // ignore header
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            String[] p = line.split("#", -1);
            if (p.length < 8) continue;

            String coverUrl = BASE_IMG + p[4];

            tracks.add(new Track(
                    p[0],         // title
                    p[1],         // album
                    p[2],         // artist
                    p[3],         // date
                    coverUrl,     // cover (URL complète)
                    p[5],         // contentLines
                    p[6],         // mp3
                    p[7]          // duration
            ));
        }
        return tracks;
    }

    /* Retour sur le thread principal */
    private static void post(OnCsvLoaded cb, List<Track> data) {
        new Handler(Looper.getMainLooper()).post(() -> cb.onResult(data));
    }
}
