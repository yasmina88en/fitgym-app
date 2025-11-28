package com.example.fitgym.ui.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitgym.R;
import com.example.fitgym.data.db.DatabaseHelper;
import com.example.fitgym.data.db.FirebaseHelper;
import com.example.fitgym.data.model.Client;

import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    private final Context context;
    private List<Client> clients;

    public UsersAdapter(Context context, List<Client> clients) {
        this.context = context;
        this.clients = clients;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_users, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {

        Client client = clients.get(position);

        holder.nomClient.setText(client.getNom());
        holder.etLogin.setText(client.getEmail());
        holder.numTele.setText(client.getTelephone());
        holder.nbReservation.setText("Réservations Totales : --");

        // Placeholder par défaut
        holder.avatarImage.setImageResource(R.drawable.ic_avatar_placeholder);

        boolean isOnline = isConnected();

        if (isOnline) {
            FirebaseHelper firebaseHelper = new FirebaseHelper();
            firebaseHelper.getClientPhotoByEmail(client.getEmail(), photoBase64 -> {
                if (photoBase64 != null && !photoBase64.isEmpty()) {
                    try {
                        byte[] decodedString = Base64.decode(photoBase64, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        holder.avatarImage.setImageBitmap(decodedByte);
                    } catch (IllegalArgumentException e) {
                        holder.avatarImage.setImageResource(R.drawable.ic_avatar_placeholder);
                    }
                }
            });
        }

        // Bouton Supprimer
        holder.btnSupprimer.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Confirmer la suppression")
                    .setMessage("Voulez-vous vraiment supprimer ce client ?")
                    .setPositiveButton("Oui", (dialog, which) -> {

                        DatabaseHelper db = new DatabaseHelper(context);
                        db.getWritableDatabase().delete("Client", "email = ?", new String[]{client.getEmail()});

                        int pos = holder.getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            clients.remove(pos);
                            notifyItemRemoved(pos);
                            notifyItemRangeChanged(pos, clients.size());
                        }

                        Toast.makeText(context, "Client supprimé localement", Toast.LENGTH_SHORT).show();

                        if (isOnline) {
                            FirebaseHelper firebaseHelper = new FirebaseHelper();
                            firebaseHelper.supprimerClientParEmail(client.getEmail(), success -> {
                                // Résultat suppression Firebase
                            });
                        }
                    })
                    .setNegativeButton("Non", null)
                    .show();
        });
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    @Override
    public int getItemCount() {
        return clients.size();
    }

    public void updateList(List<Client> newClients) {
        this.clients = newClients;
        notifyDataSetChanged();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView nomClient, etLogin, numTele, nbReservation;
        Button btnSupprimer;
        ImageView avatarImage;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            nomClient = itemView.findViewById(R.id.nomClient);
            etLogin = itemView.findViewById(R.id.etLogin);
            numTele = itemView.findViewById(R.id.numTele);
            nbReservation = itemView.findViewById(R.id.nbReservation);
            avatarImage = itemView.findViewById(R.id.avatarImage);
            btnSupprimer = itemView.findViewById(R.id.btnSupprimer);
        }
    }
}
