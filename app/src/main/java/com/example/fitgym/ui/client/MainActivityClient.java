package com.example.fitgym.ui.client;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.fitgym.R;
import com.example.fitgym.data.db.FirebaseHelper;
import com.example.fitgym.data.model.Client;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivityClient extends AppCompatActivity {

    private String clientId;
    private Client currentClient;
    private FirebaseHelper firebaseHelper;

    private DashboardFragmentClient dashboardFragment;
    private ProfileFragmentClient profileFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_client);

        firebaseHelper = new FirebaseHelper();

        clientId = getIntent().getStringExtra("client_id");
        if (clientId == null || clientId.isEmpty()) {
            Toast.makeText(this, "Erreur : aucun client trouvé.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Sauvegarder clientId dans SharedPreferences pour un accès global
        getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit()
                .putString("current_client_id", clientId)
                .apply();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationClient);

        dashboardFragment = new DashboardFragmentClient();
        profileFragment = new ProfileFragmentClient();

        // Écran par défaut
        loadFragment(dashboardFragment);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                loadFragment(dashboardFragment);
            } else if (id == R.id.nav_sessions) {
                ClientSeanceFragmentYas seancesFragment = new ClientSeanceFragmentYas();
                Bundle args = new Bundle();
                args.putString("client_id", clientId);
                seancesFragment.setArguments(args);
                loadFragment(seancesFragment);
            } else if (id == R.id.nav_coaches) {
                loadFragment(new ListeCoachesFragmentClient());
            } else if (id == R.id.nav_more) {
                openMoreMenu();
            }
            return true;
        });

        loadClientData(clientId);
    }

    private void openMoreMenu() {
        MoreBottomSheet sheet = new MoreBottomSheet();
        sheet.setListener(new MoreBottomSheet.MoreListener() {
            @Override
            public void onProfileSelected() {
                loadFragment(profileFragment);
            }

            @Override
            public void onFavorisSelected() {
                // Charger le fragment Favoris et lui transmettre l'ID client
                FavorisFragment favFrag = new FavorisFragment();
                Bundle args = new Bundle();
                args.putString("client_id", clientId);
                favFrag.setArguments(args);
                loadFragment(favFrag);
            }
        });
        sheet.show(getSupportFragmentManager(), "MoreMenu");
    }

    private void loadClientData(String clientId) {
        // Essayer d'abord avec Firebase (qui gère aussi le cache si configuré)
        firebaseHelper.getClientById(clientId, client -> {
            if (client == null) {
                // Si Firebase échoue (ex: hors ligne et pas de cache), essayer SQLite local
                com.example.fitgym.data.db.DatabaseHelper dbHelper = new com.example.fitgym.data.db.DatabaseHelper(
                        this);
                client = dbHelper.getClientById(clientId);

                if (client == null) {
                    // Vraiment introuvable nulle part
                    Toast.makeText(MainActivityClient.this, "Client introuvable (hors ligne).", Toast.LENGTH_SHORT)
                            .show();
                    finish();
                    return;
                } else {
                    // Trouvé localement
                    Toast.makeText(MainActivityClient.this, "Mode hors ligne activé ✅", Toast.LENGTH_SHORT).show();
                }
            }

            currentClient = client;

            if (dashboardFragment != null)
                dashboardFragment.updateClient(currentClient);

            if (profileFragment != null)
                profileFragment.updateClient(currentClient);
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container_client, fragment);
        transaction.commit();
    }

    public Client getCurrentClient() {
        return currentClient;
    }
}
