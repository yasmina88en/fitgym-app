package com.example.fitgym.data.dao;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.fitgym.data.db.DatabaseHelper;
import com.example.fitgym.data.model.Favoris;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DAOFavoris {

    private final DatabaseHelper dbHelper;

    public DAOFavoris(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    // Ajouter un favori
    public boolean ajouterFavori(Favoris f) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (f.getId() == null || f.getId().isEmpty()) {
            f.setId(UUID.randomUUID().toString());
        }
        ContentValues cv = new ContentValues();
        cv.put("id", f.getId());
        cv.put("clientId", f.getClientId());
        cv.put("seanceId", f.getSeanceId());

        long res = db.insertWithOnConflict("favoris", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
        return res != -1;
    }

    // Supprimer un favori
    public boolean supprimerFavori(String id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete("favoris", "id=?", new String[]{id});
        db.close();
        return rows > 0;
    }

    // Supprimer tous les favoris d'un client
    public boolean supprimerFavorisClient(String clientId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete("favoris", "clientId=?", new String[]{clientId});
        db.close();
        return rows > 0;
    }

    // Vérifier si une séance est déjà favorite
    public boolean estFavori(String clientId, String seanceId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT id FROM favoris WHERE clientId=? AND seanceId=?",
                new String[]{clientId, seanceId}
        );
        boolean exists = c.moveToFirst();
        c.close();
        db.close();
        return exists;
    }

    // Récupérer tous les favoris d'un client
    public List<Favoris> getFavorisClient(String clientId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT * FROM favoris WHERE clientId=?",
                new String[]{clientId}
        );

        List<Favoris> list = new ArrayList<>();
        while (c.moveToNext()) {
            Favoris f = new Favoris();
            f.setId(c.getString(c.getColumnIndexOrThrow("id")));
            f.setClientId(c.getString(c.getColumnIndexOrThrow("clientId")));
            f.setSeanceId(c.getString(c.getColumnIndexOrThrow("seanceId")));
            list.add(f);
        }
        c.close();
        db.close();
        return list;
    }
}
