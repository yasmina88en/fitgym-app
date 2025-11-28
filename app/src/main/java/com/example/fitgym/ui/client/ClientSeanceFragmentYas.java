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
import android.widget.Toast;

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
import com.example.fitgym.ui.adapter.ClientSeanceAdapterYas;

import java.util.ArrayList;
import java.util.List;

public class ClientSeanceFragmentYas extends Fragment {

    private String clientId;
    private RecyclerView recyclerView;
    private ClientSeanceAdapterYas adapter;
    private DatabaseHelper databaseHelper;
    private FirebaseHelper firebaseHelper;
    private EditText searchBar;
    private Spinner spinnerFilterCategorie;

    private List<Seance> seancesList = new ArrayList<>();
    private List<Categorie> categories = new ArrayList<>();

    public ClientSeanceFragmentYas() {}

    public static ClientSeanceFragmentYas newInstance(String clientId) {
        ClientSeanceFragmentYas f = new ClientSeanceFragmentYas();
        Bundle b = new Bundle();
        b.putString("client_id", clientId);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_liste_seances_client_yas, container, false);
        recyclerView = root.findViewById(R.id.recyclerViewSeancesClientYas);
        searchBar = root.findViewById(R.id.searchBar);
        spinnerFilterCategorie = root.findViewById(R.id.spinnerFilterCategorie);

        databaseHelper = new DatabaseHelper(requireContext());
        firebaseHelper = new FirebaseHelper();

        if (getArguments() != null && getArguments().containsKey("client_id")) {
            clientId = getArguments().getString("client_id");
        }
        if (clientId == null) {
            clientId = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .getString("current_client_id", null);
        }

        adapter = new ClientSeanceAdapterYas(new ArrayList<>(), databaseHelper, clientId, new ClientSeanceAdapterYas.OnActionListener() {
            @Override public void onReserverClicked(Seance s) {
                Toast.makeText(requireContext(), "Réserver (non géré ici)", Toast.LENGTH_SHORT).show();
            }
            @Override public void onFavoriAdded(Seance s) {
                Toast.makeText(requireContext(), "Ajouté aux favoris ✅", Toast.LENGTH_SHORT).show();
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // charger données
        loadSeancesOfflineFirst();
        chargerCategoriesLocalEtRemplirSpinner();

        // recherche texte
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { filtrerParTexteEtCategorie(s.toString()); }
        });

        // spinner sélection
        spinnerFilterCategorie.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                filtrerParTexteEtCategorie(searchBar.getText().toString());
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        return root;
    }

    private void loadSeancesOfflineFirst() {
        List<Seance> local = databaseHelper.getAllSeances();
        if (local == null) local = new ArrayList<>();
        seancesList = new ArrayList<>(local);
        adapter.updateData(seancesList);

        firebaseHelper.getAllSeances(remote -> {
            if (remote != null && remote.size() > 0) {
                databaseHelper.deleteAllSeances();
                for (Seance s : remote) databaseHelper.insertOrUpdateSeance(s);
                seancesList = new ArrayList<>(databaseHelper.getAllSeances());
                requireActivity().runOnUiThread(() -> {
                    adapter.updateData(seancesList);
                    filtrerParTexteEtCategorie(searchBar.getText().toString());
                });
            }
        });
    }

    // copie conforme admin : filtres combinés texte + catégorie
    private void filtrerParTexteEtCategorie(String texte) {
        String q = texte == null ? "" : texte.trim().toLowerCase();
        int selectedPos = spinnerFilterCategorie.getSelectedItemPosition();
        String selectedCatId = null;
        if (selectedPos > 0 && selectedPos <= categories.size()) {
            selectedCatId = categories.get(selectedPos - 1).getCategorieId();
        }

        List<Seance> result = new ArrayList<>();
        for (Seance s : seancesList) {
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
