package com.example.fitgym.ui.admin;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.fitgym.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivityAdmin extends AppCompatActivity {

    private ProfileFragmentAdmin profileFragment;
    private ListeClientFragment ClientFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_admin);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        profileFragment = new ProfileFragmentAdmin();
        ClientFragment = new ListeClientFragment();
        // Fragment par défaut → Dashboard
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new DashboardFragmentAdmin())
                .commit();

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                selectedFragment = new DashboardFragmentAdmin();
            } else if (id == R.id.nav_sessions) {
                selectedFragment = new ListeSeancesFragment();
            } else if (id == R.id.nav_coaches) {
                selectedFragment = new ListeCoachsFragment();
            } else if (id == R.id.nav_reservations) {
                // selectedFragment = new ReservationsFragment();
            } else if (id == R.id.nav_more) {
                openMoreMenu(); // ouvre le BottomSheet
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }

            return true;
        });
    }

    private void openMoreMenu() {
        MoreBottomSheetAdmin sheet = new MoreBottomSheetAdmin();
        sheet.setListener(new MoreBottomSheetAdmin.MoreListener() {
            @Override
            public void onProfileSelected() {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, profileFragment)
                        .commit();
            }

            @Override
            public void onUtilisateurSelected() {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container,ClientFragment)
                        .commit();  }
        });

        sheet.show(getSupportFragmentManager(), "MoreMenuAdmin");
    }

    // Méthodes onClick XML
    public void onManageCoachsClick(View view) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new ListeCoachsFragment())
                .addToBackStack(null)
                .commit();
    }

    public void onManageSessionsClick(View view) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new ListeSeancesFragment())
                .addToBackStack(null)
                .commit();
    }
}
