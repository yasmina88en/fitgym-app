package com.example.fitgym.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.fitgym.data.db.DatabaseHelper;
import com.example.fitgym.data.model.Client;

import java.util.ArrayList;
import java.util.List;

public class DAOClient {

    private SQLiteDatabase db;
    private DatabaseHelper dbHelper;

    public DAOClient(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    private void open() {
        db = dbHelper.getWritableDatabase();
    }

    private void close() {
        if (db != null && db.isOpen()) {
            db.close();
        }
    }

    // ------------------------------
    //    AJOUTER CLIENT
    // ------------------------------
    public long ajouterClient(Client client) {
        open();
        ContentValues values = new ContentValues();
        values.put("id", client.getId());
        values.put("nom", client.getNom());
        values.put("email", client.getEmail());
        values.put("motDePasse", client.getMotDePasse());
        values.put("telephone", client.getTelephone());
        values.put("synced", client.isSynced() ? 1 : 0);
        values.put("googleSignIn", client.isGoogleSignIn() ? 1 : 0);

        long rowId = db.insert("Client", null, values);
        close();
        return rowId;
    }

    // ------------------------------
    //    OBTENIR CLIENT PAR EMAIL
    // ------------------------------
    public Client obtenirClientParEmail(String email) {
        open();
        Client client = null;

        Cursor cursor = db.rawQuery("SELECT * FROM Client WHERE email = ?", new String[]{email});

        if (cursor.moveToFirst()) {
            client = extractClientFromCursor(cursor);
        }

        cursor.close();
        close();
        return client;
    }

    // ------------------------------
    //    OBTENIR CLIENT PAR ID
    // ------------------------------
    public Client obtenirClientParId(String id) {
        open();
        Client client = null;

        Cursor cursor = db.rawQuery("SELECT * FROM Client WHERE id = ?", new String[]{id});

        if (cursor.moveToFirst()) {
            client = extractClientFromCursor(cursor);
        }

        cursor.close();
        close();
        return client;
    }

    // ------------------------------
    //    LISTER TOUS LES CLIENTS
    // ------------------------------
    public List<Client> listerClients() {
        open();
        List<Client> liste = new ArrayList<>();

        Cursor cursor = db.rawQuery("SELECT * FROM Client", null);

        if (cursor.moveToFirst()) {
            do {
                Client client = extractClientFromCursor(cursor);
                liste.add(client);
            } while (cursor.moveToNext());
        }

        cursor.close();
        close();
        return liste;
    }

    // ------------------------------
    //    MODIFIER CLIENT
    // ------------------------------
    public int modifierClient(Client client) {
        open();
        ContentValues values = new ContentValues();
        values.put("nom", client.getNom());
        values.put("email", client.getEmail());
        values.put("motDePasse", client.getMotDePasse());
        values.put("telephone", client.getTelephone());
        values.put("synced", client.isSynced() ? 1 : 0);
        values.put("googleSignIn", client.isGoogleSignIn() ? 1 : 0);

        int result = db.update("Client", values, "id=?", new String[]{client.getId()});
        close();
        return result;
    }

    // ------------------------------
    //    SUPPRIMER CLIENT
    // ------------------------------
    public int supprimerClient(String id) {
        open();
        int result = db.delete("Client", "id=?", new String[]{id});
        close();
        return result;
    }

    // ------------------------------
    //    CLIENTS HORS LIGNE
    // ------------------------------
    public List<Client> getAllOfflineClients() {
        return dbHelper.getOfflineClients();
    }

    // ------------------------------
    //    MÉTHODE UTILE PRIVÉE
    // ------------------------------
    private Client extractClientFromCursor(Cursor cursor) {
        Client client = new Client();
        client.setId(cursor.getString(cursor.getColumnIndexOrThrow("id")));
        client.setNom(cursor.getString(cursor.getColumnIndexOrThrow("nom")));
        client.setEmail(cursor.getString(cursor.getColumnIndexOrThrow("email")));
        client.setMotDePasse(cursor.getString(cursor.getColumnIndexOrThrow("motDePasse")));
        client.setTelephone(cursor.getString(cursor.getColumnIndexOrThrow("telephone")));
        client.setSynced(cursor.getInt(cursor.getColumnIndexOrThrow("synced")) == 1);
        client.setGoogleSignIn(cursor.getInt(cursor.getColumnIndexOrThrow("googleSignIn")) == 1);
        return client;
    }
}
