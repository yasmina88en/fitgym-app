package com.example.fitgym;

import android.app.Application;
import android.util.Log;

import com.example.fitgym.util.OfflineSyncHelper;
import com.google.firebase.database.FirebaseDatabase;

public class FitGymApplication extends Application {

    private static final String TAG = "FitGymApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // Activer la persistance hors ligne pour Firebase Realtime Database
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        // Garder les données synchronisées même quand l'app n'est pas active
        // Cela permet de garder les coachs, séances, etc. en cache
        FirebaseDatabase.getInstance().getReference("coachs").keepSynced(true);
        FirebaseDatabase.getInstance().getReference("seances").keepSynced(true);
        FirebaseDatabase.getInstance().getReference("categories").keepSynced(true);
        FirebaseDatabase.getInstance().getReference("clients").keepSynced(true);

        // Synchroniser les données Firebase vers SQLite pour le mode hors ligne complet
        syncDataToLocal();
    }

    /**
     * Synchronise les données Firebase vers la base de données locale SQLite
     * Cela permet d'avoir un accès complet aux données même sans connexion
     */
    private void syncDataToLocal() {
        OfflineSyncHelper syncHelper = new OfflineSyncHelper(this);

        // Synchroniser toutes les données en arrière-plan
        syncHelper.syncAll(new OfflineSyncHelper.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "✅ Synchronisation réussie: " + message);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Erreur de synchronisation: " + error);
                // L'app continuera de fonctionner avec les données Firebase en cache
            }
        });
    }
}
