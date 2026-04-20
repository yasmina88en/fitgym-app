package com.example.fitgym.ui.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.fitgym.R;

import java.util.List;
import java.util.Map;

public class AvisAdapter extends RecyclerView.Adapter<AvisAdapter.AvisViewHolder> {

    private Context context;
    private List<Map<String, Object>> avisList;

    public AvisAdapter(Context context, List<Map<String, Object>> avisList) {
        this.context = context;
        this.avisList = avisList;
    }

    @NonNull
    @Override
    public AvisViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_avis_client_coach, parent, false);
        return new AvisViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AvisViewHolder holder, int position) {
        Map<String, Object> avis = avisList.get(position);

        // Log pour debug
        Log.d("AvisAdapter", "Avis data: " + avis.toString());

        // Nom client
        String nomClient = (String) avis.getOrDefault("nomClient", "Anonyme");
        holder.nomClient.setText(nomClient);
        Log.d("AvisAdapter", "Client name: " + nomClient);

        // Commentaire
        String commentaire = (String) avis.getOrDefault("commentaire", "");
        holder.commentaire.setText(commentaire);

        // Date
        String date = (String) avis.getOrDefault("date", "");
        holder.dateAvis.setText(date);

        // Avatar - Support pour Base64 et URLs
        String avatarUrl = (String) avis.getOrDefault("avatarUrl", "");
        Log.d("AvisAdapter", "Avatar URL: "
                + (avatarUrl != null ? avatarUrl.substring(0, Math.min(50, avatarUrl.length())) : "null"));

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            if (avatarUrl.startsWith("data:image") || avatarUrl.length() > 500) {
                // C'est une image Base64
                try {
                    String base64Image = avatarUrl;
                    if (base64Image.contains(",")) {
                        base64Image = base64Image.substring(base64Image.indexOf(",") + 1);
                    }
                    byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    if (bitmap != null) {
                        holder.avatarClient.setImageBitmap(bitmap);
                    } else {
                        holder.avatarClient.setImageResource(R.drawable.avatar_placeholder);
                    }
                } catch (Exception e) {
                    Log.e("AvisAdapter", "Erreur décodage Base64: " + e.getMessage());
                    holder.avatarClient.setImageResource(R.drawable.avatar_placeholder);
                }
            } else {
                // C'est une URL classique
                Glide.with(context)
                        .load(avatarUrl)
                        .placeholder(R.drawable.avatar_placeholder)
                        .error(R.drawable.avatar_placeholder)
                        .into(holder.avatarClient);
            }
        } else {
            holder.avatarClient.setImageResource(R.drawable.avatar_placeholder);
        }

        // Rating/Étoiles
        Object ratingObj = avis.get("rating");
        double rating = 0;
        if (ratingObj instanceof Number) {
            rating = ((Number) ratingObj).doubleValue();
        }
        afficherEtoiles(holder.layoutStarsAvis, rating);
    }

    private void afficherEtoiles(LinearLayout container, double rating) {
        container.removeAllViews();
        for (int i = 1; i <= 5; i++) {
            ImageView star = new ImageView(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(32, 32);
            params.setMarginEnd(4);
            star.setLayoutParams(params);
            star.setImageResource(i <= rating ? R.drawable.ic_star_filled : R.drawable.ic_star_empty);
            container.addView(star);
        }
    }

    @Override
    public int getItemCount() {
        return avisList.size();
    }

    static class AvisViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarClient;
        TextView nomClient, commentaire, dateAvis;
        LinearLayout layoutStarsAvis;

        AvisViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarClient = itemView.findViewById(R.id.avatarClient);
            nomClient = itemView.findViewById(R.id.nomClient);
            commentaire = itemView.findViewById(R.id.commentaire);
            dateAvis = itemView.findViewById(R.id.dateAvis);
            layoutStarsAvis = itemView.findViewById(R.id.layoutStarsAvis);
        }
    }
}
