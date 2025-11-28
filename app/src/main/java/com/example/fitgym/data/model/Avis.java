package com.example.fitgym.data.model;

public class Avis {

    private String id;
    private String userId;
    private String nom;
    private String commentaire;
    private String date;
    private double stars;
    private String avatarUrl;
    private long timestamp;

    public Avis() {
    }

    public Avis(String id, String userId, String nom, String commentaire, String date, double stars, String avatarUrl, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.nom = nom;
        this.commentaire = commentaire;
        this.date = date;
        this.stars = stars;
        this.avatarUrl = avatarUrl;
        this.timestamp = timestamp;
    }

    // GETTERS ET SETTERS
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public double getStars() { return stars; }
    public void setStars(double stars) { this.stars = stars; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}