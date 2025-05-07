package com.example.sproutify.data;

import android.content.Context;
import com.example.sproutify.R;
import com.example.sproutify.model.Track;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class CsvLoader {

    private CsvLoader() { }

    public static List<Track> loadTracks(Context ctx) {
        List<Track> list = new ArrayList<>();

        try (InputStream is = ctx.getResources().openRawResource(R.raw.lyrics);
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String line;
            boolean skipHeader = true;
            while ((line = br.readLine()) != null) {
                if (skipHeader) { skipHeader = false; continue; }
                String[] parts = line.split("#", -1); // -1 pour garder les champs vides
                if (parts.length < 8) continue;       // sécurité

                list.add(new Track(
                        parts[0], // title
                        parts[1], // album
                        parts[2], // artist
                        parts[3], // date
                        parts[4], // cover
                        parts[5], // contentlines
                        parts[6], // mp3
                        parts[7]  // duration
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}
