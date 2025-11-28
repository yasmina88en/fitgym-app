package com.example.fitgym.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.fitgym.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class MoreBottomSheetAdmin extends BottomSheetDialogFragment {

    private MoreListener listener;

    // Interface pour gérer les clics
    public interface MoreListener {
        void onProfileSelected();
        void onUtilisateurSelected();
    }

    public void setListener(MoreListener listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.bottomsheet_more_admin, container, false);

        TextView btnProfile = view.findViewById(R.id.btn_profile);
        TextView btnUtilisateur = view.findViewById(R.id.btn_utilisateur);

        btnProfile.setOnClickListener(v -> {
            if (listener != null) listener.onProfileSelected();
            dismiss();
        });

        btnUtilisateur.setOnClickListener(v -> {
            if (listener != null) listener.onUtilisateurSelected();
            dismiss();
        });

        return view;
    }
}
