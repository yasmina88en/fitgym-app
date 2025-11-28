package com.example.fitgym.data.repository;

import android.content.Context;

import com.example.fitgym.data.dao.DAOClient;
import com.example.fitgym.data.dao.DAOSeance; // ajouter DAOSeance
import com.example.fitgym.data.model.Client;
import com.example.fitgym.data.model.Seance;

import java.util.List;

public class ClientRepository {

    private DAOClient daoClient;
    private DAOSeance daoSeance; // pour récupérer les séances

    public ClientRepository(Context context) {
        daoClient = new DAOClient(context);
        daoSeance = new DAOSeance(context); // initialisation
    }

    // ------------------------------
    //    AJOUTER CLIENT
    // ------------------------------
    public long ajouterClient(Client client) {
        return daoClient.ajouterClient(client);
    }

    // ------------------------------
    //    LOGIN : OBTENIR CLIENT PAR EMAIL
    // ------------------------------
    public Client obtenirClientParEmail(String email) {
        return daoClient.obtenirClientParEmail(email);
    }

    // ------------------------------
    //    OBTENIR CLIENT PAR ID
    // ------------------------------
    public Client obtenirClientParId(String id) {
        return daoClient.obtenirClientParId(id);
    }

    // ------------------------------
    //    LISTER TOUS LES CLIENTS
    // ------------------------------
    public List<Client> listerClients() {
        return daoClient.listerClients();
    }

    // ------------------------------
    //    MODIFIER CLIENT
    // ------------------------------
    public int modifierClient(Client client) {
        return daoClient.modifierClient(client);
    }

    // ------------------------------
    //    SUPPRIMER CLIENT
    // ------------------------------
    public int supprimerClient(String id) {
        return daoClient.supprimerClient(id);
    }

    // ------------------------------
    //    OBTENIR TOUTES LES SEANCES
    // ------------------------------
    public List<Seance> getAllSeances() {
        // Récupère toutes les séances depuis DAOSeance
        return daoSeance.listerSeances();
    }
}
