package com.example.fitgym.ui.client;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.fitgym.R;

import com.example.fitgym.data.model.Coach;
import com.example.fitgym.data.model.Seance;
import com.example.fitgym.data.repository.DataRepository;
import com.example.fitgym.ui.adapter.AvisAdapter;
import com.example.fitgym.ui.adapter.SeanceCoachAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CoachDetailFragment extends Fragment implements RatingDialogFragment.OnRatingSubmitListener {

    private DatabaseReference db;
    private FirebaseAuth auth;
    private DataRepository dataRepository;

    private ImageView imageCoach;
    private TextView nomCoach, descriptionCoach, ratingCoach, nbAvis;
    private LinearLayout layoutStarsCoach, layoutSpecialites;
    private RecyclerView recyclerAvis, recyclerSeances;
    private Button btnNoter;
    private ImageButton btnRetour;

    private String coachId;
    private String clientId;
    private AvisAdapter avisAdapter;
    private SeanceCoachAdapter seanceAdapter;
    private final List<Map<String, Object>> avisList = new ArrayList<>();

    public CoachDetailFragment() {
        // Constructeur public vide requis
    }

    public static CoachDetailFragment newInstance(String coachId) {
        CoachDetailFragment fragment = new CoachDetailFragment();
        Bundle args = new Bundle();
        args.putString("coach_id", coachId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_coach_detail, container, false);

        // Initialisation de Firebase et DataRepository
        db = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();
        dataRepository = DataRepository.getInstance(requireContext());

        // Liaison des vues
        imageCoach = view.findViewById(R.id.imageCoach);
        nomCoach = view.findViewById(R.id.nomCoach);
        descriptionCoach = view.findViewById(R.id.descriptionCoach);
        ratingCoach = view.findViewById(R.id.ratingCoach);
        nbAvis = view.findViewById(R.id.nbAvis);
        layoutStarsCoach = view.findViewById(R.id.layoutStarsCoach);
        layoutSpecialites = view.findViewById(R.id.layoutSpecialites);
        recyclerAvis = view.findViewById(R.id.recyclerAvis);
        recyclerSeances = view.findViewById(R.id.recyclerSeances);
        btnNoter = view.findViewById(R.id.btnRate1);
        btnRetour = view.findViewById(R.id.btnRetour);

        // Récupération des arguments (ID du coach)
        if (getArguments() != null) {
            coachId = getArguments().getString("coach_id");
        }
        if (auth.getCurrentUser() != null) {
            clientId = auth.getCurrentUser().getUid();
        }

        // Configuration du RecyclerView
        setupRecyclerView();

        // Lancement du chargement des données
        loadCoachDetails();
        loadSeances();

        // Configuration des actions des boutons
        handleActions();

        return view;
    }

    private void setupRecyclerView() {
        // Configuration Avis
        avisAdapter = new AvisAdapter(getContext(), avisList);
        recyclerAvis.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerAvis.setAdapter(avisAdapter);

        // Configuration Séances (Horizontal, avec le nouvel adaptateur)
        seanceAdapter = new SeanceCoachAdapter(getContext(), s -> {
            // Gérer le clic sur une séance
            Toast.makeText(getContext(), "Séance: " + s.getTitre(), Toast.LENGTH_SHORT).show();
        });
        recyclerSeances.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerSeances.setAdapter(seanceAdapter);
    }

    private void loadSeances() {
        if (coachId == null)
            return;

        db.child("seances").orderByChild("coachId").equalTo(coachId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (getContext() == null)
                            return;

                        List<com.example.fitgym.data.model.Seance> loadedSeances = new ArrayList<>();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            com.example.fitgym.data.model.Seance seance = snap
                                    .getValue(com.example.fitgym.data.model.Seance.class);
                            if (seance != null) {
                                seance.setId(snap.getKey());
                                loadedSeances.add(seance);
                            }
                        }
                        seanceAdapter.setItems(loadedSeances);

                        // Afficher ou masquer le RecyclerView selon s'il y a des séances
                        if (loadedSeances.isEmpty()) {
                            recyclerSeances.setVisibility(View.GONE);
                        } else {
                            recyclerSeances.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("CoachDetail", "Erreur chargement séances: " + error.getMessage());
                    }
                });
    }

    private void loadCoachDetails() {
        if (coachId == null || coachId.isEmpty()) {
            Toast.makeText(getContext(), "Erreur: ID du coach manquant.", Toast.LENGTH_SHORT).show();
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
            return;
        }

        // Utilise DataRepository pour charger depuis SQLite puis Firebase
        dataRepository.getCoachById(coachId, new DataRepository.DataCallback<Coach>() {
            @Override
            public void onSuccess(Coach coach) {
                if (getContext() == null || getActivity() == null || !isAdded())
                    return;
                if (coach != null) {
                    fillUI(coach);
                    loadAvis();
                } else {
                    Toast.makeText(getContext(), "Erreur: Aucun coach trouvé.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String error) {
                if (getContext() == null)
                    return;
                Toast.makeText(getContext(), "Erreur: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fillUI(Coach c) {
        if (getContext() == null || getActivity() == null || !isAdded())
            return;

        loadCoachImage(c.getPhotoUrl());
        nomCoach.setText(c.getNom());
        descriptionCoach.setText(c.getDescription());
        ratingCoach.setText(String.format(Locale.US, "%.1f", c.getRating()));
        nbAvis.setText(String.format(Locale.FRANCE, "(%d avis)", c.getReviewCount()));

        afficherEtoiles(layoutStarsCoach, c.getRating());
        afficherSpecialites(c.getSpecialites());
    }

    private void loadCoachImage(String photoUrl) {
        if (getContext() == null)
            return;

        if (photoUrl != null && !photoUrl.isEmpty()) {
            if (photoUrl.startsWith("data:image") || photoUrl.length() > 500) {
                try {
                    String base64Image = photoUrl;
                    if (base64Image.contains(",")) {
                        base64Image = base64Image.substring(base64Image.indexOf(",") + 1);
                    }
                    byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    if (bitmap != null) {
                        imageCoach.setImageBitmap(bitmap);
                    } else {
                        imageCoach.setImageResource(R.drawable.coach_placeholder);
                    }
                } catch (Exception e) {
                    imageCoach.setImageResource(R.drawable.coach_placeholder);
                }
            } else {
                Glide.with(this).load(photoUrl)
                        .placeholder(R.drawable.coach_placeholder)
                        .error(R.drawable.ic_placeholder)
                        .into(imageCoach);
            }
        } else {
            imageCoach.setImageResource(R.drawable.coach_placeholder);
        }
    }

    private void afficherEtoiles(LinearLayout container, double rating) {
        if (getContext() == null)
            return;
        container.removeAllViews();
        for (int i = 1; i <= 5; i++) {
            ImageView star = new ImageView(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(40, 40);
            params.setMarginEnd(8);
            star.setLayoutParams(params);
            star.setImageResource(i <= rating ? R.drawable.ic_star_filled : R.drawable.ic_star_empty);
            container.addView(star);
        }
    }

    private void afficherSpecialites(List<String> specialites) {
        if (getContext() == null)
            return;
        layoutSpecialites.removeAllViews();
        if (specialites == null || specialites.isEmpty()) {
            layoutSpecialites.setVisibility(View.GONE);
            return;
        }
        layoutSpecialites.setVisibility(View.VISIBLE);
        for (String sp : specialites) {
            TextView chip = new TextView(getContext());
            chip.setText(sp);
            chip.setTextSize(13);
            chip.setPadding(32, 12, 32, 12);
            chip.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.chip_background));
            chip.setTextColor(ContextCompat.getColor(getContext(), android.R.color.black));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(12);
            layoutSpecialites.addView(chip, lp);
        }
    }

    private void loadAvis() {
        if (coachId == null)
            return;
        db.child("coachs").child(coachId).child("avis").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                avisList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Map<String, Object> avis = (Map<String, Object>) data.getValue();
                    if (avis != null) {
                        avisList.add(avis);
                    }
                }
                avisAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("CoachDetail", "Erreur chargement avis: " + error.getMessage());
            }
        });
    }

    private void handleActions() {
        btnRetour.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        btnNoter.setOnClickListener(v -> {
            if (clientId == null) {
                Toast.makeText(getContext(), "Veuillez vous connecter pour noter", Toast.LENGTH_SHORT).show();
                return;
            }
            RatingDialogFragment dialog = RatingDialogFragment.newInstance(coachId, nomCoach.getText().toString(),
                    this);
            dialog.show(getParentFragmentManager(), "RatingDialog");
        });
    }

    @Override
    public void onRatingSubmitted() {
        // Cette méthode est appelée quand l'utilisateur valide sa note
        // La logique de sauvegarde est gérée dans le Dialog ou via un Repository
        // Ici on peut juste rafraîchir ou afficher un message si besoin
        Toast.makeText(getContext(), "Merci pour votre avis !", Toast.LENGTH_SHORT).show();
        // Le listener Firebase mettra à jour la liste des avis automatiquement
    }
}
