package com.example.fitgym.ui.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.fitgym.R;
import com.example.fitgym.data.model.Coach;
import com.example.fitgym.ui.client.CoachDetailFragment;
import com.example.fitgym.ui.client.RatingDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;
import java.util.Locale;

public class CoachAdapter extends RecyclerView.Adapter<CoachAdapter.CoachViewHolder> {

    private final Fragment parentFragment;
    private final List<Coach> coachList;
    private int lastPosition = -1;

    public CoachAdapter(Fragment parentFragment, List<Coach> coachList) {
        this.parentFragment = parentFragment;
        this.coachList = coachList;
    }

    @NonNull
    @Override
    public CoachViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_coach_client, parent, false);
        return new CoachViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CoachViewHolder holder, int position) {
        Coach coach = coachList.get(position);
        if (coach == null)
            return;

        // 1. Remplir les informations textuelles du coach
        holder.coachName.setText(coach.getNom());
        holder.coachDescription.setText(coach.getDescription());
        holder.coachRating.setText(String.format(Locale.US, "%.1f", coach.getRating()));
        holder.coachReviewCount.setText(String.format(Locale.FRANCE, "(%d avis)", coach.getReviewCount()));

        // 2. Charger l'image du coach (gère Base64 et URLs)
        loadCoachImage(holder.coachAvatar, coach.getPhotoUrl(), holder.itemView);

        // 3. Afficher les étoiles et les spécialités
        afficherEtoiles(holder.layoutStarsCoach, coach.getRating());
        afficherSpecialites(holder.chipGroupSpecialties, coach.getSpecialites());

        // 4. Définir les listeners pour les clics

        // Listener pour toute la carte (ouvre le détail)
        holder.itemView.setOnClickListener(v -> {
            CoachDetailFragment detailFragment = CoachDetailFragment.newInstance(coach.getId());
            parentFragment.getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_client, detailFragment)
                    .addToBackStack(null)
                    .commit();
        });

        // Listener pour le bouton "Noter"
        holder.btnNoter.setOnClickListener(v -> {
            if (parentFragment instanceof RatingDialogFragment.OnRatingSubmitListener) {
                RatingDialogFragment dialog = RatingDialogFragment.newInstance(
                        coach.getId(),
                        coach.getNom(),
                        (RatingDialogFragment.OnRatingSubmitListener) parentFragment);
                dialog.show(parentFragment.getParentFragmentManager(), "rating_dialog_from_list");
            } else {
                Log.e("CoachAdapter",
                        "Le fragment parent doit implémenter RatingDialogFragment.OnRatingSubmitListener");
            }
        });

        // 5. Animation d'entrée
        setAnimation(holder.itemView, position);
    }

    private void setAnimation(View viewToAnimate, int position) {
        // Si la position est nouvelle, on anime
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(viewToAnimate.getContext(),
                    android.R.anim.slide_in_left);
            animation.setDuration(300); // Durée plus courte pour fluidité
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    /**
     * Charge une image de coach en gérant à la fois les images Base64 et les URLs
     */
    private void loadCoachImage(ImageView imageView, String photoUrl, View itemView) {
        if (photoUrl != null && !photoUrl.isEmpty()) {
            if (photoUrl.startsWith("data:image") || photoUrl.length() > 500) {
                // C'est une image Base64
                try {
                    String base64Image = photoUrl;

                    // Si c'est un Data URI complet, extraire seulement la partie Base64
                    if (base64Image.contains(",")) {
                        base64Image = base64Image.substring(base64Image.indexOf(",") + 1);
                    }

                    // Décoder le Base64
                    byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    } else {
                        imageView.setImageResource(R.drawable.coach_placeholder);
                    }
                } catch (Exception e) {
                    Log.e("CoachAdapter", "Erreur décodage Base64: " + e.getMessage());
                    imageView.setImageResource(R.drawable.coach_placeholder);
                }
            } else {
                // C'est une URL classique, utiliser Glide
                Glide.with(itemView.getContext())
                        .load(photoUrl)
                        .placeholder(R.drawable.coach_placeholder)
                        .error(R.drawable.ic_placeholder)
                        .into(imageView);
            }
        } else {
            // Pas de photo, afficher le placeholder
            imageView.setImageResource(R.drawable.coach_placeholder);
        }
    }

    private void afficherEtoiles(LinearLayout container, double rating) {
        if (container.getContext() == null)
            return;
        container.removeAllViews();
        for (int i = 1; i <= 5; i++) {
            ImageView star = new ImageView(container.getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(32, 32);
            params.setMarginEnd(4);
            star.setLayoutParams(params);
            star.setImageResource(i <= rating ? R.drawable.ic_star_filled : R.drawable.ic_star_empty);
            container.addView(star);
        }
    }

    private void afficherSpecialites(ChipGroup chipGroup, List<String> specialites) {
        if (chipGroup.getContext() == null)
            return;
        chipGroup.removeAllViews();
        if (specialites != null && !specialites.isEmpty()) {
            chipGroup.setVisibility(View.VISIBLE);
            for (String sp : specialites) {
                Chip chip = new Chip(chipGroup.getContext());
                chip.setText(sp);

                // Style Premium (Violet Theme)
                chip.setChipBackgroundColor(
                        android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F3E8FF"))); // Fond
                // très
                // clair
                chip.setChipStrokeColor(
                        android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#D8B4FE"))); // Bordure
                // douce
                chip.setChipStrokeWidth(1f);
                chip.setTextColor(android.graphics.Color.parseColor("#6D28D9")); // Texte foncé
                chip.setTextSize(12f);
                chip.setTypeface(null, android.graphics.Typeface.BOLD); // Texte en gras

                chip.setClickable(false);
                chipGroup.addView(chip);
            }
        } else {
            chipGroup.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return coachList != null ? coachList.size() : 0;
    }

    static class CoachViewHolder extends RecyclerView.ViewHolder {
        ImageView coachAvatar;
        TextView coachName, coachRating, coachReviewCount, coachDescription;
        LinearLayout layoutStarsCoach;
        ChipGroup chipGroupSpecialties;
        Button btnNoter;

        CoachViewHolder(@NonNull View itemView) {
            super(itemView);
            coachAvatar = itemView.findViewById(R.id.coachAvatar);
            coachName = itemView.findViewById(R.id.coachName);
            coachRating = itemView.findViewById(R.id.coachRating);
            coachReviewCount = itemView.findViewById(R.id.coachReviewCount);
            coachDescription = itemView.findViewById(R.id.coachDescription);
            layoutStarsCoach = itemView.findViewById(R.id.layoutStarsCoach);
            chipGroupSpecialties = itemView.findViewById(R.id.chipGroupSpecialties);
            btnNoter = itemView.findViewById(R.id.btnRate);
        }
    }
}
