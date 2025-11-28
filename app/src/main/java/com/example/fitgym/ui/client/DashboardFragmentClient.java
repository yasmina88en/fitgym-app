package com.example.fitgym.ui.client;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitgym.R;
import com.example.fitgym.data.db.FirebaseHelper;
import com.example.fitgym.data.model.Categorie;
import com.example.fitgym.data.model.Client;
import com.example.fitgym.data.model.Coach;
import com.example.fitgym.data.model.Seance;
import com.example.fitgym.ui.adapter.SeanceClientAdapter;
import com.google.android.material.card.MaterialCardView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class DashboardFragmentClient extends Fragment {

    private View rootView;

    private LinearLayout layoutCategoryList, layoutNiveauList;
    private ImageView ivCategoryArrow, ivNiveauArrow;
    private MaterialCardView cardCategoryFilter, cardNiveauFilter;
    private EditText etSearch;

    private RecyclerView recyclerSeances;
    private SeanceClientAdapter seanceAdapter;

    private ImageView ivSearchIcon;
    private FirebaseHelper firebaseHelper;
    private Client currentClient;

    // Filter names
    private final String[] categories = {"Toutes", "Zumba", "Crossfit", "Cardio", "Musculation",
            "Pilates", "Yoga", "Stretching", "Boxe"};
    private final String[] niveaux = {"Tous", "Débutant", "Intermédiaire", "Avancé"};

    private List<Seance> allSeances = new ArrayList<>();
    private List<Seance> filteredSeances = new ArrayList<>();

    private HashMap<String, Coach> coachMap = new HashMap<>();
    private HashMap<String, Categorie> categorieMap = new HashMap<>();

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_dashboard_client, container, false);
        firebaseHelper = new FirebaseHelper();

        initViews();
        setupCategoryOptions();
        setupNiveauOptions();
        setupListeners();
        setupRecycler();
        preloadCoachesAndCategories();

        return rootView;
    }

    private void initViews() {
        ivSearchIcon = rootView.findViewById(R.id.ivSearchIcon);
        layoutCategoryList = rootView.findViewById(R.id.layoutCategoryList);
        layoutNiveauList = rootView.findViewById(R.id.layoutNiveauList);
        ivCategoryArrow = rootView.findViewById(R.id.ivCategoryArrow);
        ivNiveauArrow = rootView.findViewById(R.id.ivNiveauArrow);
        cardCategoryFilter = rootView.findViewById(R.id.cardCategoryFilter);
        cardNiveauFilter = rootView.findViewById(R.id.cardNiveauFilter);
        etSearch = rootView.findViewById(R.id.etSearch);
        recyclerSeances = rootView.findViewById(R.id.rvSessions);
    }

    private void setupRecycler() {
        recyclerSeances.setLayoutManager(new LinearLayoutManager(getContext()));
        seanceAdapter = new SeanceClientAdapter(getContext(), new SeanceClientAdapter.OnItemAction() {
            @Override
            public void onItemClicked(Seance s) {}
            @Override
            public void onFavoriteClicked(Seance s) {}
        });
        recyclerSeances.setAdapter(seanceAdapter);
    }

    private void setupCategoryOptions() {
        layoutCategoryList.removeAllViews();
        for (String cat : categories) {
            TextView tv = createFilterItem(cat);
            tv.setOnClickListener(v -> {
                clearSelection(layoutCategoryList);
                tv.setTag("selected");
                tv.setBackgroundColor(Color.parseColor("#E5E7EB"));
                layoutCategoryList.setVisibility(View.GONE);
                ivCategoryArrow.setRotation(0);
                applyFilters();
            });
            layoutCategoryList.addView(tv);
        }
    }

    private void setupNiveauOptions() {
        layoutNiveauList.removeAllViews();
        for (String niv : niveaux) {
            TextView tv = createFilterItem(niv);
            tv.setOnClickListener(v -> {
                clearSelection(layoutNiveauList);
                tv.setTag("selected");
                tv.setBackgroundColor(Color.parseColor("#E5E7EB"));
                layoutNiveauList.setVisibility(View.GONE);
                ivNiveauArrow.setRotation(0);
                applyFilters();
            });
            layoutNiveauList.addView(tv);
        }
    }

    private TextView createFilterItem(String text) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setPadding(20, 20, 20, 20);
        tv.setTextSize(14);
        tv.setTextColor(Color.parseColor("#111827"));
        return tv;
    }

    private void clearSelection(LinearLayout layout) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            layout.getChildAt(i).setTag(null);
            layout.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
        }
    }
    private void setupListeners() {
        cardCategoryFilter.setOnClickListener(v -> {
            boolean open = layoutCategoryList.getVisibility() == View.GONE;
            layoutCategoryList.setVisibility(open ? View.VISIBLE : View.GONE);
            ivCategoryArrow.setRotation(open ? 180 : 0);
            layoutNiveauList.setVisibility(View.GONE);
            ivNiveauArrow.setRotation(0);

            // Vider la recherche quand un filtre est ouvert
            etSearch.setText("");
        });

        cardNiveauFilter.setOnClickListener(v -> {
            boolean open = layoutNiveauList.getVisibility() == View.GONE;
            layoutNiveauList.setVisibility(open ? View.VISIBLE : View.GONE);
            ivNiveauArrow.setRotation(open ? 180 : 0);
            layoutCategoryList.setVisibility(View.GONE);
            ivCategoryArrow.setRotation(0);

            // Vider la recherche quand un filtre est ouvert
            etSearch.setText("");
        });

        ivSearchIcon.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            applyFilters();
            Toast.makeText(getContext(), "Recherche : " + query, Toast.LENGTH_SHORT).show();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
        });
    }



    private void preloadCoachesAndCategories() {
        // Charger tous les coachs
        firebaseHelper.getAllCoaches(coachList -> {
            coachMap.clear();
            for (Coach c : coachList) coachMap.put(c.getId(), c);
            preloadCategories();
        });
    }

    private void preloadCategories() {
        firebaseHelper.getAllCategories(cats -> {
            categorieMap.clear();
            for (Categorie c : cats) categorieMap.put(c.getCategorieId(), c);
            loadSeancesFromFirebase();
        });
    }

    private void loadSeancesFromFirebase() {
        firebaseHelper.getAllSeances(seances -> {
            allSeances = seances != null ? seances : new ArrayList<>();
            applyFiltersDefault();
        });
    }

    private void applyFiltersDefault() {
        filteredSeances.clear();
        Date now = new Date();
        for (Seance s : allSeances) {
            if (isFutureOrToday(s)) filteredSeances.add(s);
        }
        sortSeancesByDate(filteredSeances);
        if (filteredSeances.size() > 7)
            filteredSeances = filteredSeances.subList(0, 7);
        seanceAdapter.setItems(filteredSeances);
    }

    private boolean isFutureOrToday(Seance s) {
        try {
            if (s.getDate() == null) return false;
            Date d = sdf.parse(s.getDate() + " " + s.getHeure());
            return !d.before(new Date());
        } catch (ParseException e) {
            return false;
        }
    }

    private void sortSeancesByDate(List<Seance> list) {
        Collections.sort(list, (s1, s2) -> {
            try {
                Date d1 = sdf.parse(s1.getDate() + " " + s1.getHeure());
                Date d2 = sdf.parse(s2.getDate() + " " + s2.getHeure());
                return d1.compareTo(d2);
            } catch (Exception e) { return 0; }
        });
    }

    private void applyFilters() {
        String selectedCat = getSelected(layoutCategoryList, "Toutes");
        String selectedNiv = getSelected(layoutNiveauList, "Tous");
        String search = etSearch.getText().toString().trim().toLowerCase();

        filteredSeances.clear();
        for (Seance s : allSeances) {
            if (!isFutureOrToday(s)) continue;

            // Filtre catégorie
            if (!selectedCat.equals("Toutes")) {
                Categorie c = categorieMap.get(s.getCategorieId());
                if (c == null || !c.getNom().toLowerCase().contains(selectedCat.toLowerCase()))
                    continue;
            }

            // Filtre niveau
            if (!selectedNiv.equals("Tous") && !s.getNiveau().equalsIgnoreCase(selectedNiv))
                continue;

            // Filtre recherche
            boolean matchSearch = false;
            if (!search.isEmpty()) {
                if (s.getTitre() != null && s.getTitre().toLowerCase().contains(search)) matchSearch = true;
                Coach coach = coachMap.get(s.getCoachId());
                if (coach != null && coach.getNomComplet() != null && coach.getNomComplet().toLowerCase().contains(search))
                    matchSearch = true;
                if (!matchSearch) continue;
            }

            filteredSeances.add(s);
        }

        sortSeancesByDate(filteredSeances);
        seanceAdapter.setItems(filteredSeances);
    }

    private String getSelected(LinearLayout layout, String defaultValue) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            if ("selected".equals(layout.getChildAt(i).getTag())) {
                return ((TextView) layout.getChildAt(i)).getText().toString();
            }
        }
        return defaultValue;
    }

    // --- Méthode pour mise à jour client depuis MainActivity ---
    public void updateClient(Client client) {
        this.currentClient = client;
    }
}
