package com.example.fitgym.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.fitgym.data.db.FirebaseHelper;
import com.example.fitgym.data.model.Coach;

import java.util.ArrayList;
import java.util.List;

public class CoachViewModel extends AndroidViewModel {

    private MutableLiveData<List<Coach>> coachsLiveData = new MutableLiveData<>(new ArrayList<>());
    private FirebaseHelper firebaseHelper;

    public CoachViewModel(@NonNull Application application) {
        super(application);
        firebaseHelper = new FirebaseHelper();
        chargerCoachs();
    }

    public LiveData<List<Coach>> getListeCoachs() {
        return coachsLiveData;
    }

    private void chargerCoachs() {
        firebaseHelper.getAllCoaches(fireBaseCoachs -> {
            coachsLiveData.postValue(fireBaseCoachs != null ? fireBaseCoachs : new ArrayList<>());
        });
    }

    public void ajouterCoach(Coach coach) {
        List<Coach> current = coachsLiveData.getValue();
        if (current == null) current = new ArrayList<>();
        current.add(0, coach); // add en tête
        coachsLiveData.setValue(current);

        List<Coach> finalCurrent = current;
        firebaseHelper.ajouterCoach(coach, success -> {
            if (!success) {
                finalCurrent.remove(coach);
                coachsLiveData.postValue(finalCurrent);
            }
        });
    }

    public void supprimerCoach(String coachId) {
        List<Coach> current = coachsLiveData.getValue();
        if (current != null) {
            for (int i = 0; i < current.size(); i++) {
                if (current.get(i).getId() != null && current.get(i).getId().equals(coachId)) {
                    current.remove(i);
                    break;
                }
            }
            coachsLiveData.setValue(current);
        }

        firebaseHelper.supprimerCoach(coachId, success -> {});
    }

    public void modifierCoach(Coach coach) {
        List<Coach> current = coachsLiveData.getValue();
        if (current != null) {
            for (int i = 0; i < current.size(); i++) {
                if (current.get(i).getId() != null && current.get(i).getId().equals(coach.getId())) {
                    current.set(i, coach);
                    break;
                }
            }
            coachsLiveData.setValue(current);
        }
        firebaseHelper.modifierCoach(coach, success -> {});
    }
}
