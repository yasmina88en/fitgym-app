package com.example.fitgym.ui.client;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitgym.R;
import com.example.fitgym.data.db.FirebaseHelper;
import com.example.fitgym.data.model.Coach;
import com.example.fitgym.ui.adapter.CoachAdapter;

import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ListeCoachesFragmentClient extends Fragment implements RatingDialogFragment.OnRatingSubmitListener {

    private RecyclerView coachesRecyclerView;
    private EditText searchInput;
    private LinearLayout emptyState;
    private TextView selectedFilterText;
    private CoachAdapter adapter;

    private List<Coach> coachList = new ArrayList<>();
    private List<Coach> filteredList = new ArrayList<>();
    private String selectedSpecialty = null;

    private FirebaseHelper firebaseHelper;

    public ListeCoachesFragmentClient() {
        // Constructeur public vide requis
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragement_coachs_client, container, false);

        // Liaison des vues
        coachesRecyclerView = view.findViewById(R.id.coachesRecyclerView);
        searchInput = view.findViewById(R.id.searchInput);
        emptyState = view.findViewById(R.id.emptyState);
        selectedFilterText = view.findViewById(R.id.selectedFilterText);
        ImageButton btnFilter = view.findViewById(R.id.btnFilter);

        // Initialisation de FirebaseHelper
        firebaseHelper = new FirebaseHelper();

        // Configuration du RecyclerView
        adapter = new CoachAdapter(this, filteredList);
        coachesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        coachesRecyclerView.setAdapter(adapter);

        // Gestion de l'affichage des filtres
        btnFilter.setOnClickListener(v -> showFilterDialog());

        // Écouteur pour la barre de recherche
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Chargement initial des données
        loadFirebaseCoaches();

        return view;
    }

    private void loadFirebaseCoaches() {
        firebaseHelper.getAllCoaches(coaches -> {
            if (getContext() == null || !isAdded())
                return;

            coachList.clear();
            coachList.addAll(coaches);

            if (coachList.isEmpty()) {
                Log.d("DEBUG_COACHS", "Aucun coach récupéré depuis Firebase !");
            }

            // Appliquer les filtres
            applyFilters();
        });
    }

    private void showFilterDialog() {
        if (getContext() == null)
            return;

        // Collecte de toutes les spécialités uniques
        Set<String> specialtiesSet = new HashSet<>();
        for (Coach coach : coachList) {
            if (coach.getSpecialites() != null) {
                specialtiesSet.addAll(coach.getSpecialites());
            }
        }

        List<String> specialtiesList = new ArrayList<>(specialtiesSet);
        Collections.sort(specialtiesList);
        specialtiesList.add(0, "Tout afficher"); // Option pour réinitialiser

        String[] items = specialtiesList.toArray(new String[0]);

        new AlertDialog.Builder(getContext())
                .setTitle("Choisir une spécialité")
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        selectedSpecialty = null;
                        selectedFilterText.setVisibility(View.GONE);
                    } else {
                        selectedSpecialty = items[which];
                        selectedFilterText.setText("Filtre: " + selectedSpecialty);
                        selectedFilterText.setVisibility(View.VISIBLE);
                    }
                    applyFilters();
                })
                .show();
    }

    private void applyFilters() {
        String searchText = searchInput.getText().toString().toLowerCase().trim();
        filteredList.clear();

        // Filtre la liste complète des coachs
        List<Coach> tempList = new ArrayList<>();
        for (Coach coach : coachList) {
            // Vérifie si le nom correspond au texte de recherche
            boolean matchesName = coach.getNom() != null &&
                    coach.getNom().toLowerCase().contains(searchText);

            // Vérifie si le coach a la spécialité sélectionnée
            boolean matchesSpecialty = selectedSpecialty == null ||
                    (coach.getSpecialites() != null && coach.getSpecialites().contains(selectedSpecialty));

            if (matchesName && matchesSpecialty) {
                tempList.add(coach);
            }
        }

        filteredList.addAll(tempList);
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        // Affiche ou cache le message "Aucun coach trouvé"
        if (filteredList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            coachesRecyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            coachesRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRatingSubmitted() {
        loadFirebaseCoaches();
    }
}
