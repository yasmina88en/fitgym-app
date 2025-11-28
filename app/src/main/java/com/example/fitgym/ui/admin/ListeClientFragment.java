package com.example.fitgym.ui.admin;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitgym.R;
import com.example.fitgym.data.db.DatabaseHelper;
import com.example.fitgym.data.db.FirebaseHelper;
import com.example.fitgym.data.model.Client;
import com.example.fitgym.ui.adapter.UsersAdapter;

import java.util.ArrayList;
import java.util.List;

public class ListeClientFragment extends Fragment {

    private RecyclerView recyclerView;
    private UsersAdapter usersAdapter;
    private DatabaseHelper dbHelper;
    private EditText searchBar;
    private List<Client> allClients = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_liste_users, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewSeances);
        searchBar = view.findViewById(R.id.searchBar);

        dbHelper = new DatabaseHelper(getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        usersAdapter = new UsersAdapter(getContext(), new ArrayList<>());
        recyclerView.setAdapter(usersAdapter);

        loadClients();

        // Recherche par nom
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterClients(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void loadClients() {
        if (isConnected()) {
            // Récupérer les clients depuis Firebase
            FirebaseHelper firebaseHelper = new FirebaseHelper();
            firebaseHelper.getAllClientsFromFirebase(firebaseClients -> {
                // Synchroniser avec SQLite
                DatabaseHelper dbHelper = new DatabaseHelper(getContext());
                for (Client client : firebaseClients) {
                    dbHelper.syncClient(client);
                }

                // Mettre à jour la liste de la classe pour affichage
                allClients.clear();
                allClients.addAll(dbHelper.getAllClients());
                usersAdapter.updateList(allClients);
            });
        } else {
            // Offline : récupérer depuis SQLite
            DatabaseHelper dbHelper = new DatabaseHelper(getContext());
            allClients.clear();
            allClients.addAll(dbHelper.getAllClients());
            usersAdapter.updateList(allClients);
        }
    }


    private void filterClients(String query) {
        List<Client> filtered = new ArrayList<>();
        for (Client c : allClients) {
            if (c.getNom().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(c);
            }
        }
        usersAdapter.updateList(filtered);
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }
}
