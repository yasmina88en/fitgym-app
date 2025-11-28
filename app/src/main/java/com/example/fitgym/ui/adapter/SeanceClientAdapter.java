package com.example.fitgym.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitgym.R;
import com.example.fitgym.data.db.FirebaseHelper;
import com.example.fitgym.data.model.Seance;
import com.example.fitgym.data.model.Categorie;
import com.example.fitgym.data.model.Coach;

import java.util.ArrayList;
import java.util.List;

public class SeanceClientAdapter extends RecyclerView.Adapter<SeanceClientAdapter.SeanceViewHolder> {

    private final Context context;
    private List<Seance> seances = new ArrayList<>();
    private final OnItemAction listener;

    private final FirebaseHelper firebase = new FirebaseHelper();

    public interface OnItemAction {
        void onItemClicked(Seance s);
        void onFavoriteClicked(Seance s);
    }

    public SeanceClientAdapter(Context context, OnItemAction listener) {
        this.context = context;
        this.listener = listener;
    }

    /** Mettre à jour la liste des séances */
    public void setItems(List<Seance> newSeances) {
        this.seances = newSeances != null ? newSeances : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SeanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_seance_client, parent, false);
        return new SeanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SeanceViewHolder holder, int position) {
        Seance s = seances.get(position);

        holder.tvTitre.setText(s.getTitre());
        holder.tvPrix.setText(s.getPrix() + " €"); // EURO demandé
        holder.tvNiveauChip.setText(s.getNiveau());
        holder.tvDateHeure.setText(s.getDate() + " • " + s.getHeure());

        /* ---------------------------
         Récupérer nom réel catégorie
        ---------------------------- */
        firebase.getCategorieById(s.getCategorieId(), category -> {
            if (category != null) {
                holder.tvCategorieChip.setText(category.getNom());

                // charger image selon catégorie
                holder.ivSessionImage.setImageResource(
                        mapCategoryToDrawable(category.getNom())
                );
            } else {
                holder.tvCategorieChip.setText("Catégorie");
                holder.ivSessionImage.setImageResource(R.drawable.placeholder);
            }
        });

        /* ---------------------------
          Récupérer coach
        ---------------------------- */
        if (s.getCoachId() != null && !s.getCoachId().isEmpty()) {
            firebase.getCoachById(s.getCoachId(), coach -> {
                if (coach != null)
                    holder.tvCoach.setText(coach.getNomComplet());
                else
                    holder.tvCoach.setText("Coach inconnu");
            });
        } else {
            holder.tvCoach.setText("Coach non attribué");
        }

        /* ---------------------------
           Places restantes
        ---------------------------- */
        int placesRestantes = s.getPlacesDisponibles();
        if (placesRestantes <= 3) {
            holder.tvWarning.setVisibility(View.VISIBLE);
            holder.tvWarning.setText("⚠️ Plus que " + placesRestantes + " places");
        } else {
            holder.tvWarning.setVisibility(View.GONE);
        }

        /* ---------------------------
           Clicks
        ---------------------------- */
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClicked(s);
        });

        holder.btnFavorite.setOnClickListener(v -> {
            if (listener != null) listener.onFavoriteClicked(s);
        });
    }

    @Override
    public int getItemCount() {
        return seances != null ? seances.size() : 0;
    }

    /** ViewHolder */
    public static class SeanceViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitre, tvPrix, tvCategorieChip, tvNiveauChip, tvCoach, tvDateHeure, tvWarning;
        ImageView ivSessionImage;
        ImageButton btnFavorite;

        public SeanceViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitre = itemView.findViewById(R.id.tvTitre);
            tvPrix = itemView.findViewById(R.id.tvPrix);
            tvCategorieChip = itemView.findViewById(R.id.tvCategorieChip);
            tvNiveauChip = itemView.findViewById(R.id.tvNiveauChip);
            tvCoach = itemView.findViewById(R.id.tvCoach);
            tvDateHeure = itemView.findViewById(R.id.tvDateHeure);
            tvWarning = itemView.findViewById(R.id.tvWarning);
            ivSessionImage = itemView.findViewById(R.id.ivSessionImage);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }
    }

    /** Mapper catégorie → drawable */
    private int mapCategoryToDrawable(String categoryName) {
        if (categoryName == null) return R.drawable.placeholder;

        switch (categoryName.toLowerCase()) {
            case "zumba": return R.drawable.zumba;
            case "crossfit": return R.drawable.crossfit;
            case "cardio": return R.drawable.cardio;
            case "musculation": return R.drawable.musculation;
            case "pilates": return R.drawable.pilates;
            case "yoga": return R.drawable.yoga;
            case "stretching": return R.drawable.stretching;
            case "boxe": return R.drawable.boxe;
            default: return R.drawable.placeholder;
        }
    }
}
