package com.example.sproutify.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Track implements Parcelable {
    public final String title, album, artist, date, coverUrl, contentLines, mp3Url, duration;

    public Track(String title, String album, String artist, String date,
                 String coverUrl, String contentLines, String mp3Url, String duration) {
        this.title = title;
        this.album = album;
        this.artist = artist;
        this.date = date;
        this.coverUrl = coverUrl;
        this.contentLines = contentLines;
        this.mp3Url = mp3Url;
        this.duration = duration;
    }

    /* ---- Parcelable ---- */
    protected Track(Parcel in) {
        title = in.readString();
        album = in.readString();
        artist = in.readString();
        date = in.readString();
        coverUrl = in.readString();
        contentLines = in.readString();
        mp3Url = in.readString();
        duration = in.readString();
    }

    public static final Creator<Track> CREATOR = new Creator<Track>() {
        @Override public Track createFromParcel(Parcel in) { return new Track(in); }
        @Override public Track[] newArray(int size) { return new Track[size]; }
    };

    @Override public int describeContents() { return 0; }
    @Override public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(album);
        dest.writeString(artist);
        dest.writeString(date);
        dest.writeString(coverUrl);
        dest.writeString(contentLines);
        dest.writeString(mp3Url);
        dest.writeString(duration);
    }
}
