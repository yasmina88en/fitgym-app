package com.example.fitgym.ui.admin;

import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.fitgym.R;
import com.example.fitgym.data.dao.DAOCoach;
import com.example.fitgym.data.db.DatabaseHelper;
import com.example.fitgym.data.db.FirebaseHelper;
import com.example.fitgym.data.model.Coach;
import com.example.fitgym.data.model.Seance;
import com.example.fitgym.ui.viewmodel.CoachViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ListeCoachsFragment extends Fragment {

    private CoachViewModel viewModelCoach;
    private RecyclerView recyclerViewCoachs;
    private AdaptateurCoach adaptateur;
    private List<Coach> listeCompleteCoachs = new ArrayList<>();
    private List<Coach> listeAffichee = new ArrayList<>();
    private FloatingActionButton fabAjouterCoach;
    private EditText champRecherche;
    private static final int PICK_IMAGE = 101;
    private Uri currentImageUri = null;
    private ImageView imagePreviewActive;
    private ImageButton btnFilter;

    private DatabaseHelper dbHelper;
    private DAOCoach daoCoach;

    private String imageBase64 = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View vue = inflater.inflate(R.layout.activity_liste_coachs, container, false);

        // --- Initialisations ---
        dbHelper = new DatabaseHelper(getContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        daoCoach = new DAOCoach(db);

        viewModelCoach = new ViewModelProvider(this).get(CoachViewModel.class);
        recyclerViewCoachs = vue.findViewById(R.id.coachesRecyclerView);
        champRecherche = vue.findViewById(R.id.searchInput);
        fabAjouterCoach = vue.findViewById(R.id.fabAddCoach);
        btnFilter = vue.findViewById(R.id.btnFilter);

        btnFilter.setOnClickListener(v -> afficherDialogFiltre());

        adaptateur = new AdaptateurCoach();
        recyclerViewCoachs.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewCoachs.setAdapter(adaptateur);

        configurerEcouteurs();

        // --- 1️⃣ Charger depuis SQLite (local) ---
        List<Coach> localCoachs = daoCoach.listerCoachs();
        localCoachs.removeIf(c -> c.getId() == null || c.getId().trim().isEmpty());

        // Supprimer les fantômes de SQLite
        for (Coach c : new ArrayList<>(localCoachs)) {
            if (c.getId() == null || c.getId().trim().isEmpty()) {
                daoCoach.supprimerCoachSansId(c.getNomComplet());
            }
        }

        listeCompleteCoachs.clear();
        listeCompleteCoachs.addAll(localCoachs);
        listeAffichee.clear();
        listeAffichee.addAll(localCoachs);
        adaptateur.definirCoachs(listeAffichee);

        Log.d("ListeCoachsFragment", "Coachs locaux chargés : " + localCoachs.size());

        // --- 2️⃣ Charger depuis Firebase (cloud) ---
        FirebaseHelper firebaseHelper = new FirebaseHelper();
        firebaseHelper.getAllCoaches(coaches -> {
            if (coaches == null || coaches.isEmpty()) return;

            // Facultatif : calculer les séances pour chaque coach si nécessaire
            for (Coach c : coaches) {
                firebaseHelper.getAllSeances(snapshot -> {
                    int nbSeances = 0;
                    for (Seance s : snapshot) {
                        if (s.getCoachId() != null && s.getCoachId().equals(c.getId())) {
                            nbSeances++;
                        }
                    }
                    c.setSessionCount(nbSeances);
                    adaptateur.notifyDataSetChanged();
                });
            }

            // Mise à jour des listes et adapter
            listeCompleteCoachs.clear();
            listeCompleteCoachs.addAll(coaches);
            listeAffichee.clear();
            listeAffichee.addAll(coaches);
            adaptateur.definirCoachs(listeAffichee);

            // Synchroniser SQLite pour avoir la version à jour
            daoCoach.viderCoachs();
            for (Coach c : coaches) {
                daoCoach.ajouterCoach(c);
            }

            Log.d("ListeCoachsFragment", "Coachs Firebase chargés : " + coaches.size());
        });

        return vue;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    private void afficherDialogFiltre() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filtre_coach, null);
        Spinner spinnerFiltre = dialogView.findViewById(R.id.spinnerFiltre);
        Spinner spinnerSort = dialogView.findViewById(R.id.spinnerSort);

        String[] filtres = {"Tous", "Yoga", "Crossfit", "Cardio","Musculation","Pilates","Zumba","Box","Stretching"};
        String[] tris = {"Nom (A-Z)", "Nom (Z-A)", "Plus populaire ↑", "Séances ↑"};

        spinnerFiltre.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, filtres));
        spinnerSort.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, tris));

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Filtrer et trier les coachs");
        builder.setView(dialogView);

        builder.setPositiveButton("Appliquer", (dialog, which) -> {
            String filtreChoisi = spinnerFiltre.getSelectedItem().toString();
            String triChoisi = spinnerSort.getSelectedItem().toString();
            appliquerFiltreEtTri(filtreChoisi, triChoisi);
        });

        builder.setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void appliquerFiltreEtTri(String filtre, String tri) {
        if (listeCompleteCoachs.isEmpty()) {
            Toast.makeText(getContext(), "Aucun coach disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Coach> resultat = new ArrayList<>();
        for (Coach c : listeCompleteCoachs) {
            if (filtre.equals("Tous") || (c.getSpecialites() != null && c.getSpecialites().contains(filtre))) {
                resultat.add(c);
            }
        }

        switch (tri) {
            case "Nom (A-Z)":
                Collections.sort(resultat, (c1, c2) -> c1.getNomComplet().compareToIgnoreCase(c2.getNomComplet()));
                break;
            case "Nom (Z-A)":
                Collections.sort(resultat, (c1, c2) -> c2.getNomComplet().compareToIgnoreCase(c1.getNomComplet()));
                break;
            case "Plus populaire ↑":
                Collections.sort(resultat, (c1, c2) -> Double.compare(c2.getRating(), c1.getRating()));
                break;
            case "Séances ↑":
                Collections.sort(resultat, (c1, c2) -> Integer.compare(c2.getSessionCount(), c1.getSessionCount()));
                break;
        }

        listeAffichee.clear();
        listeAffichee.addAll(resultat);
        adaptateur.definirCoachs(listeAffichee);
    }

    private void configurerEcouteurs() {
        fabAjouterCoach.setOnClickListener(v -> afficherDialogAjouterCoach());
        champRecherche.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adaptateur != null) adaptateur.getFilter().filter(s);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void observerViewModel() {
        viewModelCoach.getListeCoachs().observe(getViewLifecycleOwner(), coaches -> {
            if (coaches == null || coaches.isEmpty()) {
                return;
            }

            listeCompleteCoachs.clear();
            listeCompleteCoachs.addAll(coaches);
            listeAffichee.clear();
            listeAffichee.addAll(coaches);
            adaptateur.definirCoachs(listeAffichee);

            // Sync Firebase -> SQLite : vider et réinsérer
            daoCoach.viderCoachs();
            for (Coach c : coaches) {
                daoCoach.ajouterCoach(c);
            }
        });
    }

    private void afficherDialogAjouterCoach() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View vueDialog = getLayoutInflater().inflate(R.layout.dialog_ajout_coach, null);
        builder.setView(vueDialog);
        AlertDialog dialog = builder.create();

        TextInputEditText edtNom = vueDialog.findViewById(R.id.inputNomComplet);
        TextInputEditText edtDescription = vueDialog.findViewById(R.id.inputBiographie);
        MultiAutoCompleteTextView inputSpecialites = vueDialog.findViewById(R.id.inputSpecialites);
        ChipGroup chipGroupSpecialites = vueDialog.findViewById(R.id.chipGroupSpecialites);
        MaterialButton btnChoisirImage = vueDialog.findViewById(R.id.inputUrlImage);
        Button btnAnnuler = vueDialog.findViewById(R.id.btnAnnuler);
        Button btnAjouter = vueDialog.findViewById(R.id.btnModifier);
        ImageView preview = vueDialog.findViewById(R.id.imagePreview1);

        preview.setImageResource(R.drawable.ic_placeholder);
        imagePreviewActive = preview;
        currentImageUri = null;
        imageBase64 = null;

        // Liste des spécialités disponibles
        // Préparer l'AutoComplete
        String[] specialitesDisponibles = {"Yoga", "Crossfit", "Cardio", "Musculation", "Pilates", "Zumba", "Box", "Stretching"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line, specialitesDisponibles);

        inputSpecialites.setAdapter(adapter);
        inputSpecialites.setThreshold(1); // Commence à proposer dès 1 caractère
        inputSpecialites.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

// Ajouter chip quand on sélectionne une spécialité
        inputSpecialites.setOnItemClickListener((parent, view, position, id) -> {
            String specialiteChoisie = adapter.getItem(position);
            if (specialiteChoisie == null) return;

            // Vérifier doublons
            for (int i = 0; i < chipGroupSpecialites.getChildCount(); i++) {
                Chip chip = (Chip) chipGroupSpecialites.getChildAt(i);
                if (chip.getText().toString().equalsIgnoreCase(specialiteChoisie)) return;
            }

            Chip chip = new Chip(getContext());
            chip.setText(specialiteChoisie);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(c -> chipGroupSpecialites.removeView(chip));
            chipGroupSpecialites.addView(chip);

            inputSpecialites.setText(""); // clear input
        });


        btnAnnuler.setOnClickListener(v -> dialog.dismiss());

        btnChoisirImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE);
        });

        btnAjouter.setOnClickListener(v -> {
            String nom = edtNom.getText().toString().trim();
            String desc = edtDescription.getText().toString().trim();

            if (nom.isEmpty()) {
                Toast.makeText(getContext(), "Nom obligatoire", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔹 Récupérer les spécialités depuis les chips
            List<String> specialitesChoisies = new ArrayList<>();
            for (int i = 0; i < chipGroupSpecialites.getChildCount(); i++) {
                Chip chip = (Chip) chipGroupSpecialites.getChildAt(i);
                specialitesChoisies.add(chip.getText().toString());
            }

            Coach nouveauCoach = new Coach();
            nouveauCoach.setNom(nom);
            nouveauCoach.setDescription(desc);
            nouveauCoach.setSpecialites(specialitesChoisies);

            if (imageBase64 != null && imageBase64.trim().length() > 20) {
                nouveauCoach.setPhotoUrl(imageBase64);
            } else {
                nouveauCoach.setPhotoUrl("");
            }

            currentImageUri = null;
            imageBase64 = null;


            new FirebaseHelper().ajouterCoach(nouveauCoach, success -> {
                if (success) {
                    Toast.makeText(getContext(), "Coach ajouté !", Toast.LENGTH_SHORT).show();
                    listeCompleteCoachs.add(0, nouveauCoach);
                    adaptateur.definirCoachs(listeCompleteCoachs);
                    dialog.dismiss();
                    daoCoach.ajouterCoach(nouveauCoach);
                } else {
                    Toast.makeText(getContext(), "Erreur lors de l'ajout", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }


    private void afficherDialogModifierCoach(Coach coach) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View vueDialog = getLayoutInflater().inflate(R.layout.dialog_modifier_coach, null);
        builder.setView(vueDialog);
        AlertDialog dialog = builder.create();

        TextInputEditText inputNom = vueDialog.findViewById(R.id.inputNomComplet);
        MultiAutoCompleteTextView inputSpecialites = vueDialog.findViewById(R.id.inputSpecialites);
        ChipGroup chipGroupSpecialites = vueDialog.findViewById(R.id.chipGroupSpecialites);
        TextInputEditText inputBio = vueDialog.findViewById(R.id.inputBiographie);
        MaterialButton btnChoisirImage = vueDialog.findViewById(R.id.inputUrlImage);
        MaterialButton btnModifier = vueDialog.findViewById(R.id.btnModifier);
        MaterialButton btnAnnuler = vueDialog.findViewById(R.id.btnAnnuler);
        ImageView preview = vueDialog.findViewById(R.id.imagePreview1);

        inputNom.setText(coach.getNomComplet());
        inputBio.setText(coach.getDescription());

        // Charger les chips existants
        chipGroupSpecialites.removeAllViews();
        if (coach.getSpecialites() != null) {
            for (String s : coach.getSpecialites()) {
                Chip chip = new Chip(getContext());
                chip.setText(s);
                chip.setCloseIconVisible(true);
                chip.setOnCloseIconClickListener(c -> chipGroupSpecialites.removeView(chip));
                chipGroupSpecialites.addView(chip);
            }
        }

        // Préparer l'AutoComplete
        // Préparer l'AutoComplete
        String[] specialitesDisponibles = {"Yoga", "Crossfit", "Cardio","Musculation","Pilates","Zumba","Box","Stretching"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line, specialitesDisponibles);

        inputSpecialites.setAdapter(adapter);
        inputSpecialites.setThreshold(1); // Commence à proposer dès 1 caractère
        inputSpecialites.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

// Ajouter chip quand on sélectionne une spécialité
        inputSpecialites.setOnItemClickListener((parent, view, position, id) -> {
            String specialiteChoisie = adapter.getItem(position);
            if (specialiteChoisie == null) return;

            // Vérifier doublons
            for (int i = 0; i < chipGroupSpecialites.getChildCount(); i++) {
                Chip chip = (Chip) chipGroupSpecialites.getChildAt(i);
                if (chip.getText().toString().equalsIgnoreCase(specialiteChoisie)) return;
            }

            Chip chip = new Chip(getContext());
            chip.setText(specialiteChoisie);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(c -> chipGroupSpecialites.removeView(chip));
            chipGroupSpecialites.addView(chip);

            inputSpecialites.setText(""); // clear input
        });

        // Préparer l'image
        String base64Image = coach.getPhotoUrl();
        if (base64Image != null && base64Image.trim().length() > 20) {
            Bitmap bitmap = convertBase64ToBitmap(base64Image);
            if (bitmap != null) {
                Glide.with(getContext())
                        .load(bitmap)
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_placeholder)
                        .into(preview);
            } else preview.setImageResource(R.drawable.ic_placeholder);
        } else preview.setImageResource(R.drawable.ic_placeholder);

        imagePreviewActive = preview;
        currentImageUri = null;
        imageBase64 = null;

        btnAnnuler.setOnClickListener(v -> dialog.dismiss());

        btnChoisirImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE);
            imagePreviewActive = preview;
        });

        btnModifier.setOnClickListener(v -> {
            String nom = inputNom.getText().toString().trim();
            String desc = inputBio.getText().toString().trim();

            if (nom.isEmpty()) {
                Toast.makeText(getContext(), "Nom obligatoire", Toast.LENGTH_SHORT).show();
                return;
            }

            // Récupérer toutes les spécialités depuis les chips
            List<String> specialitesChoisies = new ArrayList<>();
            for (int i = 0; i < chipGroupSpecialites.getChildCount(); i++) {
                Chip chip = (Chip) chipGroupSpecialites.getChildAt(i);
                specialitesChoisies.add(chip.getText().toString());
            }

            coach.setNom(nom);
            coach.setDescription(desc);
            coach.setSpecialites(specialitesChoisies);

            if (imageBase64 != null && imageBase64.trim().length() > 20) {
                coach.setPhotoUrl(imageBase64);
            }

            currentImageUri = null;
            imageBase64 = null;

            new FirebaseHelper().modifierCoach(coach, success -> {
                if (success) adaptateur.notifyItemChanged(listeCompleteCoachs.indexOf(coach));
                dialog.dismiss();
                daoCoach.modifierCoach(coach);
            });
        });

        dialog.show();
    }


    private String convertUriToBase64(Uri uri) {
        try {
            InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            Bitmap bitmapReduit = resizeBitmap(bitmap, 800);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmapReduit.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e("Base64", "Erreur de conversion Uri en Base64", e);
            Toast.makeText(getContext(), "Erreur conversion image", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    public Bitmap convertBase64ToBitmap(String base64Str) {
        try {
            if (base64Str == null || base64Str.trim().length() < 20) return null;
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            Log.e("Base64", "Erreur de décodage Base64", e);
            return null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            currentImageUri = data.getData();
            imageBase64 = convertUriToBase64(currentImageUri);
            if (imagePreviewActive != null) imagePreviewActive.setImageURI(currentImageUri);
            if (imageBase64 == null) {
                Toast.makeText(getContext(), "Erreur lors de la conversion de l'image", Toast.LENGTH_LONG).show();
            }
        }
    }

    // --- Adapter ---
    private class AdaptateurCoach extends RecyclerView.Adapter<AdaptateurCoach.ViewHolderCoach>
            implements android.widget.Filterable {

        private List<Coach> listeAff = new ArrayList<>();
        private List<Coach> listeCompl = new ArrayList<>();

        public void definirCoachs(List<Coach> liste) {
            listeAff.clear();
            listeAff.addAll(liste);
            listeCompl.clear();
            listeCompl.addAll(liste);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolderCoach onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View vue = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_coach, parent, false);
            return new ViewHolderCoach(vue);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolderCoach holder, int position) {
            holder.lierDonnees(listeAff.get(position));
        }

        @Override
        public int getItemCount() {
            return listeAff.size();
        }

        @Override
        public android.widget.Filter getFilter() {
            return filtreCoach;
        }

        private final android.widget.Filter filtreCoach = new android.widget.Filter() {
            @Override
            protected android.widget.Filter.FilterResults performFiltering(CharSequence constraint) {
                List<Coach> listeFiltree = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    listeFiltree.addAll(listeCompl);
                } else {
                    String motif = constraint.toString().toLowerCase(Locale.FRANCE).trim();
                    for (Coach c : listeCompl) {
                        if ((c.getNomComplet() != null && c.getNomComplet().toLowerCase(Locale.FRANCE).contains(motif))
                                || (c.getDescription() != null && c.getDescription().toLowerCase(Locale.FRANCE).contains(motif))
                                || (c.getSpecialites() != null && c.getSpecialites().toString().toLowerCase(Locale.FRANCE).contains(motif))) {
                            listeFiltree.add(c);
                        }
                    }
                }
                android.widget.Filter.FilterResults result = new android.widget.Filter.FilterResults();
                result.values = listeFiltree;
                return result;
            }

            @Override
            protected void publishResults(CharSequence constraint, android.widget.Filter.FilterResults results) {
                listeAff.clear();
                if (results.values != null) {
                    listeAff.addAll((List<Coach>) results.values);
                }
                notifyDataSetChanged();
            }
        };

        class ViewHolderCoach extends RecyclerView.ViewHolder {
            CircleImageView imgProfil;
            TextView txtNom, txtDescription, txtNote, txtNbSeances;
            ChipGroup groupeDeChips;
            MaterialButton btnModifier;
            Button btnSupprimer;

            public ViewHolderCoach(@NonNull View itemView) {
                super(itemView);
                imgProfil = itemView.findViewById(R.id.coachAvatar);
                txtNom = itemView.findViewById(R.id.coachName);
                txtDescription = itemView.findViewById(R.id.coachDescription);
                txtNote = itemView.findViewById(R.id.coachRating);
                txtNbSeances = itemView.findViewById(R.id.coachSessionsCount);
                groupeDeChips = itemView.findViewById(R.id.chipGroupSpecialties);
                btnModifier = itemView.findViewById(R.id.btnModifier);
                btnSupprimer = itemView.findViewById(R.id.btnSupprimer);
            }

            public void lierDonnees(Coach coach) {
                txtNom.setText(coach.getNomComplet());
                txtDescription.setText(coach.getDescription());
                txtNote.setText(String.format(Locale.FRANCE, "%.1f (%d avis)", coach.getRating(), coach.getReviewCount()));
                txtNbSeances.setText(coach.getSessionCount() + " séances");

                String base64Image = coach.getPhotoUrl();
                if (base64Image != null && base64Image.trim().length() > 20) {
                    Bitmap bitmap = convertBase64ToBitmap(base64Image);
                    if (bitmap != null) {
                        Glide.with(itemView.getContext())
                                .load(bitmap)
                                .placeholder(R.drawable.ic_placeholder)
                                .error(R.drawable.ic_placeholder)
                                .into(imgProfil);
                    } else {
                        imgProfil.setImageResource(R.drawable.ic_placeholder);
                    }
                } else {
                    imgProfil.setImageResource(R.drawable.ic_placeholder);
                }

                groupeDeChips.removeAllViews();
                if (coach.getSpecialites() != null) {
                    for (String s : coach.getSpecialites()) {
                        Chip chip = new Chip(itemView.getContext());
                        chip.setText(s);
                        chip.setChipBackgroundColorResource(R.color.chip_background_color);
                        chip.setTextColor(getResources().getColor(R.color.chip_text_color));
                        groupeDeChips.addView(chip);
                    }
                }

                btnModifier.setOnClickListener(v -> afficherDialogModifierCoach(coach));

                btnSupprimer.setOnClickListener(v -> {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Supprimer Coach")
                            .setMessage("Voulez-vous vraiment supprimer " + coach.getNomComplet() + " ?")
                            .setPositiveButton("Oui", (dialog, which) -> {
                                String coachId = coach.getId();

                                if (coachId == null || coachId.trim().isEmpty()) {
                                    // 🔹 Supprimer uniquement localement
                                    daoCoach.supprimerCoachSansId(coach.getNomComplet());
                                    listeCompleteCoachs.remove(coach);
                                    listeAffichee.remove(coach);
                                    adaptateur.definirCoachs(listeAffichee);
                                    dialog.dismiss();
                                    Toast.makeText(getContext(), "Coach sans ID supprimé localement", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                new FirebaseHelper().supprimerCoach(coachId, success -> {
                                    if (success) {
                                        listeCompleteCoachs.removeIf(c -> coachId.equals(c.getId()));
                                        listeAffichee.removeIf(c -> coachId.equals(c.getId()));
                                        daoCoach.supprimerCoach(coachId);
                                        adaptateur.definirCoachs(listeAffichee);
                                        dialog.dismiss();
                                        Toast.makeText(getContext(), "Coach supprimé", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getContext(), "Erreur de suppression Firebase", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            })
                            .setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss())
                            .show();
                });

            }
        }
    }
}
