package com.example.fitgym.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.fitgym.data.model.Client;
import com.example.fitgym.data.repository.ClientRepository;

public class ClientViewModel extends AndroidViewModel {

    private ClientRepository clientRepository;

    public ClientViewModel(@NonNull Application application) {
        super(application);
        clientRepository = new ClientRepository(application);
    }

    // LOGIN
    public Client login(String email, String password) {
        Client client = clientRepository.obtenirClientParEmail(email);
        if (client != null && client.getMotDePasse().equals(password)) {
            return client;
        }
        return null;
    }

    // INSCRIPTION
    public long inscrire(Client client) {
        return clientRepository.ajouterClient(client);
    }

    // PROFIL PAR ID
    public Client obtenirProfil(int id) {
        // Convertir l'int en String
        return clientRepository.obtenirClientParId(String.valueOf(id));
    }


    // MODIFIER PROFIL
    public int modifierProfil(Client client) {
        return clientRepository.modifierClient(client);
    }


}