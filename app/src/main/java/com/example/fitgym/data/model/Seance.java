package com.example.fitgym.data.model;

public class Seance {

    private String id;
    private String titre;
    private String categorieId;
    private String coachId;
    private String imageUrl;

    private String niveau;
    private String date;
    private String heure;
    private int duree;
    private double prix;
    private int placesTotales;
    private int placesDisponibles;
    private String description;

    public Seance() {
    } // pour Firebase

    public Seance(String id, String titre, String categorieId, String niveau, String date, String heure,
            int duree, double prix, int placesTotales, int placesDisponibles, String description, String coachId) {
        this.id = id;
        this.titre = titre;
        this.niveau = niveau;
        this.date = date;
        this.heure = heure;
        this.duree = duree;
        this.prix = prix;
        this.placesTotales = placesTotales;
        this.placesDisponibles = placesDisponibles;
        this.description = description;
        this.coachId = coachId;
        this.categorieId = categorieId;

    }

    // Getters & Setters
    // Getters & Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getCategorieId() {
        return categorieId;
    }

    public void setCategorieId(String categorieId) {
        this.categorieId = categorieId;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getHeure() {
        return heure;
    }

    public void setHeure(String heure) {
        this.heure = heure;
    }

    public int getDuree() {
        return duree;
    }

    public void setDuree(int duree) {
        this.duree = duree;
    }

    public double getPrix() {
        return prix;
    }

    public void setPrix(double prix) {
        this.prix = prix;
    }

    public int getPlacesTotales() {
        return placesTotales;
    }

    public void setPlacesTotales(int placesTotales) {
        this.placesTotales = placesTotales;
    }

    public int getPlacesDisponibles() {
        return placesDisponibles;
    }

    public void setPlacesDisponibles(int placesDisponibles) {
        this.placesDisponibles = placesDisponibles;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoachId() {
        return coachId;
    }

    public void setCoachId(String coachId) {
        this.coachId = coachId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

}
