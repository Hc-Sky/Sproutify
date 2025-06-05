package com.example.sproutify.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Classe représentant un morceau de musique dans l'application.
 * Implémente Parcelable pour permettre le passage de l'objet entre les activités.
 */
public class Track implements Parcelable {
    /** Identifiant unique du morceau */
    public final String id;
    /** Titre du morceau */
    public final String title;
    /** Nom de l'album */
    public final String album;
    /** Nom de l'artiste */
    public final String artist;
    /** Date de sortie */
    public final String date;
    /** URL de la pochette d'album */
    public final String coverUrl;
    /** Paroles du morceau */
    public final String contentLines;
    /** URL du fichier MP3 */
    public final String mp3Url;
    /** Durée du morceau */
    public final String duration;

    /**
     * Constructeur principal de la classe Track.
     * 
     * @param id Identifiant unique du morceau
     * @param title Titre du morceau
     * @param album Nom de l'album
     * @param artist Nom de l'artiste
     * @param date Date de sortie
     * @param coverUrl URL de la pochette d'album
     * @param contentLines Paroles du morceau
     * @param mp3Url URL du fichier MP3
     * @param duration Durée du morceau
     */
    public Track(String id, String title, String album, String artist, String date,
                 String coverUrl, String contentLines, String mp3Url, String duration) {
        this.id = id;
        this.title = title;
        this.album = album;
        this.artist = artist;
        this.date = date;
        this.coverUrl = coverUrl;
        this.contentLines = contentLines;
        this.mp3Url = mp3Url;
        this.duration = duration;
    }

    /**
     * Constructeur utilisé pour la désérialisation d'un Parcel.
     * 
     * @param in Parcel contenant les données du morceau
     */
    protected Track(Parcel in) {
        id = in.readString();
        title = in.readString();
        album = in.readString();
        artist = in.readString();
        date = in.readString();
        coverUrl = in.readString();
        contentLines = in.readString();
        mp3Url = in.readString();
        duration = in.readString();
    }

    /**
     * Créateur utilisé pour la désérialisation des objets Track.
     */
    public static final Creator<Track> CREATOR = new Creator<Track>() {
        /**
         * Crée une nouvelle instance de Track à partir d'un Parcel.
         * 
         * @param in Parcel contenant les données du morceau
         * @return Nouvelle instance de Track
         */
        @Override public Track createFromParcel(Parcel in) { return new Track(in); }

        /**
         * Crée un tableau de Track de la taille spécifiée.
         * 
         * @param size Taille du tableau à créer
         * @return Nouveau tableau de Track
         */
        @Override public Track[] newArray(int size) { return new Track[size]; }
    };

    /**
     * Décrit le contenu spécial du Parcelable.
     * 
     * @return 0 car il n'y a pas de contenu spécial
     */
    @Override public int describeContents() { return 0; }

    /**
     * Écrit les données du Track dans un Parcel.
     * 
     * @param dest Parcel dans lequel écrire les données
     * @param flags Flags supplémentaires pour la sérialisation
     */
    @Override public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
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
