package com.example.fitgym.ui.admin;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitgym.R;
import com.example.fitgym.data.dao.DAOCoach;
import com.example.fitgym.data.db.DatabaseHelper;
import com.example.fitgym.data.model.Categorie;
import com.example.fitgym.data.model.Seance;
import com.example.fitgym.ui.adapter.SeanceAdapter;
import com.example.fitgym.ui.viewmodel.SeanceViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ListeSeancesFragment extends Fragment {

    private RecyclerView recyclerView;
    private SeanceAdapter adapter;
    private List<Seance> seanceList = new ArrayList<>();
    private EditText searchBar;
    private FloatingActionButton btnAddSeance;
    private Spinner spinnerFilterCategorie;

    private SeanceViewModel seanceViewModel;
    private DatabaseHelper databaseHelper;

    private List<Categorie> categories = new ArrayList<>(); // local cache categories

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_liste_seances, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewSeances);
        searchBar = view.findViewById(R.id.searchBar);
        btnAddSeance = view.findViewById(R.id.btnAddSeance);
        spinnerFilterCategorie = view.findViewById(R.id.spinnerFilterCategorie);

        databaseHelper = new DatabaseHelper(requireContext());

        adapter = new SeanceAdapter(seanceList, new SeanceAdapter.OnItemClickListener() {
            @Override
            public void onModifierClicked(Seance seance) {
                showDialogModifierSeance(seance);
            }

            @Override
            public void onSupprimerClicked(Seance seance) {
                confirmerEtSupprimer(seance);
            }
        }, databaseHelper);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        seanceViewModel = new ViewModelProvider(this).get(SeanceViewModel.class);
        seanceViewModel.getListeSeances().observe(getViewLifecycleOwner(), seances -> {
            if (seances == null) return;
            seanceList.clear();
            seanceList.addAll(seances);
            adapter.updateData(new ArrayList<>(seanceList));
            chargerCategoriesLocalEtRemplirSpinner();
        });

        // recherche texte
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { filtrerParTexteEtCategorie(s.toString()); }
        });

        // filtre catégorie
        spinnerFilterCategorie.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view1, int position, long id) {
                filtrerParTexteEtCategorie(searchBar.getText().toString());
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // ADD -> ouvre dialog_ajouter_seance
        btnAddSeance.setOnClickListener(v -> showDialogAjouterSeance());

        // initial fill categories (local)
        chargerCategoriesLocalEtRemplirSpinner();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (databaseHelper != null) {
            databaseHelper.close();
            databaseHelper = null;
        }
    }

    // ---------------------------
    // FILTRAGE combiné texte + catégorie
    // ---------------------------
    private void filtrerParTexteEtCategorie(String texte) {
        String q = texte == null ? "" : texte.trim().toLowerCase();
        int selectedPos = spinnerFilterCategorie.getSelectedItemPosition();
        String selectedCatId = null;
        if (selectedPos > 0 && selectedPos <= categories.size()) {
            selectedCatId = categories.get(selectedPos - 1).getCategorieId();
        }

        List<Seance> result = new ArrayList<>();
        for (Seance s : seanceList) {
            boolean textMatch = q.isEmpty() || (s.getTitre() != null && s.getTitre().toLowerCase().contains(q));
            boolean catMatch = (selectedCatId == null) || (s.getCategorieId() != null && s.getCategorieId().equals(selectedCatId));
            if (textMatch && catMatch) result.add(s);
        }
        adapter.updateData(result);
    }

    // ---------------------------
    // Charger catégories depuis SQLite et remplir spinner
    // ---------------------------
    private void chargerCategoriesLocalEtRemplirSpinner() {
        categories.clear();
        categories.addAll(databaseHelper.getAllCategories());

        List<String> noms = new ArrayList<>();
        noms.add("Toutes");
        for (Categorie c : categories) {
            noms.add(c.getNom() != null ? c.getNom() : c.getCategorieId());
        }
        // Adapter personnalisé pour texte noir + fond blanc
        ArrayAdapter<String> adapterSpinner = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, noms);
        adapterSpinner.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerFilterCategorie.setAdapter(adapterSpinner);
    }

    // ---------------------------
