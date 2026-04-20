package com.example.fitgym.ui.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.fitgym.R;
import com.example.fitgym.data.dao.DAOCoach;
import com.example.fitgym.data.db.DatabaseHelper;
import com.example.fitgym.data.db.FirebaseHelper;
import com.example.fitgym.data.model.Categorie;
import com.example.fitgym.data.model.Coach;
import com.example.fitgym.data.model.Favoris;
import com.example.fitgym.data.model.Seance;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FavorisAdapter extends RecyclerView.Adapter<FavorisAdapter.VH> {

    private List<Seance> seances;
    private final DatabaseHelper db;
    private final FirebaseHelper firebaseHelper;
    private final DAOCoach daoCoach;
    private final String clientId;
    private final Runnable onRemovedCallback; // appelé après suppression pour recharger

    public FavorisAdapter(List<Seance> seances, DatabaseHelper db, String clientId, FirebaseHelper firebaseHelper, Runnable onRemovedCallback) {
        this.seances = seances != null ? seances : new ArrayList<>();
        this.db = db;
        this.firebaseHelper = firebaseHelper != null ? firebaseHelper : new FirebaseHelper();
        this.daoCoach = new DAOCoach(db.getWritableDatabase());
        this.clientId = clientId;
        this.onRemovedCallback = onRemovedCallback;
    }

    public void updateData(List<Seance> list) {
        this.seances = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FavorisAdapter.VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favoris, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FavorisAdapter.VH holder, int position) {
        Seance s = seances.get(position);

        holder.titre.setText(s.getTitre() != null ? s.getTitre() : "");
        holder.dateHeure.setText((s.getDate() != null ? s.getDate() : "") + " · " + (s.getHeure() != null ? s.getHeure() : ""));
        holder.places.setText("Places: " + s.getPlacesDisponibles() + "/" + s.getPlacesTotales());
        holder.Prix.setText(s.getPrix() + "€");

        // category
        String catId = s.getCategorieId();
        if (catId != null && !catId.isEmpty()) {
            Categorie catLocal = db.getCategorieById(catId);
            if (catLocal != null) {
                holder.categorieSeance.setText(catLocal.getNom());
                holder.imageSeance.setImageResource(getImageForCategorie(catLocal.getNom()));
            } else {
                holder.categorieSeance.setText("Chargement...");
                firebaseHelper.getCategorieById(catId, fetchedCat -> {
                    if (fetchedCat != null) {
                        holder.categorieSeance.setText(fetchedCat.getNom());
                        holder.imageSeance.setImageResource(getImageForCategorie(fetchedCat.getNom()));
                        db.insertOrUpdateCategorie(fetchedCat);
                    } else {
                        holder.categorieSeance.setText("Autre");
                        holder.imageSeance.setImageResource(R.drawable.default_image);
                    }
                });
            }
        } else {
            holder.categorieSeance.setText("Autre");
            holder.imageSeance.setImageResource(R.drawable.default_image);
        }

        // coach
        String coachId = s.getCoachId();
        if (coachId != null && !coachId.isEmpty()) {
            Coach coachLocal = db.getCoachById(coachId);
            if (coachLocal != null) {
                holder.Prix.setText((coachLocal.getNom() != null ? coachLocal.getNom() : "Coach") + " · " + s.getPrix() + "€");
                if (coachLocal.getPhotoUrl() != null && coachLocal.getPhotoUrl().trim().length() > 20) {
                    Bitmap bmp = convertBase64ToBitmap(coachLocal.getPhotoUrl());
                    if (bmp != null) Glide.with(holder.imageCoach.getContext()).load(bmp).placeholder(R.drawable.ic_placeholder).into(holder.imageCoach);
                    else holder.imageCoach.setImageResource(R.drawable.ic_placeholder);
                } else holder.imageCoach.setImageResource(R.drawable.ic_placeholder);
            } else {
                holder.Prix.setText("Chargement coach · " + s.getPrix() + "€");
                firebaseHelper.getCoachById(coachId, fetchedCoach -> {
                    if (fetchedCoach != null) {
                        holder.Prix.setText((fetchedCoach.getNom() != null ? fetchedCoach.getNom() : "Coach") + " · " + s.getPrix() + "€");
                        if (fetchedCoach.getPhotoUrl() != null && fetchedCoach.getPhotoUrl().trim().length() > 20) {
                            Bitmap bmp = convertBase64ToBitmap(fetchedCoach.getPhotoUrl());
                            if (bmp != null) Glide.with(holder.imageCoach.getContext()).load(bmp).placeholder(R.drawable.ic_placeholder).into(holder.imageCoach);
                            else holder.imageCoach.setImageResource(R.drawable.ic_placeholder);
                        } else holder.imageCoach.setImageResource(R.drawable.ic_placeholder);
                        try { daoCoach.ajouterCoach(fetchedCoach); } catch (Exception ignored) {}
                    } else {
                        holder.Prix.setText("Coach inconnu · " + s.getPrix() + "€");
                        holder.imageCoach.setImageResource(R.drawable.ic_placeholder);
                    }
                });
            }
        } else {
            holder.Prix.setText("Coach inconnu · " + s.getPrix() + "€");
            holder.imageCoach.setImageResource(R.drawable.ic_placeholder);
        }

        // Suppression du favori via le bouton coeur (btnSuppFavori)
        holder.btnSuppFavori.setOnClickListener(v -> {
            if (clientId == null || s.getId() == null) return;

            // 1) supprimer localement
            boolean removedLocal = db.supprimerFavoriLocal(clientId, s.getId());
            if (removedLocal) {
                // 2) tenter suppression côté Firebase
                firebaseHelper.getFavorisClient(clientId, favorisList -> {
                    // favorisList contient Favoris (avec id & seanceId) pour ce client
                    for (Favoris f : favorisList) {
                        if (f != null && s.getId().equals(f.getSeanceId())) {
                            // supprimer ce favoris par id
                            firebaseHelper.supprimerFavori(f.getId(), success -> {
                                // ignore success/fail for now, but could rollback
                            });
                        }
                    }
                    // 3) mettre à jour l'UI (supprimer de la liste courante)
                    seances.remove(s);
                    notifyDataSetChanged();
                    if (onRemovedCallback != null) onRemovedCallback.run();
                    Toast.makeText(holder.itemView.getContext(), "Supprimé des favoris", Toast.LENGTH_SHORT).show();
                });
            } else {
                // peut-être déjà supprimé localement
                seances.remove(s);
                notifyDataSetChanged();
                if (onRemovedCallback != null) onRemovedCallback.run();
            }
        });
    }

    @Override
    public int getItemCount() {
        return seances.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imageSeance, imageCoach;
        ImageButton btnSuppFavori;
        TextView titre, categorieSeance, dateHeure, places, Prix;

        VH(@NonNull View itemView) {
            super(itemView);
            imageSeance = itemView.findViewById(R.id.imageSeance);
            imageCoach = itemView.findViewById(R.id.imageCoach);
            btnSuppFavori = itemView.findViewById(R.id.btnSuppFavori);
            titre = itemView.findViewById(R.id.titre);
            categorieSeance = itemView.findViewById(R.id.categorieSeance);
            dateHeure = itemView.findViewById(R.id.dateHeure);
            places = itemView.findViewById(R.id.places);
            Prix = itemView.findViewById(R.id.Prix);
        }
    }

    private int getImageForCategorie(String categorieNom) {
        if (categorieNom == null) return R.drawable.default_image;
        switch (categorieNom.toLowerCase()) {
            case "yoga": return R.drawable.yoga;
            case "musculation": return R.drawable.musculation;
            case "pilates": return R.drawable.pilates;
            case "zumba": return R.drawable.zumba;
            case "crossfit": return R.drawable.crossfit;
            case "cardio": return R.drawable.cardio;
            case "box": return R.drawable.boxe;
            case "stretching": return R.drawable.stretching;
            default: return R.drawable.default_image;
        }
    }

    private Bitmap convertBase64ToBitmap(String base64Str) {
        try {
            if (base64Str == null || base64Str.trim().length() < 20) return null;
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
