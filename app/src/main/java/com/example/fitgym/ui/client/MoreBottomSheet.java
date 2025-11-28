package com.example.fitgym.ui.client;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.fitgym.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class MoreBottomSheet extends BottomSheetDialogFragment {

    private MoreListener listener;

    public interface MoreListener {
        void onProfileSelected();
        void onFavorisSelected();
    }

    public void setListener(MoreListener listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.bottomsheet_more, container, false);

        TextView btnProfile = view.findViewById(R.id.btn_profile);
        TextView btnFavoris = view.findViewById(R.id.btn_favoris);

        btnProfile.setOnClickListener(v -> {
            if (listener != null) listener.onProfileSelected();
            dismiss();
        });

        btnFavoris.setOnClickListener(v -> {
            if (listener != null) listener.onFavorisSelected();
            dismiss();
        });

        return view;
    }
}
