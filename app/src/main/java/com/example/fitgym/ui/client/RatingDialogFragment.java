package com.example.fitgym.ui.client;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.fitgym.R;
import com.example.fitgym.data.model.Avis;
import com.example.fitgym.data.repository.AvisRepository;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RatingDialogFragment extends BottomSheetDialogFragment {

    private String coachId;
    private String coachName;
    private double selectedRating = 0;

    private LinearLayout starsContainer;
    private EditText commentaireInput;
    private TextView ratingText;
    private MaterialButton btnEnvoyer, btnAnnuler;

    private FirebaseAuth auth;
    private AvisRepository avisRepository;

    private OnRatingSubmitListener listener;

    public interface OnRatingSubmitListener {
        void onRatingSubmitted();
    }

    public static RatingDialogFragment newInstance(String coachId, String coachName, OnRatingSubmitListener listener) {
        RatingDialogFragment fragment = new RatingDialogFragment();
        Bundle args = new Bundle();
        args.putString("coach_id", coachId);
        args.putString("coach_name", coachName);
        fragment.setArguments(args);
        fragment.setListener(listener);
        return fragment;
    }

    public void setListener(OnRatingSubmitListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        avisRepository = new AvisRepository();

        if (getArguments() != null) {
            coachId = getArguments().getString("coach_id");
            coachName = getArguments().getString("coach_name");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rating_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        starsContainer = view.findViewById(R.id.starsContainer);
        commentaireInput = view.findViewById(R.id.commentaireInput);
        ratingText = view.findViewById(R.id.ratingText);
        btnEnvoyer = view.findViewById(R.id.btnEnvoyer);
        btnAnnuler = view.findViewById(R.id.btnAnnuler);

        createInteractiveStars();

        btnEnvoyer.setOnClickListener(v -> submitRating());
        btnAnnuler.setOnClickListener(v -> dismiss());
    }

    private void createInteractiveStars() {
        starsContainer.removeAllViews();

        for (int i = 1; i <= 5; i++) {
            final int position = i;
            ImageView star = new ImageView(requireContext());
            star.setTag(position);
            star.setImageResource(R.drawable.ic_star_empty);
            star.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(80, 80);
            params.setMarginEnd(16);
            star.setLayoutParams(params);

            star.setOnClickListener(v -> {
                selectedRating = position;
                updateStarDisplay();
            });

            starsContainer.addView(star);
        }
    }

    private void updateStarDisplay() {
        for (int i = 0; i < starsContainer.getChildCount(); i++) {
            ImageView star = (ImageView) starsContainer.getChildAt(i);
            int position = (int) star.getTag();

            if (position <= selectedRating) {
                star.setImageResource(R.drawable.ic_star_filled);
            } else {
                star.setImageResource(R.drawable.ic_star_empty);
            }
        }

        updateRatingText();
    }

    private void updateRatingText() {
        String[] ratings = {
                "😞 Terrible - Ne recommande pas",
                "😕 Mauvais - À améliorer",
                "😐 Acceptable - C'est correct",
                "😊 Très bon - Recommande",
                "🤩 Excellent - Exceptionnel !"
        };

        String[] colors = {"#DC2626", "#EA580C", "#F59E0B", "#10B981", "#059669"};

        if (selectedRating > 0 && selectedRating <= 5) {
            int index = (int) selectedRating - 1;
            ratingText.setText(ratings[index]);
            ratingText.setTextColor(android.graphics.Color.parseColor(colors[index]));
            ratingText.setTextSize(14);
        }
    }

    private void submitRating() {
        if (selectedRating == 0) {
            Toast.makeText(requireContext(), "⭐ Veuillez sélectionner une note", Toast.LENGTH_SHORT).show();
            return;
        }

        String commentaire = commentaireInput.getText().toString().trim();
        if (commentaire.isEmpty()) {
            Toast.makeText(requireContext(), "✍️ Veuillez ajouter un commentaire", Toast.LENGTH_SHORT).show();
            return;
        }

        if (commentaire.length() < 10) {
            Toast.makeText(requireContext(), "✍️ Le commentaire doit avoir au moins 10 caractères", Toast.LENGTH_SHORT).show();
            return;
        }

        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "🔐 Vous devez être connecté", Toast.LENGTH_SHORT).show();
            return;
        }

        String clientId = auth.getCurrentUser().getUid();
        String clientName = auth.getCurrentUser().getDisplayName() != null ?
                auth.getCurrentUser().getDisplayName() : "Client";
        String clientPhotoUrl = auth.getCurrentUser().getPhotoUrl() != null ?
                auth.getCurrentUser().getPhotoUrl().toString() : "";

        Avis avis = new Avis(
                null,
                clientId,
                clientName,
                commentaire,
                new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH).format(new Date()),
                selectedRating,
                clientPhotoUrl,
                System.currentTimeMillis()
        );

        btnEnvoyer.setEnabled(false);
        btnEnvoyer.setText("⏳ Envoi en cours...");

        // Utiliser AvisRepository
        avisRepository.ajouterAvis(clientId, coachId, avis, new AvisRepository.AvisCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                Toast.makeText(requireContext(), "✅ Avis envoyé avec succès !", Toast.LENGTH_SHORT).show();
                if (listener != null) {
                    listener.onRatingSubmitted();
                }
                dismiss();
            }

            @Override
            public void onError(String error) {
                btnEnvoyer.setEnabled(true);
                btnEnvoyer.setText("Envoyer mon avis");
                Toast.makeText(requireContext(), "❌ Erreur : " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}