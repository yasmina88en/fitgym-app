package com.example.fitgym.ui.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.fitgym.R;
import com.example.fitgym.data.model.Seance;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SeanceCoachAdapter extends RecyclerView.Adapter<SeanceCoachAdapter.ViewHolder> {

    private Context context;
    private List<Seance> seances;
    private OnSeanceClickListener listener;

    public interface OnSeanceClickListener {
        void onSeanceClick(Seance seance);
    }

    public SeanceCoachAdapter(Context context, OnSeanceClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.seances = new ArrayList<>();
    }

    public void setItems(List<Seance> seances) {
        this.seances = seances;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_seance_client, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Seance seance = seances.get(position);
        holder.bind(seance, listener, context);
    }

    @Override
    public int getItemCount() {
        return seances.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivSessionImage;
        TextView tvTitre, tvPrix, tvDateHeure, tvNiveauChip, tvWarning;
        TextView tvCategorieChip, tvCoach, tvRating;
        ImageButton btnFavorite;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSessionImage = itemView.findViewById(R.id.ivSessionImage);
            tvTitre = itemView.findViewById(R.id.tvTitre);
            tvPrix = itemView.findViewById(R.id.tvPrix);
            tvDateHeure = itemView.findViewById(R.id.tvDateHeure);
            tvNiveauChip = itemView.findViewById(R.id.tvNiveauChip);
            tvWarning = itemView.findViewById(R.id.tvWarning);
            tvCategorieChip = itemView.findViewById(R.id.tvCategorieChip);
            tvCoach = itemView.findViewById(R.id.tvCoach);
            tvRating = itemView.findViewById(R.id.tvRating);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }

        public void bind(final Seance seance, final OnSeanceClickListener listener, Context context) {
            tvTitre.setText(seance.getTitre());
            tvPrix.setText(String.format(Locale.FRANCE, "%.0f€", seance.getPrix()));
            tvDateHeure.setText(String.format("%s • %s", seance.getDate(), seance.getHeure()));
            tvNiveauChip.setText(seance.getNiveau());

            // Masquer le bouton favoris dans le contexte du coach detail
            if (btnFavorite != null) {
                btnFavorite.setVisibility(View.GONE);
            }

            // Charger le nom de la catégorie depuis Firebase
            if (seance.getCategorieId() != null && !seance.getCategorieId().isEmpty()) {
                loadCategorieName(seance.getCategorieId(), tvCategorieChip);
            } else {
                tvCategorieChip.setText("Catégorie");
            }

            // Charger le nom du coach depuis Firebase
            if (seance.getCoachId() != null && !seance.getCoachId().isEmpty()) {
                loadCoachName(seance.getCoachId(), tvCoach, tvRating);
            } else {
                tvCoach.setText("Coach");
                tvRating.setVisibility(View.GONE);
            }

            // Gestion de l'image de la séance
            if (seance.getImageUrl() != null && !seance.getImageUrl().isEmpty()) {
                // Si une URL d'image est disponible sur la séance, l'utiliser
                Glide.with(itemView.getContext())
                        .load(seance.getImageUrl())
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.placeholder)
                        .into(ivSessionImage);
            } else if (seance.getCategorieId() != null && !seance.getCategorieId().isEmpty()) {
                // Sinon, charger l'image depuis la catégorie Firebase
                loadCategorieImage(seance.getCategorieId(), ivSessionImage);
            } else {
                // En dernier recours, utiliser le placeholder
                ivSessionImage.setImageResource(R.drawable.placeholder);
            }

            // Gestion de l'état "Complet"
            if (seance.getPlacesDisponibles() <= 0) {
                tvWarning.setVisibility(View.VISIBLE);
                tvWarning.setText("⚠️ Complet");
            } else if (seance.getPlacesDisponibles() <= 3) {
                tvWarning.setVisibility(View.VISIBLE);
                tvWarning.setText("⚠️ Plus que " + seance.getPlacesDisponibles() + " places");
            } else {
                tvWarning.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSeanceClick(seance);
                }
            });
        }

        private void loadCategorieName(String categorieId, TextView textView) {
            FirebaseDatabase.getInstance().getReference("categories")
                    .child(categorieId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String nom = snapshot.child("nom").getValue(String.class);
                                if (nom != null) {
                                    textView.setText(nom);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("SeanceCoachAdapter", "Erreur chargement catégorie: " + error.getMessage());
                        }
                    });
        }

        private void loadCoachName(String coachId, TextView tvCoach, TextView tvRating) {
            FirebaseDatabase.getInstance().getReference("coachs")
                    .child(coachId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String nom = snapshot.child("nom").getValue(String.class);
                                Double rating = snapshot.child("rating").getValue(Double.class);
                                Integer reviewCount = snapshot.child("reviewCount").getValue(Integer.class);

                                if (nom != null) {
                                    tvCoach.setText(nom);
                                }

                                if (rating != null && reviewCount != null && reviewCount > 0) {
                                    tvRating.setText(String.format(Locale.FRANCE, "%.1f (%d)", rating, reviewCount));
                                    tvRating.setVisibility(View.VISIBLE);
                                } else {
                                    tvRating.setVisibility(View.GONE);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("SeanceCoachAdapter", "Erreur chargement coach: " + error.getMessage());
                        }
                    });
        }

        private void loadCategorieImage(String categorieId, ImageView imageView) {
            FirebaseDatabase.getInstance().getReference("categories")
                    .child(categorieId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String imageUrl = snapshot.child("imageUrl").getValue(String.class);
                                String nom = snapshot.child("nom").getValue(String.class);

                                if (imageUrl != null && !imageUrl.isEmpty()) {
                                    // Charger l'image depuis l'URL Firebase
                                    Glide.with(itemView.getContext())
                                            .load(imageUrl)
                                            .placeholder(R.drawable.placeholder)
                                            .error(R.drawable.placeholder)
                                            .into(imageView);
                                } else if (nom != null) {
                                    // Utiliser une image locale basée sur le nom de la catégorie
                                    int imageRes = getImageForCategory(nom);
                                    Glide.with(itemView.getContext())
                                            .load(imageRes)
                                            .placeholder(R.drawable.placeholder)
                                            .into(imageView);
                                } else {
                                    imageView.setImageResource(R.drawable.placeholder);
                                }
                            } else {
                                // Si la catégorie n'existe pas, essayer avec l'ID directement
                                int imageRes = getImageForCategory(categorieId);
                                Glide.with(itemView.getContext())
                                        .load(imageRes)
                                        .placeholder(R.drawable.placeholder)
                                        .into(imageView);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("SeanceCoachAdapter", "Erreur chargement image catégorie: " + error.getMessage());
                            // En cas d'erreur, utiliser l'image par défaut basée sur l'ID
                            int imageRes = getImageForCategory(categorieId);
                            Glide.with(itemView.getContext())
                                    .load(imageRes)
                                    .placeholder(R.drawable.placeholder)
                                    .into(imageView);
                        }
                    });
        }

        private int getImageForCategory(String categorieId) {
            if (categorieId == null)
                return R.drawable.placeholder;

            // Mapping des catégories vers les images correspondantes
            switch (categorieId.toLowerCase()) {
                case "yoga":
                    return R.drawable.yoga;
                case "boxe":
                    return R.drawable.boxe;
                case "cardio":
                    return R.drawable.cardio;
                case "crossfit":
                    return R.drawable.crossfit;
                case "musculation":
                    return R.drawable.musculation;
                case "pilates":
                    return R.drawable.pilates;
                case "stretching":
                    return R.drawable.stretching;
                case "zumba":
                    return R.drawable.zumba;
                default:
                    return R.drawable.placeholder;
            }
        }
    }
}
