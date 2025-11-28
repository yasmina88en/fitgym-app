package com.example.fitgym.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.fitgym.data.db.DatabaseHelper;
import com.example.fitgym.data.model.Categorie;
import com.example.fitgym.data.model.Coach;
import com.example.fitgym.data.model.Seance;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository hybride qui gère le stockage dans Firebase ET SQLite
 * Garantit le mode hors ligne en synchronisant automatiquement les données
 */
public class DataRepository {

    private static final String TAG = "DataRepository";
    private final DatabaseHelper dbHelper;
    private final FirebaseDatabase firebaseDb;
    private static DataRepository instance;

    private DataRepository(Context context) {
        this.dbHelper = new DatabaseHelper(context.getApplicationContext());
        this.firebaseDb = FirebaseDatabase.getInstance();
    }

    public static synchronized DataRepository getInstance(Context context) {
        if (instance == null) {
            instance = new DataRepository(context);
        }
        return instance;
    }

    // ==================== SEANCES ====================

    /**
     * Charge les séances : d'abord depuis SQLite (rapide), puis synchronise avec
     * Firebase
     */
    public void getSeances(DataCallback<List<Seance>> callback) {
        // 1. Charger d'abord depuis SQLite (données locales - instantané)
        List<Seance> localSeances = dbHelper.getAllSeances();

        if (!localSeances.isEmpty()) {
            Log.d(TAG, "✅ Chargé " + localSeances.size() + " séances depuis SQLite");
            callback.onSuccess(localSeances);
        }

        // 2. Synchroniser avec Firebase en arrière-plan
        firebaseDb.getReference("seances")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Seance> firebaseSeances = new ArrayList<>();

                        for (DataSnapshot seanceSnapshot : snapshot.getChildren()) {
                            Seance seance = seanceSnapshot.getValue(Seance.class);
                            if (seance != null) {
                                seance.setId(seanceSnapshot.getKey());
                                firebaseSeances.add(seance);

                                // Sauvegarder automatiquement dans SQLite
                                dbHelper.insertOrUpdateSeance(seance);
                            }
                        }

                        Log.d(TAG, "🔄 Synchronisé " + firebaseSeances.size() + " séances depuis Firebase");

                        // Retourner les données Firebase (plus récentes)
                        if (!firebaseSeances.isEmpty()) {
                            callback.onSuccess(firebaseSeances);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "❌ Erreur Firebase séances: " + error.getMessage());

                        // En cas d'erreur Firebase, utiliser les données SQLite
                        if (localSeances.isEmpty()) {
                            callback.onError("Aucune donnée disponible. Connectez-vous à Internet.");
                        }
                        // Sinon, on a déjà retourné les données locales
                    }
                });
    }

    /**
     * Récupère une séance par ID (SQLite d'abord, puis Firebase)
     */
    public void getSeanceById(String id, DataCallback<Seance> callback) {
        // Charger depuis SQLite
        Seance localSeance = dbHelper.getSeanceById(id);

        if (localSeance != null) {
            callback.onSuccess(localSeance);
        }

        // Synchroniser avec Firebase
        firebaseDb.getReference("seances").child(id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Seance seance = snapshot.getValue(Seance.class);
                        if (seance != null) {
                            seance.setId(snapshot.getKey());
                            dbHelper.insertOrUpdateSeance(seance);
                            callback.onSuccess(seance);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (localSeance == null) {
                            callback.onError("Séance non trouvée");
                        }
                    }
                });
    }

    // ==================== COACHES ====================

    /**
     * Charge les coaches : d'abord depuis SQLite, puis synchronise avec Firebase
     */
    public void getCoaches(DataCallback<List<Coach>> callback) {
        // 1. Charger depuis SQLite
        List<Coach> localCoaches = dbHelper.getAllCoaches();

        if (!localCoaches.isEmpty()) {
            Log.d(TAG, "✅ Chargé " + localCoaches.size() + " coaches depuis SQLite");
            callback.onSuccess(localCoaches);
        }

        // 2. Synchroniser avec Firebase
        firebaseDb.getReference("coachs")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Coach> firebaseCoaches = new ArrayList<>();

                        for (DataSnapshot coachSnapshot : snapshot.getChildren()) {
                            Coach coach = coachSnapshot.getValue(Coach.class);
                            if (coach != null) {
                                coach.setId(coachSnapshot.getKey());
                                firebaseCoaches.add(coach);

                                // Sauvegarder dans SQLite
                                dbHelper.syncCoach(coach);
                            }
                        }

                        Log.d(TAG, "🔄 Synchronisé " + firebaseCoaches.size() + " coaches depuis Firebase");

                        if (!firebaseCoaches.isEmpty()) {
                            callback.onSuccess(firebaseCoaches);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "❌ Erreur Firebase coaches: " + error.getMessage());

                        if (localCoaches.isEmpty()) {
                            callback.onError("Aucune donnée disponible. Connectez-vous à Internet.");
                        }
                    }
                });
    }

    /**
     * Récupère un coach par ID
     */
    public void getCoachById(String id, DataCallback<Coach> callback) {
        // Charger depuis SQLite
        Coach localCoach = dbHelper.getCoachById(id);

        if (localCoach != null) {
            callback.onSuccess(localCoach);
        }

        // Synchroniser avec Firebase
        firebaseDb.getReference("coachs").child(id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Coach coach = snapshot.getValue(Coach.class);
                        if (coach != null) {
                            coach.setId(snapshot.getKey());
                            dbHelper.syncCoach(coach);
                            callback.onSuccess(coach);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (localCoach == null) {
                            callback.onError("Coach non trouvé");
                        }
                    }
                });
    }

    // ==================== CATEGORIES ====================

    /**
     * Charge les catégories : SQLite d'abord, puis Firebase
     */
    public void getCategories(DataCallback<List<Categorie>> callback) {
        // 1. Charger depuis SQLite
        List<Categorie> localCategories = dbHelper.getAllCategories();

        if (!localCategories.isEmpty()) {
            Log.d(TAG, "✅ Chargé " + localCategories.size() + " catégories depuis SQLite");
            callback.onSuccess(localCategories);
        }

        // 2. Synchroniser avec Firebase
        firebaseDb.getReference("categories")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Categorie> firebaseCategories = new ArrayList<>();

                        for (DataSnapshot catSnapshot : snapshot.getChildren()) {
                            Categorie categorie = catSnapshot.getValue(Categorie.class);
                            if (categorie != null) {
                                categorie.setCategorieId(catSnapshot.getKey());
                                firebaseCategories.add(categorie);

                                // Sauvegarder dans SQLite
                                dbHelper.insertOrUpdateCategorie(categorie);
                            }
                        }

                        Log.d(TAG, "🔄 Synchronisé " + firebaseCategories.size() + " catégories depuis Firebase");

                        if (!firebaseCategories.isEmpty()) {
                            callback.onSuccess(firebaseCategories);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "❌ Erreur Firebase catégories: " + error.getMessage());

                        if (localCategories.isEmpty()) {
                            callback.onError("Aucune donnée disponible.");
                        }
                    }
                });
    }

    /**
     * Récupère une catégorie par ID
     */
    public void getCategorieById(String id, DataCallback<Categorie> callback) {
        Categorie localCategorie = dbHelper.getCategorieById(id);

        if (localCategorie != null) {
            callback.onSuccess(localCategorie);
        }

        firebaseDb.getReference("categories").child(id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Categorie categorie = snapshot.getValue(Categorie.class);
                        if (categorie != null) {
                            categorie.setCategorieId(snapshot.getKey());
                            dbHelper.insertOrUpdateCategorie(categorie);
                            callback.onSuccess(categorie);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (localCategorie == null) {
                            callback.onError("Catégorie non trouvée");
                        }
                    }
                });
    }

    // ==================== CALLBACKS ====================

    public interface DataCallback<T> {
        void onSuccess(T data);

        void onError(String error);
    }
}
