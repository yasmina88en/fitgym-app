package com.example.fitgym.data.repository;

import androidx.annotation.NonNull;

import com.example.fitgym.data.model.Avis;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class AvisRepository {

    private DatabaseReference db = FirebaseDatabase.getInstance().getReference();

    // Interface pour les callbacks
    public interface AvisCallback<T> {
        void onSuccess(T data);

        void onError(String error);
    }

    // 🔹 AJOUTER UN AVIS (Mode Optimiste pour support Hors Ligne)
    public void ajouterAvis(String clientId, String coachId, Avis avis, AvisCallback<Void> callback) {
        Map<String, Object> avisMap = new HashMap<>();
        avisMap.put("clientId", clientId);
        avisMap.put("nomClient", avis.getNom());
        avisMap.put("avatarUrl", avis.getAvatarUrl());
        avisMap.put("rating", avis.getStars());
        avisMap.put("commentaire", avis.getCommentaire());
        avisMap.put("date", avis.getDate());
        avisMap.put("timestamp", avis.getTimestamp());

        // 1. Sauvegarder dans coachs/{coachId}/avis/{clientId}
        db.child("coachs")
                .child(coachId)
                .child("avis")
                .child(clientId)
                .setValue(avisMap);

        // 2. Sauvegarder dans clients/{clientId}/avis/{coachId}
        Map<String, Object> avisClient = new HashMap<>();
        avisClient.put("coachId", coachId);
        avisClient.put("rating", avis.getStars());
        avisClient.put("commentaire", avis.getCommentaire());
        avisClient.put("date", avis.getDate());
        avisClient.put("timestamp", avis.getTimestamp());

        db.child("clients")
                .child(clientId)
                .child("avis")
                .child(coachId)
                .setValue(avisClient);

        // 3. Mettre à jour la moyenne (Calcul local immédiat)
        updateCoachRating(coachId);

        // 4. Retourner le succès IMMÉDIATEMENT (Optimistic UI)
        // Les données sont dans le cache local et seront synchronisées plus tard.
        callback.onSuccess(null);
    }

    private void updateCoachRating(String coachId) {
        // On lit les données (qui incluent maintenant notre nouvel avis grâce au cache
        // local)
        db.child("coachs")
                .child(coachId)
                .child("avis")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        double totalRating = 0;
                        int count = 0;

                        for (DataSnapshot avisSnapshot : snapshot.getChildren()) {
                            Object ratingObj = avisSnapshot.child("rating").getValue();
                            if (ratingObj != null) {
                                totalRating += ((Number) ratingObj).doubleValue();
                                count++;
                            }
                        }

                        double averageRating = count > 0 ? totalRating / count : 0;

                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put("rating", Math.round(averageRating * 10.0) / 10.0);
                        updateData.put("reviewCount", count);

                        // Mise à jour locale (sera sync plus tard)
                        db.child("coachs")
                                .child(coachId)
                                .updateChildren(updateData);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Ignorer les erreurs en mode hors ligne/optimiste
                    }
                });
    }
}