package com.example.fitgym.ui.admin;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.example.fitgym.data.db.DatabaseHelper;
import com.example.fitgym.R;
import com.example.fitgym.data.model.Admin;
import com.example.fitgym.data.db.FirebaseHelper;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

public class ProfileFragmentAdmin extends Fragment {


    Button btnLogout, btnEditProfile, btnChangePassword, btnNotifications;
    TextView profileEmail;
    DatabaseHelper dbHelper;
    FirebaseHelper firebaseHelper;

    ImageButton changePhotoBtn;
    ImageView avatarImage;

    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialiser le launcher pour choisir l'image
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream is = getActivity().getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(is);

                            // 🔵 Transformer l'image en cercle pour l'affichage
                            Bitmap circularBitmap = getCircularBitmap(bitmap);
                            avatarImage.setImageBitmap(circularBitmap);

                            // 🔵 Encoder la photo originale en Base64 pour Firebase
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                            String imageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

                            // 🔵 Sauvegarder dans Firebase
                            firebaseHelper.updateAdminPhoto(imageBase64, success -> {
                                if (success) {
                                    Toast.makeText(getActivity(), "Photo mise à jour", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getActivity(), "Erreur lors de la mise à jour", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile_admin, container, false);

        profileEmail = view.findViewById(R.id.profileEmail);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnNotifications = view.findViewById(R.id.btnNotifications);
        changePhotoBtn = view.findViewById(R.id.changePhotoBtn);
        avatarImage = view.findViewById(R.id.avatarImage);

        dbHelper = new DatabaseHelper(getActivity());
        firebaseHelper = new FirebaseHelper();

        // Changer la photo
        changePhotoBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        loadAdminData();

        btnEditProfile.setOnClickListener(v -> showChangeEmailDialog());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        btnNotifications.setOnClickListener(v -> {
            Intent intent = new Intent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getActivity().getPackageName());
            } else {
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", getActivity().getPackageName(), null));
            }
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            if (getActivity() != null)
                getActivity().finish();
        });

        return view;
    }

    // Méthode pour transformer un Bitmap en cercle
    private Bitmap getCircularBitmap(Bitmap bitmap) {
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, size, size);
        final RectF rectF = new RectF(rect);

        float radius = size / 2f;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.WHITE);
        canvas.drawOval(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, null, rect, paint);

        return output;
    }

    private void loadAdminData() {
        Admin localAdmin = dbHelper.getAdmin();
        if (localAdmin != null) {
            profileEmail.setText(localAdmin.getLogin());
        }

        // Charger la photo depuis Firebase uniquement si connecté
        if (isConnected()) {
            firebaseHelper.getAdminPhoto(photoBase64 -> {
                if (photoBase64 != null && !photoBase64.isEmpty()) {
                    byte[] decodedBytes = Base64.decode(photoBase64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                    // Transformer la photo en cercle
                    Bitmap circularBitmap = getCircularBitmap(bitmap);
                    avatarImage.setImageBitmap(circularBitmap);
                } else {
                    avatarImage.setImageResource(R.drawable.ic_avatar_placeholder);
                }
            });
        } else {
            avatarImage.setImageResource(R.drawable.ic_avatar_placeholder);
        }
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    private void showChangeEmailDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Changer votre email");
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_mail_admin, null);
        builder.setView(dialogView);

        EditText Email = dialogView.findViewById(R.id.Email);
        EditText NewEmail = dialogView.findViewById(R.id.NewEmail);
        EditText Password = dialogView.findViewById(R.id.Password);

        builder.setPositiveButton("Valider", (dialog, which) -> {
            if (!isConnected()) {
                Toast.makeText(getActivity(), "Vérifiez votre connexion Internet", Toast.LENGTH_SHORT).show();
                return;
            }

            String oldEmail = Email.getText().toString().trim();
            String newEmail = NewEmail.getText().toString().trim();
            String password = Password.getText().toString().trim();

            Admin localAdmin = dbHelper.getAdmin();
            if (localAdmin != null && localAdmin.getLogin().equals(oldEmail)
                    && localAdmin.getMotDePasse().equals(password)) {

                firebaseHelper.updateAdminEmail(newEmail, success -> {
                    if (success) {
                        dbHelper.updateAdminEmail(newEmail);
                        profileEmail.setText(newEmail);
                        Toast.makeText(getActivity(), "Email mis à jour avec succès", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity(), "Erreur lors de la mise à jour Firebase", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(getActivity(), "Email ou mot de passe incorrect", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Changer votre mot de passe");
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password_admin, null);
        builder.setView(dialogView);

        EditText etCurrent = dialogView.findViewById(R.id.CurrentPassword);
        EditText etNew = dialogView.findViewById(R.id.NewPassword);
        EditText etConfirm = dialogView.findViewById(R.id.ConfirmPassword);

        builder.setPositiveButton("Valider", (dialog, which) -> {
            if (!isConnected()) {
                Toast.makeText(getActivity(), "Vérifiez votre connexion Internet", Toast.LENGTH_SHORT).show();
                return;
            }

            String current = etCurrent.getText().toString().trim();
            String newPass = etNew.getText().toString().trim();
            String confirm = etConfirm.getText().toString().trim();

            Admin localAdmin = dbHelper.getAdmin();
            if (localAdmin != null && localAdmin.getMotDePasse().equals(current)) {
                if (newPass.equals(confirm)) {
                    firebaseHelper.updateAdminPassword(newPass, success -> {
                        if (success) {
                            dbHelper.updateAdminPassword(newPass);
                            Toast.makeText(getActivity(), "Mot de passe mis à jour", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(), "Erreur lors de la mise à jour Firebase", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(getActivity(), "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getActivity(), "Mot de passe actuel incorrect", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss());
        builder.show();
    }


}
