package com.example.fitgym.ui.admin;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.fitgym.data.db.FirebaseHelper;
import com.example.fitgym.R;
import com.example.fitgym.data.db.DatabaseHelper;
import com.example.fitgym.data.model.Admin;

public class LoginAdminActivity extends AppCompatActivity {

    EditText etLogin, etPassword;
    Button btnSeConnecter;
    DatabaseHelper dbHelper;
    ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_admin);

        etLogin = findViewById(R.id.etLogin);
        etPassword = findViewById(R.id.etPassword);
        btnSeConnecter = findViewById(R.id.btnSeConnecter);
        progressBar = findViewById(R.id.progressBar);

        dbHelper = new DatabaseHelper(this);
        // Toggle mot de passe visible/invisible
        etPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (etPassword.getRight()
                        - etPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {

                    if ((etPassword.getInputType() & android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
                            == android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                        etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    } else {
                        etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                                android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    }
                    etPassword.setSelection(etPassword.getText().length());
                    return true;
                }
            }
            return false;
        });
        btnSeConnecter.setOnClickListener(v -> {
            String login = etLogin.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            progressBar.setVisibility(View.VISIBLE);

            if (!isNetworkAvailable()) {
                // MODE OFFLINE → SQLite
                Admin localAdmin = dbHelper.getAdmin(login, password);
                if (localAdmin != null) {
                    Toast.makeText(this, "Bienvenue " + localAdmin.getLogin() + " (offline) ✅", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginAdminActivity.this, MainActivityAdmin.class));
                    finish();
                } else {
                    Toast.makeText(this, "Login ou mot de passe incorrect ❌ (offline)", Toast.LENGTH_SHORT).show();
                }
            } else {
                // MODE ONLINE → Firebase
                FirebaseHelper firebaseHelper = new FirebaseHelper();
                firebaseHelper.getAdmin(new FirebaseHelper.AdminCallback() {
                    @Override
                    public void onCallback(Admin admin) {
                        if (admin != null && admin.getLogin().equals(login) && admin.getMotDePasse().equals(password)) {
                            runOnUiThread(() -> {
                                Toast.makeText(LoginAdminActivity.this, "Bienvenue " + admin.getLogin() + " ! ✅", Toast.LENGTH_SHORT).show();
                                dbHelper.syncAdmin(admin);
                                startActivity(new Intent(LoginAdminActivity.this, MainActivityAdmin.class));
                                finish();
                            });
                        } else {
                            runOnUiThread(() ->
                                    Toast.makeText(LoginAdminActivity.this, "Login ou mot de passe incorrect ❌", Toast.LENGTH_SHORT).show()
                            );
                        }
                }

            });
            }
        });
    }

    // Vérifie la connexion Internet
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
