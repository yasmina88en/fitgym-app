package com.example.fitgym.ui.client;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.example.fitgym.R;
import com.example.fitgym.data.db.FirebaseHelper;
import com.example.fitgym.data.model.Client;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ProfileFragmentClient extends Fragment {

    private Button btnLogout, btnEditProfile, btnChangePassword, btnNotifications;
    private TextView profileEmail, profileName;
    private ImageButton changePhotoBtn;
    private ImageView avatarImage;

    private FirebaseHelper firebaseHelper;
    private Client currentClient;
    private ActivityResultLauncher<Intent> pickImageLauncher;

    private View rootView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseHelper = new FirebaseHelper();

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (getActivity() == null) return;
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (currentClient == null || avatarImage == null) return;

                        try {
                            InputStream is = getActivity().getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(is);
                            avatarImage.setImageBitmap(getCircularBitmap(bitmap));

                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                            String imageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

                            firebaseHelper.updateClientPhoto(currentClient.getId(), imageBase64, success -> {
                                if (getActivity() == null) return;
                                String message = success ? "Photo mise à jour" : "Erreur de mise à jour";
                                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                            });

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_profile_client, container, false);

        profileEmail = rootView.findViewById(R.id.profileEmail);
        profileName = rootView.findViewById(R.id.profileName);
        btnLogout = rootView.findViewById(R.id.btnLogout);
        btnEditProfile = rootView.findViewById(R.id.btnEditProfile);
        btnChangePassword = rootView.findViewById(R.id.btnChangePassword);
        btnNotifications = rootView.findViewById(R.id.btnNotifications);
        changePhotoBtn = rootView.findViewById(R.id.changePhotoBtn);
        avatarImage = rootView.findViewById(R.id.avatarImage);

        profileEmail.setText("");
        profileName.setText("Client");
        if (avatarImage != null) avatarImage.setImageResource(R.drawable.ic_avatar_placeholder);

        changePhotoBtn.setOnClickListener(v -> {
            if (getActivity() == null) return;
            Intent pick = new Intent(Intent.ACTION_PICK);
            pick.setType("image/*");
            pickImageLauncher.launch(pick);
        });

        btnLogout.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().finish();
        });

        btnEditProfile.setOnClickListener(v -> showChangeMailDialog());

        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        btnNotifications.setOnClickListener(v -> {
            // Notifications
        });

        if (currentClient != null) updateClient(currentClient);

        return rootView;
    }

    private void showChangeMailDialog() {
        if (currentClient == null || getActivity() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_mail_client, null);
        EditText oldMail = dialogView.findViewById(R.id.oldEmail);
        EditText newMail = dialogView.findViewById(R.id.newEmail);

        oldMail.setText(currentClient.getEmail());
        newMail.setText("");

        new AlertDialog.Builder(getActivity())
                .setTitle("Changer votre mail")
                .setView(dialogView)
                .setPositiveButton("Enregistrer", (dialog, which) -> {
                    String email = newMail.getText().toString().trim();
                    if (!email.isEmpty()) {
                        firebaseHelper.updateClientEmail(currentClient.getId(), email, success -> {
                            if (getActivity() == null) return;
                            if (success) {
                                currentClient.setEmail(email);
                                if (profileEmail != null) profileEmail.setText(email);
                                Toast.makeText(getActivity(), "Email mis à jour", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getActivity(), "Erreur lors de la mise à jour", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void showChangePasswordDialog() {
        if (currentClient == null || getActivity() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_password_client, null);
        EditText current = dialogView.findViewById(R.id.currentPassword);
        EditText nouv = dialogView.findViewById(R.id.newPassword);
        EditText confirm = dialogView.findViewById(R.id.confirmPassword);

        new AlertDialog.Builder(getActivity())
                .setTitle("Changer le mot de passe")
                .setView(dialogView)
                .setPositiveButton("Enregistrer", (dialog, which) -> {
                    String currentPassword = current.getText().toString().trim();
                    String newPassword = nouv.getText().toString().trim();
                    String confirmPassword = confirm.getText().toString().trim();

                    if (!newPassword.equals(confirmPassword)) {
                        Toast.makeText(getActivity(), "Les mots de passe ne correspondent pas ❌", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (newPassword.length() < 6) {
                        Toast.makeText(getActivity(), "Mot de passe trop court ❌", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // ===== REAUTHENTIFICATION ET MISE À JOUR FIREBASE =====
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
                        user.reauthenticate(credential).addOnCompleteListener(authTask -> {
                            if (authTask.isSuccessful()) {
                                user.updatePassword(newPassword).addOnCompleteListener(updateTask -> {
                                    if (updateTask.isSuccessful()) {
                                        // Mise à jour dans Realtime Database
                                        firebaseHelper.updateClientPassword(currentClient.getId(), newPassword, success -> {
                                            if (getActivity() == null) return;
                                            String msg = success ? "Mot de passe mis à jour ✅" : "Erreur mise à jour ❌";
                                            Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
                                        });
                                    } else {
                                        Toast.makeText(getActivity(), "Erreur lors du changement de mot de passe ❌", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                Toast.makeText(getActivity(), "Mot de passe actuel incorrect ❌", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    public void updateClient(Client client) {
        if (client == null) return;
        currentClient = client;

        if (profileName != null) profileName.setText(client.getNom());
        if (profileEmail != null) profileEmail.setText(client.getEmail());

        if (firebaseHelper != null && avatarImage != null) {
            firebaseHelper.getClientPhoto(client.getId(), photoBase64 -> {
                if (photoBase64 != null && !photoBase64.isEmpty()) {
                    byte[] decoded = Base64.decode(photoBase64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                    if (avatarImage != null) avatarImage.setImageBitmap(getCircularBitmap(bitmap));
                } else {
                    if (avatarImage != null) avatarImage.setImageResource(R.drawable.ic_avatar_placeholder);
                }
            });
        }
    }

    private Bitmap getCircularBitmap(Bitmap bitmap) {
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, size, size);
        RectF rectF = new RectF(rect);
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        canvas.drawOval(rectF, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, null, rect, paint);
        return output;
    }
}
