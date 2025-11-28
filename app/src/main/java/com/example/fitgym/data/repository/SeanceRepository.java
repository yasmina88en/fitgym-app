package com.example.fitgym.data.repository;

import android.content.Context;
import com.example.fitgym.data.db.FirebaseHelper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.fitgym.data.dao.DAOSeance;
import com.example.fitgym.data.model.Seance;

import java.util.List;

public class SeanceRepository {

    private DAOSeance dao;
    private FirebaseHelper firebaseHelper;

    public SeanceRepository(Context context) {
        dao = new DAOSeance(context);
        firebaseHelper = new FirebaseHelper();
    }

    public LiveData<List<Seance>> getSeances() {
        MutableLiveData<List<Seance>> data = new MutableLiveData<>();
        // 1) Local
        data.postValue(dao.listerSeances());
        // 2) Remote
        firebaseHelper.getAllSeances(seances -> {
            data.postValue(seances);
            dao.viderSeances();
            for (Seance s : seances) {
                dao.ajouterSeance(s);
            }
        });
        return data;
    }

    public void ajouterSeance(Seance s) {
        firebaseHelper.ajouterSeance(s, success -> {
            if (success) dao.ajouterSeance(s);
        });
    }

    public void modifierSeance(Seance s) {
        firebaseHelper.modifierSeance(s, success -> {
            if (success) dao.modifierSeance(s);
        });
    }

    public void supprimerSeance(Seance s) {
        firebaseHelper.supprimerSeance(s.getId(), success -> {
            if (success) dao.supprimerSeance(s.getId());
        });
    }
}
