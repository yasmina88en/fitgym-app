package com.example.fitgym.data.model;


public class Favoris {

    private String id;         // id du favoris (UUID ou clé Firebase)
    private String clientId;   // id du client
    private String seanceId;   // id de la séance

    public Favoris() { }

    public Favoris(String id, String clientId, String seanceId) {
        this.id = id;
        this.clientId = clientId;
        this.seanceId = seanceId;
    }

    // Getters et Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getSeanceId() {
        return seanceId;
    }

    public void setSeanceId(String seanceId) {
        this.seanceId = seanceId;
    }
}

