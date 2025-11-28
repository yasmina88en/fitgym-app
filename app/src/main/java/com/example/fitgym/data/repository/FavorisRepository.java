package com.example.fitgym.data.repository;

import android.content.Context;
import androidx.lifecycle.MutableLiveData;

import com.example.fitgym.data.db.DatabaseHelper;
import com.example.fitgym.data.db.FirebaseHelper;
import com.example.fitgym.data.model.Favoris;
import com.example.fitgym.data.model.Seance;

import java.util.ArrayList;
import java.util.List;

/**
 * Fournit la liste des séances favorites pour un client.
 * Utilise DB local (favoris table + seances) + Firebase en fallback.
 */
public class FavorisRepository {

    private final DatabaseHelper db;
    private final FirebaseHelper firebase;

    public FavorisRepository(Context context) {
        this.db = new DatabaseHelper(context);
        this.firebase = new FirebaseHelper();
    }

    /**
     * Retourne LiveData (MutableLiveData) contenant la liste des Seance favoris pour clientId.
     * Logic:
     *  - lire les seanceId depuis la table favoris locale
     *  - pour chaque seanceId -> getSeanceById local, si absent -> fetch firebase (getSeanceById) et insert local
     */
    public MutableLiveData<List<Seance>> getFavorisForClient(String clientId) {
        MutableLiveData<List<Seance>> data = new MutableLiveData<>(new ArrayList<>());
        // 1) obtenir les seanceId favoris locaux
        List<String> seanceIds = db.getFavorisParClient(clientId);
        List<Seance> result = new ArrayList<>();
        if (seanceIds == null || seanceIds.isEmpty()) {
            data.postValue(result);
            return data;
        }

        // Essayons de charger depuis la DB locale d'abord
        for (String sid : seanceIds) {
            Seance s = db.getSeanceById(sid);
            if (s != null) {
                result.add(s);
            } else {
                // si absent local -> fetch firebase et insérer local
                final String _sid = sid;
                firebase.getSeanceById(sid, seance -> {
                    if (seance != null) {
                        // insert local pour cohérence
                        db.insertOrUpdateSeance(seance);
                        // re-lire la liste actuelle et poster
                        List<Seance> current = data.getValue();
                        if (current == null) current = new ArrayList<>();
                        current.add(seance);
                        data.postValue(current);
                    } else {
                        // si pas trouvé, on ignore
                    }
                });
            }
        }

        // poster les résultats locaux initiaux
        data.postValue(result);
        return data;
    }

    /**
     * Ajoute un favori (remote + local)
     * callback boolean via Runnable style: (success -> true/false) => here we return boolean synchronously false/true via listener
     */
    public void ajouterFavori(String clientId, Seance seance, FavoriCallback callback) {
        if (clientId == null || clientId.isEmpty() || seance == null || seance.getId() == null) {
            callback.onComplete(false);
            return;
        }

        // 1) add local (optimiste)
        boolean localOk = db.ajouterFavoriLocal(clientId, seance.getId());
        // ensure seance present locally
        db.insertOrUpdateSeance(seance);

        // 2) push to firebase
        Favoris f = new Favoris();
        f.setClientId(clientId);
        f.setSeanceId(seance.getId());
        // id will be generated in firebaseHelper if null
        firebase.ajouterFavori(f, success -> {
            if (success) {
                // nothing more (local already added). optionally sync id mapping but not required
                callback.onComplete(true);
            } else {
                // rollback local if desired (we keep local optimistic). Here we remove local favori if push fails
                db.supprimerFavoriLocal(clientId, seance.getId());
                callback.onComplete(false);
            }
        });
    }

    public interface FavoriCallback {
        void onComplete(boolean success);
    }
    // FavorisRepository.java (ajoute ces méthodes à la classe existante)
    public void supprimerFavori(String clientId, String seanceId, FavoriRemoveCallback cb) {
        if (clientId == null || seanceId == null) {
            cb.onComplete(false);
            return;
        }

        // 1) supprimer localement (optimiste)
        boolean localOk = db.supprimerFavoriLocal(clientId, seanceId);

        // 2) supprimer sur Firebase : recherche puis suppression
        firebase.supprimerFavoriParClientSeance(clientId, seanceId, success -> {
            if (success) {
                cb.onComplete(true);
            } else {
                // tentative rollback : si on a supprimé localement mais remote KO, on peut réinsérer local,
                // mais ici on retourne false et laisse l'état local (ou ré-insert selon choix)
                cb.onComplete(false);
            }
        });
    }

    public interface FavoriRemoveCallback {
        void onComplete(boolean success);
    }

}
