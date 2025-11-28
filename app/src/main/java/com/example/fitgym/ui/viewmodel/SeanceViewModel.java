package com.example.fitgym.ui.viewmodel;


import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.fitgym.data.db.FirebaseHelper;
import com.example.fitgym.data.dao.DAOCoach;
import com.example.fitgym.data.db.DatabaseHelper;
import com.example.fitgym.data.model.Categorie;
import com.example.fitgym.data.model.Coach;
import com.example.fitgym.data.model.Seance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SeanceViewModel extends AndroidViewModel {

    private MutableLiveData<List<Seance>> seancesLiveData = new MutableLiveData<>(new ArrayList<>());
    private FirebaseHelper firebaseHelper;
    private DatabaseHelper databaseHelper;

    public SeanceViewModel(@NonNull Application application) {
        super(application);
        firebaseHelper = new FirebaseHelper();
        databaseHelper = new DatabaseHelper(application);
        chargerSeances();
    }

    public LiveData<List<Seance>> getListeSeances() {
        return seancesLiveData;
    }

    private void chargerSeances() {
        Log.d("SeanceViewModel", "chargerSeances() start");

        // 1) Afficher immédiatement les séances locales (offline-first)
        List<Seance> locaux = databaseHelper.getAllSeances();
        Log.d("SeanceViewModel", "Local seances count = " + (locaux == null ? 0 : locaux.size()));
        seancesLiveData.postValue(locaux != null ? locaux : new ArrayList<>());

        // 2) Tenter la synchronisation depuis Firebase
        firebaseHelper.getAllSeances(firebaseSeances -> {
            Log.d("SeanceViewModel", "Firebase callback called");
            List<Seance> remote = firebaseSeances != null ? firebaseSeances : new ArrayList<>();
            Log.d("SeanceViewModel", "Firebase renvoyé " + remote.size() + " séances");

            // Si Firebase renvoie AU MOINS 1 séance -> on remplace le local (sync)
            // Si c'est vide, on NE SUPPRIME PAS le local (on conserve l'affichage hors-ligne).
            if (remote.size() > 0) {
                // Mettre à jour l'UI avec les données distantes (optimiste)
                seancesLiveData.postValue(remote);

                // Vider localement puis insérer une par une (les méthodes internes gèrent open/close)
                databaseHelper.deleteAllSeances();
                for (Seance s : remote) {
                    boolean inserted = databaseHelper.insertOrUpdateSeance(s);
                    if (!inserted) {
                        Log.e("SeanceViewModel", "Échec insertion Seance: " + (s != null ? s.getTitre() : "null"));
                    }
                }

                // --- En plus : récupérer et stocker coaches & categories référencés ---
                // Collecter IDs uniques
                Set<String> coachIds = new HashSet<>();
                Set<String> categorieIds = new HashSet<>();
                for (Seance s : remote) {
                    if (s.getCoachId() != null && !s.getCoachId().isEmpty()) coachIds.add(s.getCoachId());
                    if (s.getCategorieId() != null && !s.getCategorieId().isEmpty()) categorieIds.add(s.getCategorieId());
                }

                // DAOCoach (utilise la même DB) pour insérer les coaches récupérés
                DAOCoach daoCoach = new DAOCoach(databaseHelper.getWritableDatabase());

                // Parcourir coaches : si absent localement -> fetch Firebase -> insert local
                for (String cid : coachIds) {
                    Coach localCoach = databaseHelper.getCoachById(cid);
                    if (localCoach == null) {
                        firebaseHelper.getCoachById(cid, fetchedCoach -> {
                            if (fetchedCoach != null) {
                                // inserir localement via DAOCoach
                                try {
                                    daoCoach.ajouterCoach(fetchedCoach);
                                } catch (Exception e) {
                                    Log.e("SeanceViewModel", "Erreur insertion coach local: " + e.getMessage());
                                }
                            }
                        });
                    }
                }

                // Parcourir categories : si absent localement -> fetch Firebase -> insert local
                for (String catId : categorieIds) {
                    Categorie localCat = databaseHelper.getCategorieById(catId);
                    if (localCat == null) {
                        firebaseHelper.getCategorieById(catId, fetchedCat -> {
                            if (fetchedCat != null) {
                                databaseHelper.insertOrUpdateCategorie(fetchedCat);
                            }
                        });
                    }
                }

                // Re-lire et poster la version finale depuis SQLite (assure cohérence)
                List<Seance> updatedLocal = databaseHelper.getAllSeances();
                Log.d("SeanceViewModel", "Après insert, seances en local = " + (updatedLocal == null ? 0 : updatedLocal.size()));
                seancesLiveData.postValue(updatedLocal != null ? updatedLocal : new ArrayList<>());
            } else {
                // Remote vide -> on ne touche PAS à la DB locale, on garde ce qu'il y a.
                Log.d("SeanceViewModel", "Firebase vide ou inaccessible — conservation du local.");
                // Optionnel : si tu veux notifier l'UI d'un état "pas de connexion", ajoute un LiveData d'état.
            }
        });
    }

    public void ajouterSeance(Seance seance) {
        List<Seance> current = seancesLiveData.getValue();
        if (current == null) current = new ArrayList<>();
        current.add(0, seance); // affichage optimiste
        seancesLiveData.setValue(current);

        List<Seance> finalCurrent = current;
        firebaseHelper.ajouterSeance(seance, success -> {
            if (success) {
                databaseHelper.insertOrUpdateSeance(seance);
            } else {
                finalCurrent.remove(seance);
                seancesLiveData.postValue(finalCurrent);
            }
        });
    }

    public void modifierSeance(Seance seance) {
        List<Seance> current = seancesLiveData.getValue();
        if (current != null) {
            for (int i = 0; i < current.size(); i++) {
                if (current.get(i).getId() != null && current.get(i).getId().equals(seance.getId())) {
                    current.set(i, seance);
                    break;
                }
            }
            seancesLiveData.setValue(current);
        }
        firebaseHelper.modifierSeance(seance, success -> {
            if (success) databaseHelper.updateSeance(seance);
        });
    }

    public void supprimerSeance(Seance seance) {
        List<Seance> current = seancesLiveData.getValue();
        if (current != null) {
            current.removeIf(s -> s.getId() != null && s.getId().equals(seance.getId()));
            seancesLiveData.setValue(current);
        }
        firebaseHelper.supprimerSeance(seance.getId(), success -> {
            if (success) databaseHelper.deleteSeance(seance.getId());
        });
    }
}

