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

public class ClientSeanceAdapterYas extends RecyclerView.Adapter<ClientSeanceAdapterYas.VH> {

    private List<Seance> seances;
    private final OnActionListener listener;
    private final FirebaseHelper firebaseHelper;
    private final DatabaseHelper databaseHelper;
    private final DAOCoach daoCoach;
    private final String clientId;

    public interface OnActionListener {
        void onReserverClicked(Seance s);
        // optional callback if fragment wants to react when favourited
        void onFavoriAdded(Seance s);
    }

    public ClientSeanceAdapterYas(List<Seance> seances, DatabaseHelper dbHelper, String clientId, OnActionListener listener) {
        this.seances = seances != null ? seances : new ArrayList<>();
        this.listener = listener;
        this.databaseHelper = dbHelper;
        this.clientId = clientId;
        this.firebaseHelper = new FirebaseHelper();
        this.daoCoach = new DAOCoach(dbHelper.getWritableDatabase());
    }

    public void updateData(List<Seance> newList) {
        this.seances = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_seance_client_yas, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Seance seance = seances.get(position);

        holder.titre.setText(seance.getTitre() != null ? seance.getTitre() : "");
        holder.niveauSeance.setText(seance.getNiveau() != null ? seance.getNiveau() : "");
        holder.places.setText("Places: " + seance.getPlacesDisponibles() + "/" + seance.getPlacesTotales());
        holder.dateHeure.setText((seance.getDate() != null ? seance.getDate() : "") + " · " + (seance.getHeure() != null ? seance.getHeure() : ""));
        holder.Prix.setText(seance.getPrix() + "€");

        // --- Catégorie (local then remote) ---
        String catId = seance.getCategorieId();
        if (catId != null && !catId.isEmpty()) {
            Categorie catLocal = databaseHelper.getCategorieById(catId);
            if (catLocal != null) {
                holder.categorieSeance.setText(catLocal.getNom());
                holder.imageSeance.setImageResource(getImageForCategorie(catLocal.getNom()));
            } else {
                holder.categorieSeance.setText("Chargement...");
                firebaseHelper.getCategorieById(catId, fetchedCat -> {
                    if (fetchedCat != null) {
                        holder.categorieSeance.setText(fetchedCat.getNom());
                        holder.imageSeance.setImageResource(getImageForCategorie(fetchedCat.getNom()));
                        databaseHelper.insertOrUpdateCategorie(fetchedCat);
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

        // --- Coach (local then remote) ---
        String coachId = seance.getCoachId();
        if (coachId != null && !coachId.isEmpty()) {
            Coach coachLocal = databaseHelper.getCoachById(coachId);
            if (coachLocal != null) {
                holder.Prix.setText((coachLocal.getNom() != null ? coachLocal.getNom() : "Coach") + " · " + seance.getPrix() + "€");
                if (coachLocal.getPhotoUrl() != null && coachLocal.getPhotoUrl().trim().length() > 20) {
                    Bitmap bmp = convertBase64ToBitmap(coachLocal.getPhotoUrl());
                    if (bmp != null) Glide.with(holder.imageCoach.getContext()).load(bmp).placeholder(R.drawable.ic_placeholder).into(holder.imageCoach);
                    else holder.imageCoach.setImageResource(R.drawable.ic_placeholder);
                } else holder.imageCoach.setImageResource(R.drawable.ic_placeholder);
            } else {
                holder.Prix.setText("Chargement coach · " + seance.getPrix() + "€");
                firebaseHelper.getCoachById(coachId, fetchedCoach -> {
                    if (fetchedCoach != null) {
                        holder.Prix.setText((fetchedCoach.getNom() != null ? fetchedCoach.getNom() : "Coach") + " · " + seance.getPrix() + "€");
                        if (fetchedCoach.getPhotoUrl() != null && fetchedCoach.getPhotoUrl().trim().length() > 20) {
                            Bitmap bmp = convertBase64ToBitmap(fetchedCoach.getPhotoUrl());
                            if (bmp != null) Glide.with(holder.imageCoach.getContext()).load(bmp).placeholder(R.drawable.ic_placeholder).into(holder.imageCoach);
                            else holder.imageCoach.setImageResource(R.drawable.ic_placeholder);
                        } else holder.imageCoach.setImageResource(R.drawable.ic_placeholder);

                        // insert local coach
                        try { daoCoach.ajouterCoach(fetchedCoach); } catch (Exception ignored) {}
                    } else {
                        holder.Prix.setText("Coach inconnu · " + seance.getPrix() + "€");
                        holder.imageCoach.setImageResource(R.drawable.ic_placeholder);
                    }
                });
            }
        } else {
            holder.Prix.setText("Coach inconnu · " + seance.getPrix() + "€");
            holder.imageCoach.setImageResource(R.drawable.ic_placeholder);
        }

        // --- Etat favoris local ---
        boolean estFavori = false;
        try {
            if (clientId != null && seance.getId() != null) {
                estFavori = databaseHelper.estFavori(clientId, seance.getId());
            }
        } catch (Exception ignored) { estFavori = false; }
        holder.btnFavori.setImageResource(estFavori ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);

        // Click coeur : optimistic UI + persist local + push firebase
        holder.btnFavori.setOnClickListener(v -> {
            // if already favori, do nothing (you said you won't implement remove)
            if (databaseHelper.estFavori(clientId, seance.getId())) {
                Toast.makeText(holder.itemView.getContext(), "Déjà dans les favoris", Toast.LENGTH_SHORT).show();
                holder.btnFavori.setImageResource(R.drawable.ic_heart_filled);
                return;
            }

            // Optimistic UI
            holder.btnFavori.setImageResource(R.drawable.ic_heart_filled);

            // 1) insert local
            boolean localInserted = databaseHelper.ajouterFavoriLocal(clientId, seance.getId());

            // 2) push remote
            Favoris f = new Favoris();
            f.setClientId(clientId);
            f.setSeanceId(seance.getId());
            // id sera géré par FirebaseHelper si null
            firebaseHelper.ajouterFavori(f, success -> {
                // si succès → nothing (local already inserted) ; si échec → on peut laisser local (sync later) or show msg
                if (!success) {
                    // optional rollback local removal (if you want) — here we'll keep local and inform user
                    holder.itemView.post(() -> Toast.makeText(holder.itemView.getContext(), "Échec sync favoris ; sera retrié plus tard", Toast.LENGTH_SHORT).show());
                }
                // notify fragment if wants to react
                if (listener != null) listener.onFavoriAdded(seance);
            });
        });

        // Reserver button: only UI; actual logic handled by another dev
        holder.btnReserver.setOnClickListener(v -> {
            if (listener != null) listener.onReserverClicked(seance);
        });
    }

    @Override
    public int getItemCount() {
        return seances.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imageSeance;
        ImageButton btnFavori;
        TextView titre, categorieSeance, niveauSeance, dateHeure, places, Prix;
        ImageView imageCoach;
        Button btnReserver;

        VH(@NonNull View itemView) {
            super(itemView);
            imageSeance = itemView.findViewById(R.id.imageSeance);
            btnFavori = itemView.findViewById(R.id.btnFavori);
            titre = itemView.findViewById(R.id.titre);
            categorieSeance = itemView.findViewById(R.id.categorieSeance);
            niveauSeance = itemView.findViewById(R.id.niveauSeance);
            dateHeure = itemView.findViewById(R.id.dateHeure);
            places = itemView.findViewById(R.id.places);
            imageCoach = itemView.findViewById(R.id.imageCoach);
            Prix = itemView.findViewById(R.id.Prix);
            btnReserver = itemView.findViewById(R.id.btnReserver);
        }
    }

    // same image mapping as SeanceAdapter
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
