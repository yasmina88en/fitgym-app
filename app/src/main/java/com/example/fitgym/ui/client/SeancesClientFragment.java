package com.example.fitgym.ui.client;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitgym.R;
import com.example.fitgym.data.repository.DataRepository;
import com.example.fitgym.data.model.Seance;
import com.example.fitgym.ui.adapter.SeanceClientAdapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class SeancesClientFragment extends Fragment {

    private RecyclerView rv;
    private SeanceClientAdapter adapter;
    private DataRepository dataRepository;

    private EditText etSearch;
    private LinearLayout layoutCategoryList, layoutNiveauList;
    private TextView tvCategoryTitle, tvNiveauTitle;

    private final String[] categories = {
            "Zumba", "Crossfit", "Cardio", "Musculation", "Pilates", "Yoga", "Stretching", "Boxe"
    };

    private final String[] niveaux = { "Débutant", "Intermédiaire", "Avancé" };

    private List<Seance> allSeances = new ArrayList<>();
    private String currentCategoryFilter = "";
    private String currentNiveauFilter = "";
    private String currentQuery = "";

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard_client, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        dataRepository = DataRepository.getInstance(requireContext());

        rv = view.findViewById(R.id.rvSessions);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new SeanceClientAdapter(requireContext(), new SeanceClientAdapter.OnItemAction() {
            @Override
            public void onItemClicked(Seance s) {
                Toast.makeText(requireContext(), "Séance: " + s.getTitre(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFavoriteClicked(Seance s) {
                Toast.makeText(requireContext(),
                        "Favori cliqué: " + s.getTitre(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        rv.setAdapter(adapter);

        // Initialize UI elements
        etSearch = view.findViewById(R.id.etSearch);
        tvCategoryTitle = view.findViewById(R.id.tvCategoryTitle);
        tvNiveauTitle = view.findViewById(R.id.tvNiveauTitle);
        layoutCategoryList = view.findViewById(R.id.layoutCategoryList);
        layoutNiveauList = view.findViewById(R.id.layoutNiveauList);

        setupFiltersUI();
        setupSearch();
        loadSeancesFromFirebase();
    }

    private void loadSeancesFromFirebase() {
        // Utilise le DataRepository qui charge d'abord depuis SQLite puis synchronise
        // avec Firebase
        dataRepository.getSeances(new DataRepository.DataCallback<List<Seance>>() {
            @Override
            public void onSuccess(List<Seance> seances) {
                if (seances == null)
                    seances = new ArrayList<>();

                List<Seance> future = new ArrayList<>();
                Date today = new Date();

                for (Seance s : seances) {
                    try {
                        if (s.getDate() == null)
                            continue;
                        Date d = sdf.parse(s.getDate());
                        if (!d.before(today))
                            future.add(s);
                    } catch (ParseException ignored) {
                    }
                }

                Collections.sort(future, (a, b) -> {
                    try {
                        Date da = a.getDate() != null ? sdf.parse(a.getDate()) : null;
                        Date db = b.getDate() != null ? sdf.parse(b.getDate()) : null;
                        if (da != null && db != null) {
                            int cmp = da.compareTo(db);
                            if (cmp != 0)
                                return cmp;
                        }
                    } catch (ParseException ignored) {
                    }

                    String ha = a.getHeure() != null ? a.getHeure() : "";
                    String hb = b.getHeure() != null ? b.getHeure() : "";
                    return ha.compareTo(hb);
                });

                allSeances = future;
                applyFiltersAndShow();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFiltersAndShow() {
        List<Seance> filtered = new ArrayList<>(allSeances);

        if (!currentCategoryFilter.isEmpty()) {
            String cf = currentCategoryFilter.toLowerCase();
            filtered.removeIf(s -> s.getCategorieId() == null || !s.getCategorieId().toLowerCase().contains(cf));
        }

        if (!currentNiveauFilter.isEmpty()) {
            String nf = currentNiveauFilter.toLowerCase();
            filtered.removeIf(s -> s.getNiveau() == null || !s.getNiveau().toLowerCase().contains(nf));
        }

        if (!currentQuery.isEmpty()) {
            String q = currentQuery.toLowerCase();
            filtered.removeIf(s -> (s.getTitre() == null || !s.getTitre().toLowerCase().contains(q)) &&
                    (s.getCoachId() == null || !s.getCoachId().toLowerCase().contains(q)));
        }

        int limit = Math.min(7, filtered.size());
        adapter.setItems(filtered.subList(0, limit));
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString();
                applyFiltersAndShow();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupFiltersUI() {

        layoutCategoryList.removeAllViews();
        for (String c : categories) {
            TextView t = (TextView) getLayoutInflater().inflate(R.layout.simple_filter_item, null);
            t.setText(c);
            t.setOnClickListener(v -> {
                currentCategoryFilter = c;
                tvCategoryTitle.setText(c);
                layoutCategoryList.setVisibility(View.GONE);
                applyFiltersAndShow();
            });
            layoutCategoryList.addView(t);
        }

        layoutNiveauList.removeAllViews();
        for (String n : niveaux) {
            TextView t = (TextView) getLayoutInflater().inflate(R.layout.simple_filter_item, null);
            t.setText(n);
            t.setOnClickListener(v -> {
                currentNiveauFilter = n;
                tvNiveauTitle.setText(n);
                layoutNiveauList.setVisibility(View.GONE);
                applyFiltersAndShow();
            });
            layoutNiveauList.addView(t);
        }

        View catCard = getView().findViewById(R.id.cardCategoryFilter);
        View nivCard = getView().findViewById(R.id.cardNiveauFilter);

        catCard.setOnClickListener(v -> {
            layoutCategoryList
                    .setVisibility(layoutCategoryList.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
            layoutNiveauList.setVisibility(View.GONE);
        });

        nivCard.setOnClickListener(v -> {
            layoutNiveauList.setVisibility(layoutNiveauList.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
            layoutCategoryList.setVisibility(View.GONE);
        });
    }
}
