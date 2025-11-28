package com.example.fitgym.util;

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
 * Utilitaire pour synchroniser les données Firebase vers SQLite
 * pour permettre le mode hors ligne
 */
public class OfflineSyncHelper {

    private static final String TAG = "OfflineSyncHelper";
    private DatabaseHelper dbHelper;
    private FirebaseDatabase firebaseDb;

    public OfflineSyncHelper(Context context) {
        this.dbHelper = new DatabaseHelper(context);
        this.firebaseDb = FirebaseDatabase.getInstance();
    }

    /**
     * Synchronise tous les coachs depuis Firebase vers SQLite
     */
    public void syncCoaches(final SyncCallback callback) {
        firebaseDb.getReference("coachs")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int count = 0;
                        for (DataSnapshot coachSnapshot : snapshot.getChildren()) {
                            Coach coach = coachSnapshot.getValue(Coach.class);
                            if (coach != null) {
                                coach.setId(coachSnapshot.getKey());
                                if (dbHelper.syncCoach(coach)) {
                                    count++;
                                }
                            }
                        }
                        Log.d(TAG, "Synchronisé " + count + " coachs");
                        if (callback != null) {
                            callback.onSuccess("Synchronisé " + count + " coachs");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Erreur sync coachs: " + error.getMessage());
                        if (callback != null) {
                            callback.onError(error.getMessage());
                        }
                    }
                });
    }

    /**
     * Synchronise toutes les séances depuis Firebase vers SQLite
     */
    public void syncSeances(final SyncCallback callback) {
        firebaseDb.getReference("seances")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int count = 0;
                        for (DataSnapshot seanceSnapshot : snapshot.getChildren()) {
                            Seance seance = seanceSnapshot.getValue(Seance.class);
                            if (seance != null) {
                                seance.setId(seanceSnapshot.getKey());
                                if (dbHelper.insertOrUpdateSeance(seance)) {
                                    count++;
                                }
                            }
                        }
                        Log.d(TAG, "Synchronisé " + count + " séances");
                        if (callback != null) {
                            callback.onSuccess("Synchronisé " + count + " séances");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Erreur sync séances: " + error.getMessage());
                        if (callback != null) {
                            callback.onError(error.getMessage());
                        }
                    }
                });
    }

    /**
     * Synchronise toutes les catégories depuis Firebase vers SQLite
     */
    public void syncCategories(final SyncCallback callback) {
        firebaseDb.getReference("categories")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int count = 0;
                        for (DataSnapshot catSnapshot : snapshot.getChildren()) {
                            Categorie categorie = catSnapshot.getValue(Categorie.class);
                            if (categorie != null) {
                                categorie.setCategorieId(catSnapshot.getKey());
                                if (dbHelper.insertOrUpdateCategorie(categorie)) {
                                    count++;
                                }
                            }
                        }
                        Log.d(TAG, "Synchronisé " + count + " catégories");
                        if (callback != null) {
                            callback.onSuccess("Synchronisé " + count + " catégories");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Erreur sync catégories: " + error.getMessage());
                        if (callback != null) {
                            callback.onError(error.getMessage());
                        }
                    }
                });
    }

    /**
     * Synchronise toutes les données (coachs, séances, catégories)
     */
    public void syncAll(final SyncCallback callback) {
        final List<String> results = new ArrayList<>();
        final List<String> errors = new ArrayList<>();

        // Sync coachs
        syncCoaches(new SyncCallback() {
            @Override
            public void onSuccess(String message) {
                results.add(message);
                checkComplete();
            }

            @Override
            public void onError(String error) {
                errors.add("Coachs: " + error);
                checkComplete();
            }

            private void checkComplete() {
                if (results.size() + errors.size() >= 1) {
                    syncSeances(new SyncCallback() {
                        @Override
                        public void onSuccess(String message) {
                            results.add(message);
                            syncCategories(new SyncCallback() {
                                @Override
                                public void onSuccess(String message) {
                                    results.add(message);
                                    if (callback != null) {
                                        callback.onSuccess("Synchronisation complète: " + String.join(", ", results));
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                    errors.add("Catégories: " + error);
                                    if (callback != null) {
                                        callback.onError("Erreurs: " + String.join(", ", errors));
                                    }
                                }
                            });
                        }

                        @Override
                        public void onError(String error) {
                            errors.add("Séances: " + error);
                            if (callback != null) {
                                callback.onError("Erreurs: " + String.join(", ", errors));
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * Interface de callback pour la synchronisation
     */
    public interface SyncCallback {
        void onSuccess(String message);

        void onError(String error);
    }
}
