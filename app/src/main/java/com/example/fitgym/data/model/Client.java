package com.example.fitgym.data.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Client {

    private String id;
    private String nom;
    private String email;
    private String motDePasse;
    private String telephone;
    private boolean synced;
    private boolean googleSignIn;
    private String photoUrl; // si tu utilises les photos

    // Constructeurs
    public Client() {}

    public Client(String id, String nom, String email, String motDePasse, String telephone) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.telephone = telephone;
    }

    // Constructeur pour générer un id automatiquement
    public Client(String nom, String email, String motDePasse, String telephone, boolean generateId) {
        this.nom = nom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.telephone = telephone;
        if (generateId) {
            this.id = UUID.randomUUID().toString();
        }
    }

    // Convertir en Map (utile pour Firebase)
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("nom", nom);
        map.put("email", email);
        map.put("motDePasse", motDePasse);
        map.put("telephone", telephone);
        map.put("synced", synced);
        map.put("googleSignIn", googleSignIn);
        map.put("photoUrl", photoUrl);
        return map;
    }

    // GETTERS
    public String getId() { return id; }
    public String getNom() { return nom; }
    public String getEmail() { return email; }
    public String getMotDePasse() { return motDePasse; }
    public String getTelephone() { return telephone; }
    public boolean isSynced() { return synced; }
    public boolean isGoogleSignIn() { return googleSignIn; }
    public String getPhotoUrl() { return photoUrl; }

    // SETTERS
    public void setId(String id) { this.id = id; }
    public void setNom(String nom) { this.nom = nom; }
    public void setEmail(String email) { this.email = email; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public void setSynced(boolean synced) { this.synced = synced; }
    public void setGoogleSignIn(boolean googleSignIn) { this.googleSignIn = googleSignIn; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
}
