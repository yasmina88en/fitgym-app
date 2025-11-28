package com.example.fitgym.ui.client;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitgym.R;
import com.example.fitgym.data.db.DatabaseHelper;
import com.example.fitgym.data.db.FirebaseHelper;
import com.example.fitgym.data.model.Categorie;
import com.example.fitgym.data.model.Seance;
import com.example.fitgym.ui.adapter.FavorisAdapter;

import java.util.ArrayList;
import java.util.List;

public class FavorisFragment extends Fragment {

    private String clientId;
    private DatabaseHelper databaseHelper;
    private FirebaseHelper firebaseHelper;
    private FavorisAdapter adapter;
    private RecyclerView rvFavoris;
    private EditText searchBar;
    private Spinner spinnerFilterCategorie;

    private List<Seance> favorisSeances = new ArrayList<>();
    private List<Categorie> categories = new ArrayList<>();

    public FavorisFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_liste_favoris, container, false);
        rvFavoris = root.findViewById(R.id.recyclerViewFavoris);
        searchBar = root.findViewById(R.id.searchBar);
        spinnerFilterCategorie = root.findViewById(R.id.spinnerFilterCategorie);

        databaseHelper = new DatabaseHelper(requireContext());
        firebaseHelper = new FirebaseHelper();

        if (getArguments() != null && getArguments().containsKey("client_id"))
            clientId = getArguments().getString("client_id");

        if (clientId == null) {
            clientId = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .getString("current_client_id", null);
        }

        adapter = new FavorisAdapter(new ArrayList<>(), databaseHelper, clientId, firebaseHelper, () -> {
            // callback when adapter removed an item: reload favourites
            loadFavoris();
        });

        rvFavoris.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFavoris.setAdapter(adapter);

        chargerCategoriesLocalEtRemplirSpinner();
        loadFavoris();

        // filtre texte
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable e) { filtrerParTexteEtCategorie(e.toString()); }
        });

        spinnerFilterCategorie.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                filtrerParTexteEtCategorie(searchBar.getText().toString());
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        return root;
    }

    private void loadFavoris() {
        if (clientId == null) return;

        favorisSeances.clear();
        List<String> seanceIds = databaseHelper.getFavorisParClient(clientId);
        if (seanceIds == null || seanceIds.isEmpty()) {
            adapter.updateData(new ArrayList<>());
            return;
        }

        // charger local ou remote si manquant
        for (String sid : seanceIds) {
            Seance local = databaseHelper.getSeanceById(sid);
            if (local != null) {
                favorisSeances.add(local);
            } else {
                firebaseHelper.getSeanceById(sid, seance -> {
                    if (seance != null) {
                        databaseHelper.insertOrUpdateSeance(seance);
                        favorisSeances.add(seance);
                        requireActivity().runOnUiThread(() -> adapter.updateData(new ArrayList<>(favorisSeances)));
                    }
                });
            }
        }
        adapter.updateData(new ArrayList<>(favorisSeances));
    }

    private void filtrerParTexteEtCategorie(String texte) {
        String q = texte == null ? "" : texte.trim().toLowerCase();
        int selectedPos = spinnerFilterCategorie.getSelectedItemPosition();
        String selectedCatId = null;
        if (selectedPos > 0 && selectedPos <= categories.size()) selectedCatId = categories.get(selectedPos - 1).getCategorieId();

        List<Seance> result = new ArrayList<>();
        for (Seance s : favorisSeances) {
            boolean textMatch = q.isEmpty() || (s.getTitre() != null && s.getTitre().toLowerCase().contains(q));
            boolean catMatch = (selectedCatId == null) || (s.getCategorieId() != null && s.getCategorieId().equals(selectedCatId));
            if (textMatch && catMatch) result.add(s);
        }
        adapter.updateData(result);
    }

    private void chargerCategoriesLocalEtRemplirSpinner() {
        categories.clear();
        categories.addAll(databaseHelper.getAllCategories());

        List<String> noms = new ArrayList<>();
        noms.add("Toutes");
        for (Categorie c : categories) noms.add(c.getNom() != null ? c.getNom() : c.getCategorieId());

        ArrayAdapter<String> adapterSpinner = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, noms);
        adapterSpinner.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerFilterCategorie.setAdapter(adapterSpinner);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (databaseHelper != null) { databaseHelper.close(); databaseHelper = null; }
    }
}
