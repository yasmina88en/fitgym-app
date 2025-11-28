package com.example.fitgym.ui.viewmodel;


import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.fitgym.data.model.Seance;
import com.example.fitgym.data.repository.FavorisRepository;

import java.util.List;

public class FavorisViewModel extends AndroidViewModel {

    private final FavorisRepository repository;
    private MutableLiveData<List<Seance>> favorisLiveData;

    public FavorisViewModel(@NonNull Application application) {
        super(application);
        repository = new FavorisRepository(application);
        favorisLiveData = new MutableLiveData<>();
    }

    public LiveData<List<Seance>> getFavoris(String clientId) {
        favorisLiveData = repository.getFavorisForClient(clientId);
        return favorisLiveData;
    }

    public void ajouterFavori(String clientId, Seance seance, FavorisRepository.FavoriCallback cb) {
        repository.ajouterFavori(clientId, seance, cb);
    }

    // suppression
    public void supprimerFavori(String clientId, String seanceId, FavorisRepository.FavoriRemoveCallback cb) {
        repository.supprimerFavori(clientId, seanceId, cb);
    }
}