// DIALOG Ajouter (remplacer l'ancienne méthode)
// ---------------------------
    private void showDialogAjouterSeance() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View vue = getLayoutInflater().inflate(R.layout.dialog_ajouter_seance, null);
        builder.setView(vue);
        AlertDialog dialog = builder.create();

        // find views
        EditText editTitre = vue.findViewById(R.id.editTitre);
        Spinner spinnerCategorie = vue.findViewById(R.id.spinnerCategorie);
        Spinner spinnerCoach = vue.findViewById(R.id.spinnerCoach);
        Spinner spinnerNiveau = vue.findViewById(R.id.spinnerNiveau);
        EditText editDate = vue.findViewById(R.id.editDate);
        EditText editHeure = vue.findViewById(R.id.editHeure);
        EditText editDuree = vue.findViewById(R.id.editDuree);
        EditText editPrix = vue.findViewById(R.id.editPrix);
        EditText editPlacesTotales = vue.findViewById(R.id.editPlacesTotales);
        EditText editPlacesDisponibles = vue.findViewById(R.id.editPlacesDisponibles);
        EditText editDescription = vue.findViewById(R.id.editDescription);
        MaterialButton btnAnnuler = vue.findViewById(R.id.btnAnnuler);
        MaterialButton btnAjouter = vue.findViewById(R.id.btnAjouter);

        // récupérer listes locales
        List<Categorie> cats = databaseHelper.getAllCategories();
        if (cats == null) cats = new ArrayList<>();
        List<Categorie> allCategories = new ArrayList<>(cats);

        // remplir spinnerCategorie initialement (Toutes/Aucune)
        setCategoriesIntoSpinner(spinnerCategorie, allCategories);

        // remplir coaches via DAOCoach
        DAOCoach daoCoach = new DAOCoach(databaseHelper.getWritableDatabase());
        List<com.example.fitgym.data.model.Coach> coaches = daoCoach.listerCoachs();
        if (coaches == null) coaches = new ArrayList<>();
        // nettoyer coaches invalides
        coaches.removeIf(c -> c.getId() == null || c.getId().trim().isEmpty());
        List<com.example.fitgym.data.model.Coach> allCoaches = new ArrayList<>(coaches);

        // Build coach names
        List<String> nomsCoachs = new ArrayList<>();
        nomsCoachs.add("Aucun");
        for (com.example.fitgym.data.model.Coach c : allCoaches) nomsCoachs.add(c.getNom() != null ? c.getNom() : c.getId());
        ArrayAdapter<String> adapterCoachs = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, nomsCoachs);
        adapterCoachs.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCoach.setAdapter(adapterCoachs);

        // niveau statique
        String[] niveaux = new String[]{"Débutant", "Intermédiaire", "Avancé"};
        ArrayAdapter<String> adapterNiv = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, niveaux);
        adapterNiv.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNiveau.setAdapter(adapterNiv);

        // date/time pickers
        attachDateTimePickers(editDate, editHeure);

        // Quand on choisit un coach -> filtrer catégories selon ses spécialités (noms)
        spinnerCoach.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position <= 0) {
                    // "Aucun" selected -> afficher toutes les catégories
                    setCategoriesIntoSpinner(spinnerCategorie, allCategories);
                } else {
                    com.example.fitgym.data.model.Coach selectedCoach = allCoaches.get(position - 1);
                    refreshCategorieSpinnerForCoach(selectedCoach, allCategories, spinnerCategorie);
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btnAnnuler.setOnClickListener(v -> dialog.dismiss());

        btnAjouter.setOnClickListener(v -> {
            String titre = editTitre.getText().toString().trim();
            if (titre.isEmpty()) { Toast.makeText(getContext(), "Titre requis", Toast.LENGTH_SHORT).show(); return; }

            String niveauSel = (String) spinnerNiveau.getSelectedItem();
            String date = editDate.getText().toString().trim();
            String heure = editHeure.getText().toString().trim();
            int duree = parseIntSafe(editDuree.getText().toString().trim());
            double prix = parseDoubleSafe(editPrix.getText().toString().trim());
            int placesTot = parseIntSafe(editPlacesTotales.getText().toString().trim());
            int placesDisp = parseIntSafe(editPlacesDisponibles.getText().toString().trim());
            String description = editDescription.getText().toString().trim();

            // déterminer categorieId depuis spinnerCategorie (adapter contient "Aucune" + noms)
            String categorieId = null;
            int selCat = spinnerCategorie.getSelectedItemPosition();
            if (selCat > 0) {
                String catNom = (String) spinnerCategorie.getSelectedItem();
                // chercher la categorie par nom (insensible casse)
                for (Categorie c : allCategories) {
                    if (c.getNom() != null && c.getNom().equalsIgnoreCase(catNom)) {
                        categorieId = c.getCategorieId();
                        break;
                    }
                }
            }

            // déterminer coachId depuis spinnerCoach
            String coachId = null;
            int selCoach = spinnerCoach.getSelectedItemPosition();
            if (selCoach > 0 && selCoach <= allCoaches.size()) coachId = allCoaches.get(selCoach - 1).getId();

            Seance s = new Seance();
            s.setTitre(titre);
            s.setNiveau(niveauSel);
            s.setDate(date);
            s.setHeure(heure);
            s.setDuree(duree);
            s.setPrix(prix);
            s.setPlacesTotales(placesTot);
            s.setPlacesDisponibles(placesDisp);
            s.setDescription(description);
            s.setCategorieId(categorieId);
            s.setCoachId(coachId != null ? coachId : "");

            seanceViewModel.ajouterSeance(s);
            dialog.dismiss();
            Toast.makeText(getContext(), "Séance ajoutée", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }
    // ---------------------------
// DIALOG Modifier (remplacer l'ancienne méthode)
// ---------------------------
    private void showDialogModifierSeance(@Nullable Seance seance) {
        if (seance == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View vue = getLayoutInflater().inflate(R.layout.dialog_modifier_seance, null);
        builder.setView(vue);
        AlertDialog dialog = builder.create();

        // find views
        EditText editTitre = vue.findViewById(R.id.editTitre);
        Spinner spinnerCategorie = vue.findViewById(R.id.spinnerCategorie);
        Spinner spinnerCoach = vue.findViewById(R.id.spinnerCoach);
        Spinner spinnerNiveau = vue.findViewById(R.id.spinnerNiveau);
        EditText editDate = vue.findViewById(R.id.editDate);
        EditText editHeure = vue.findViewById(R.id.editHeure);
        EditText editDuree = vue.findViewById(R.id.editDuree);
        EditText editPrix = vue.findViewById(R.id.editPrix);
        EditText editPlacesTotales = vue.findViewById(R.id.editPlacesTotales);
        EditText editPlacesDisponibles = vue.findViewById(R.id.editPlacesDisponibles);
        EditText editDescription = vue.findViewById(R.id.editDescription);
        MaterialButton btnAnnuler = vue.findViewById(R.id.btnAnnuler);
        MaterialButton btnModifier = vue.findViewById(R.id.btnModifier);

        // récupérer listes locales
        List<Categorie> cats = databaseHelper.getAllCategories();
        if (cats == null) cats = new ArrayList<>();
        List<Categorie> allCategories = new ArrayList<>(cats);

        // remplir spinnerCategorie initialement avec toutes (sera filtré plus bas)
        setCategoriesIntoSpinner(spinnerCategorie, allCategories);

        // remplir coaches via DAOCoach
        DAOCoach daoCoach = new DAOCoach(databaseHelper.getWritableDatabase());
        List<com.example.fitgym.data.model.Coach> coaches = daoCoach.listerCoachs();
        if (coaches == null) coaches = new ArrayList<>();
        coaches.removeIf(c -> c.getId() == null || c.getId().trim().isEmpty());
        List<com.example.fitgym.data.model.Coach> allCoaches = new ArrayList<>(coaches);

        // Build coach names
        List<String> nomsCoachs = new ArrayList<>();
        nomsCoachs.add("Aucun");
        for (com.example.fitgym.data.model.Coach c : allCoaches) nomsCoachs.add(c.getNom() != null ? c.getNom() : c.getId());
        ArrayAdapter<String> adapterCoachs = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, nomsCoachs);
        adapterCoachs.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCoach.setAdapter(adapterCoachs);

        // niveau statique
        String[] niveaux = new String[]{"Débutant", "Intermédiaire", "Avancé"};
        ArrayAdapter<String> adapterNiv = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, niveaux);
        adapterNiv.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNiveau.setAdapter(adapterNiv);

        // pré-remplir valeurs texte/date/heure
        editTitre.setText(seance.getTitre());
        editDate.setText(seance.getDate());
        editHeure.setText(seance.getHeure());
        editDuree.setText(String.valueOf(seance.getDuree()));
        editPrix.setText(String.valueOf(seance.getPrix()));
        editPlacesTotales.setText(String.valueOf(seance.getPlacesTotales()));
        editPlacesDisponibles.setText(String.valueOf(seance.getPlacesDisponibles()));
        editDescription.setText(seance.getDescription());

        // Quand on choisit un coach -> filtrer catégories (même listener que ajout)
        spinnerCoach.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position <= 0) {
                    setCategoriesIntoSpinner(spinnerCategorie, allCategories);
                } else {
                    com.example.fitgym.data.model.Coach selectedCoach = allCoaches.get(position - 1);
                    refreshCategorieSpinnerForCoach(selectedCoach, allCategories, spinnerCategorie);
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // sélectionner coach courant (si défini) — ceci déclenchera le listener et filtrera le spinnerCategorie
        if (seance.getCoachId() != null && !seance.getCoachId().isEmpty()) {
            for (int i = 0; i < allCoaches.size(); i++) {
                if (allCoaches.get(i).getId().equals(seance.getCoachId())) {
                    spinnerCoach.setSelection(i + 1);
                    break;
                }
            }
        } else {
            // pas de coach : laisser "Aucun" et afficher toutes catégories
            setCategoriesIntoSpinner(spinnerCategorie, allCategories);
        }

        // attendre que spinnerCategorie soit rempli/filtré puis sélectionner la catégorie existante
        // -------- CORRECTION : sélection automatique de la catégorie --------
        String categorieInitialeId = seance.getCategorieId();

// On va attendre que le spinnerCoach finisse de filtrer la liste
        spinnerCoach.post(() -> {
            Categorie catInitiale = databaseHelper.getCategorieById(categorieInitialeId);
            if (catInitiale == null) return;
            String nomCatInitiale = catInitiale.getNom();
            if (nomCatInitiale == null) return;

            ArrayAdapter adapterCat = (ArrayAdapter) spinnerCategorie.getAdapter();
            if (adapterCat == null) return;

            int pos = -1;
            for (int i = 0; i < adapterCat.getCount(); i++) {
                Object item = adapterCat.getItem(i);
                if (item != null && item.toString().equalsIgnoreCase(nomCatInitiale)) {
                    pos = i;
                    break;
                }
            }

            if (pos >= 0) {
                spinnerCategorie.setSelection(pos);
            }
        });


        // niveau
        for (int i = 0; i < niveaux.length; i++) {
            if (niveaux[i].equalsIgnoreCase(seance.getNiveau())) {
                spinnerNiveau.setSelection(i);
                break;
            }
        }

        // date/time pickers
        attachDateTimePickers(editDate, editHeure);

        btnAnnuler.setOnClickListener(v -> dialog.dismiss());

        btnModifier.setOnClickListener(v -> {
            String titre = editTitre.getText().toString().trim();
            if (titre.isEmpty()) { Toast.makeText(getContext(), "Titre requis", Toast.LENGTH_SHORT).show(); return; }

            String niveauSel = (String) spinnerNiveau.getSelectedItem();
            String date = editDate.getText().toString().trim();
            String heure = editHeure.getText().toString().trim();
            int duree = parseIntSafe(editDuree.getText().toString().trim());
            double prix = parseDoubleSafe(editPrix.getText().toString().trim());
            int placesTot = parseIntSafe(editPlacesTotales.getText().toString().trim());
            int placesDisp = parseIntSafe(editPlacesDisponibles.getText().toString().trim());
            String description = editDescription.getText().toString().trim();

            String categorieId = null;
            int selCat = spinnerCategorie.getSelectedItemPosition();
            if (selCat > 0) {
                String catNom = (String) spinnerCategorie.getSelectedItem();
                for (Categorie c : allCategories) {
                    if (c.getNom() != null && c.getNom().equalsIgnoreCase(catNom)) {
                        categorieId = c.getCategorieId();
                        break;
                    }
                }
            }

            String coachId = null;
            int selCoach = spinnerCoach.getSelectedItemPosition();
            if (selCoach > 0 && selCoach <= allCoaches.size()) coachId = allCoaches.get(selCoach - 1).getId();

            seance.setTitre(titre);
            seance.setNiveau(niveauSel);
            seance.setDate(date);
            seance.setHeure(heure);
            seance.setDuree(duree);
            seance.setPrix(prix);
            seance.setPlacesTotales(placesTot);
            seance.setPlacesDisponibles(placesDisp);
            seance.setDescription(description);
            seance.setCategorieId(categorieId);
            seance.setCoachId(coachId != null ? coachId : "");

            seanceViewModel.modifierSeance(seance);
            dialog.dismiss();
            Toast.makeText(getContext(), "Séance modifiée", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    // helper : attache date/time pickers à deux EditText
    private void attachDateTimePickers(EditText editDate, EditText editHeure) {
        editDate.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog dpd = new DatePickerDialog(requireContext(), (DatePicker view, int y, int m, int d) -> {
                String dd = String.format("%04d-%02d-%02d", y, m + 1, d);
                editDate.setText(dd);
            }, year, month, day);
            dpd.show();
        });

        editHeure.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);
            TimePickerDialog tpd = new TimePickerDialog(requireContext(), (TimePicker view, int h, int m) -> {
                String hh = String.format("%02d:%02d", h, m);
                editHeure.setText(hh);
            }, hour, minute, true);
            tpd.show();
        });
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    private double parseDoubleSafe(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0d; }
    }

    // ---------------------------
    // SUPPRESSION (confirm)
    // ---------------------------
    private void confirmerEtSupprimer(Seance seance) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Supprimer séance")
                .setMessage("Voulez-vous vraiment supprimer \"" + (seance.getTitre() != null ? seance.getTitre() : "") + "\" ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    seanceViewModel.supprimerSeance(seance);
                    Toast.makeText(getContext(), "Séance supprimée", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss())
                .show();
    }
    // Remplit spinner avec "Aucune" + toutes les catégories fournies (affiche noms)
    private void setCategoriesIntoSpinner(Spinner spinner, List<Categorie> categories) {
        List<String> noms = new ArrayList<>();
        noms.add("Aucune");
        for (Categorie c : categories) noms.add(c.getNom() != null ? c.getNom() : c.getCategorieId());
        ArrayAdapter<String> adapterSpinner = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, noms);
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapterSpinner);
    }

    // Filtre les categories selon coach.getSpecialites() (specialites contient des noms de catégories).
    private void refreshCategorieSpinnerForCoach(com.example.fitgym.data.model.Coach coach,
                                                 List<Categorie> allCategories,
                                                 Spinner spinnerCategorie) {
        List<String> specs = coach.getSpecialites();
        if (specs == null || specs.isEmpty()) {
            // aucun filtre -> afficher toutes
            setCategoriesIntoSpinner(spinnerCategorie, allCategories);
            return;
        }

        // normaliser spécialités (minuscules)
        List<String> specsNorm = new ArrayList<>();
        for (String s : specs) if (s != null) specsNorm.add(s.trim().toLowerCase());

        List<Categorie> filtered = new ArrayList<>();
        for (Categorie cat : allCategories) {
            String catNom = cat.getNom() != null ? cat.getNom().trim().toLowerCase() : null;
            if (catNom != null && specsNorm.contains(catNom)) {
                filtered.add(cat);
            }
        }

        if (filtered.isEmpty()) {
            // si aucune correspondance, afficher toutes (ou tu peux afficher vide selon ton choix)
            setCategoriesIntoSpinner(spinnerCategorie, allCategories);
            return;
        }

        // remplir spinner avec filtered
        List<String> noms = new ArrayList<>();
        noms.add("Aucune");
        for (Categorie c : filtered) noms.add(c.getNom() != null ? c.getNom() : c.getCategorieId());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, noms);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategorie.setAdapter(adapter);
    }

}
