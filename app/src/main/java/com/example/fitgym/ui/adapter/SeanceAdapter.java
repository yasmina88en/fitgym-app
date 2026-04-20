package com.example.fitgym.ui.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.fitgym.data.db.FirebaseHelper;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.fitgym.R;
import com.example.fitgym.data.dao.DAOCoach;
import com.example.fitgym.data.db.DatabaseHelper;
import com.example.fitgym.data.model.Categorie;
import com.example.fitgym.data.model.Coach;
import com.example.fitgym.data.model.Seance;

import java.util.ArrayList;
import java.util.List;

public class SeanceAdapter extends RecyclerView.Adapter<SeanceAdapter.SeanceViewHolder> {

    private List<Seance> seances;
    public final OnItemClickListener listener;
    private final FirebaseHelper firebaseHelper;
    private final DatabaseHelper databaseHelper;
    private final DAOCoach daoCoach;

    public interface OnItemClickListener {
        void onModifierClicked(Seance seance);
        void onSupprimerClicked(Seance seance);
    }

    // NOTE : on passe databaseHelper depuis le fragment
    public SeanceAdapter(List<Seance> seances, OnItemClickListener listener, DatabaseHelper dbHelper) {
        this.seances = seances != null ? seances : new ArrayList<>();
        this.listener = listener;
        this.firebaseHelper = new FirebaseHelper();
        this.databaseHelper = dbHelper;
        // DAOCoach utilise la même DB : on ouvre une instance DAO pour les inserts locaux de coachs
        this.daoCoach = new DAOCoach(dbHelper.getWritableDatabase());
    }

    // méthode publique pour mettre à jour la donnée sans recréer l'adapter
    public void updateData(List<Seance> newList) {
        this.seances = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SeanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_seance, parent, false);
        return new SeanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SeanceViewHolder holder, int position) {
        Seance seance = seances.get(position);

        holder.titreSeance.setText(seance.getTitre() != null ? seance.getTitre() : "");
        holder.niveauSeance.setText(seance.getNiveau() != null ? seance.getNiveau() : "");
        holder.places.setText("Places: " + seance.getPlacesDisponibles() + "/" + seance.getPlacesTotales());
        holder.dateHeure.setText((seance.getDate() != null ? seance.getDate() : "") + " · " + (seance.getHeure() != null ? seance.getHeure() : ""));

        // --- Catégorie : d'abord local ---
        String catId = seance.getCategorieId();
        if (catId != null && !catId.isEmpty()) {
            Categorie categorie = databaseHelper.getCategorieById(catId);
            if (categorie != null) {
                holder.categorieSeance.setText(categorie.getNom());
                holder.imageSeance.setImageResource(getImageForCategorie(categorie.getNom()));
            } else {
                holder.categorieSeance.setText("Chargement...");
                firebaseHelper.getCategorieById(catId, fetchedCat -> {
                    if (fetchedCat != null) {
                        holder.categorieSeance.setText(fetchedCat.getNom());
                        holder.imageSeance.setImageResource(getImageForCategorie(fetchedCat.getNom()));
                        // enregistrer localement
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

        // --- Coach : d'abord local ---
        String coachId = seance.getCoachId();
        if (coachId != null && !coachId.isEmpty()) {
            Coach coachLocal = databaseHelper.getCoachById(coachId);
            if (coachLocal != null) {
                holder.coachPrix.setText((coachLocal.getNom() != null ? coachLocal.getNom() : "Coach") + " · " + seance.getPrix() + "€");
                if (coachLocal.getPhotoUrl() != null && coachLocal.getPhotoUrl().trim().length() > 20) {
                    Bitmap bmp = convertBase64ToBitmap(coachLocal.getPhotoUrl());
                    if (bmp != null) {
                        Glide.with(holder.imageCoach.getContext()).load(bmp).placeholder(R.drawable.ic_placeholder).into(holder.imageCoach);
                    } else {
                        holder.imageCoach.setImageResource(R.drawable.ic_placeholder);
                    }
                } else {
                    holder.imageCoach.setImageResource(R.drawable.ic_placeholder);
                }
            } else {
                holder.coachPrix.setText("Chargement coach · " + seance.getPrix() + "€");
                firebaseHelper.getCoachById(coachId, fetchedCoach -> {
                    if (fetchedCoach != null) {
                        holder.coachPrix.setText((fetchedCoach.getNom() != null ? fetchedCoach.getNom() : "Coach") + " · " + seance.getPrix() + "€");
                        if (fetchedCoach.getPhotoUrl() != null && fetchedCoach.getPhotoUrl().trim().length() > 20) {
                            Bitmap bmp = convertBase64ToBitmap(fetchedCoach.getPhotoUrl());
                            if (bmp != null) {
                                Glide.with(holder.imageCoach.getContext()).load(bmp).placeholder(R.drawable.ic_placeholder).into(holder.imageCoach);
                            } else {
                                holder.imageCoach.setImageResource(R.drawable.ic_placeholder);
                            }
                        } else {
                            holder.imageCoach.setImageResource(R.drawable.ic_placeholder);
                        }
                        // insérer localement
                        daoCoach.ajouterCoach(fetchedCoach);
                    } else {
                        holder.coachPrix.setText("Coach inconnu · " + seance.getPrix() + "€");
                        holder.imageCoach.setImageResource(R.drawable.ic_placeholder);
                    }
                });
            }
        } else {
            holder.coachPrix.setText("Coach inconnu · " + seance.getPrix() + "€");
            holder.imageCoach.setImageResource(R.drawable.ic_placeholder);
        }

        holder.btnModifier.setOnClickListener(v -> listener.onModifierClicked(seance));
        holder.btnSupprimer.setOnClickListener(v -> listener.onSupprimerClicked(seance));
    }

    @Override
    public int getItemCount() {
        return seances.size();
    }

    public static class SeanceViewHolder extends RecyclerView.ViewHolder {
        TextView titreSeance, categorieSeance, niveauSeance, coachPrix, dateHeure, places;
        ImageView imageSeance, imageCoach;
        Button btnModifier, btnSupprimer;

        public SeanceViewHolder(@NonNull View itemView) {
            super(itemView);
            titreSeance = itemView.findViewById(R.id.titre);
            categorieSeance = itemView.findViewById(R.id.categorieSeance);
            niveauSeance = itemView.findViewById(R.id.niveauSeance);
            coachPrix = itemView.findViewById(R.id.Prix);
            dateHeure = itemView.findViewById(R.id.dateHeure);
            places = itemView.findViewById(R.id.places);
            imageSeance = itemView.findViewById(R.id.imageSeance);
            imageCoach = itemView.findViewById(R.id.imageCoach);
            btnModifier = itemView.findViewById(R.id.btnModifier);
            btnSupprimer = itemView.findViewById(R.id.btnSupprimer);
        }
    }

    private int getImageForCategorie(String categorieNom) {
        if (categorieNom == null) return R.drawable.default_image;
        switch (categorieNom.toLowerCase()) {
            case "yoga":
                return R.drawable.yoga;
            case "musculation":
                return R.drawable.musculation;
            case "pilates":
                return R.drawable.pilates;
            case "zumba":
                return R.drawable.zumba;
            case "crossfit":
                return R.drawable.crossfit;
            case "cardio":
                return R.drawable.cardio;
            case "box":
                return R.drawable.boxe;
            case "stretching":
                return R.drawable.stretching;
            default:
                return R.drawable.default_image;
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

