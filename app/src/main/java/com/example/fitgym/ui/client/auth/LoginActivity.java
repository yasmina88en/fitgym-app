package com.example.fitgym.ui.client.auth;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

import com.example.fitgym.R;
import com.example.fitgym.data.dao.DAOClient;
import com.example.fitgym.data.db.DatabaseHelper;
import com.example.fitgym.data.db.FirebaseHelper;
import com.example.fitgym.data.model.Client;
import com.example.fitgym.ui.client.MainActivityClient;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.*;

public class LoginActivity extends AppCompatActivity {

    private EditText etLogin, etPassword;
    private Button btnSeConnecter;
    private TextView tvGoToRegister, tvForgotPassword;
    private SignInButton btnGoogleSignIn;

    private FirebaseAuth mAuth;
    private FirebaseHelper firebaseHelper;
    private GoogleSignInClient googleSignInClient;
    private DatabaseHelper dbHelper;

    private static final int RC_SIGN_IN = 1000;

    // 🔹 SharedPreferences
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_CURRENT_CLIENT_ID = "current_client_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_client);

        etLogin = findViewById(R.id.etLogin);
        etPassword = findViewById(R.id.etPassword);
        btnSeConnecter = findViewById(R.id.btnSeConnecter1);
        tvGoToRegister = findViewById(R.id.tvGoToRegister1);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);

        LinearLayout rootLayout = findViewById(R.id.rootLayout);
        Animation formAnim = AnimationUtils.loadAnimation(this, R.anim.form_anim);
        rootLayout.startAnimation(formAnim);

        mAuth = FirebaseAuth.getInstance();
        firebaseHelper = new FirebaseHelper();
        dbHelper = new DatabaseHelper(this);

        setupPasswordToggle();
        setupListeners();
        setupGoogleSignIn();
    }

    private void setupPasswordToggle() {
        etPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (etPassword.getRight()
                        - etPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {

                    if ((etPassword.getInputType()
                            & android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) == android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
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
    }

    private void setupListeners() {
        btnSeConnecter.setOnClickListener(v -> loginClient());

        tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        tvForgotPassword.setOnClickListener(v -> startActivity(new Intent(this, RecoverPasswordActivity.class)));
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
        btnGoogleSignIn.setOnClickListener(v -> googleSignIn());
    }

    private void loginClient() {
        String email = etLogin.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        Client localClient = dbHelper.getClientByEmail(email);

        // CAS HORS LIGNE
        if (!isNetworkAvailable()) {
            if (localClient != null && password.equals(localClient.getMotDePasse())) {
                proceedToMainActivity(localClient, "(offline)");
            } else {
                Toast.makeText(this, "Email ou mot de passe incorrect ❌ (offline)", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // CAS EN LIGNE
        if (localClient != null && !localClient.isSynced()) {
            syncLocalClientWithFirebase(localClient, password);
        } else {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            // Gestion détaillée des erreurs
                            String errorMessage = "Erreur de connexion ❌";

                            try {
                                throw task.getException();
                            } catch (FirebaseAuthInvalidUserException e) {
                                errorMessage = "Aucun compte trouvé avec cet email ❌";
                                Log.e("LoginActivity", "User not found: " + e.getMessage());
                            } catch (FirebaseAuthInvalidCredentialsException e) {
                                errorMessage = "Mot de passe incorrect ❌";
                                Log.e("LoginActivity", "Invalid credentials: " + e.getMessage());
                            } catch (FirebaseAuthUserCollisionException e) {
                                errorMessage = "Ce compte existe déjà ❌";
                                Log.e("LoginActivity", "User collision: " + e.getMessage());
                            } catch (FirebaseNetworkException e) {
                                errorMessage = "Problème de connexion Internet ❌";
                                Log.e("LoginActivity", "Network error: " + e.getMessage());
                            } catch (Exception e) {
                                errorMessage = "Erreur: " + (e.getMessage() != null ? e.getMessage() : "Inconnue");
                                Log.e("LoginActivity", "Login error: " + e.getMessage(), e);
                            }

                            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                            return;
                        }

                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user == null) {
                            Toast.makeText(this, "Erreur: Utilisateur non trouvé ❌", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Charger le client depuis Firebase
                        firebaseHelper.getClientById(user.getUid(), client -> {
                            Client finalClient;

                            if (client != null) {
                                // Client existe dans Firebase, on le sauvegarde dans SQLite
                                dbHelper.syncClient(client);
                                finalClient = client;
                                Log.d("LoginActivity", "Client chargé depuis Firebase: " + client.getNom());
                            } else {
                                // Client n'existe pas dans Firebase, on crée un objet avec les infos de
                                // FirebaseUser
                                finalClient = new Client();
                                finalClient.setId(user.getUid());
                                finalClient.setEmail(user.getEmail());
                                finalClient.setNom(
                                        user.getDisplayName() != null ? user.getDisplayName() : user.getEmail());
                                finalClient.setMotDePasse(password); // Sauvegarder le mot de passe pour le mode hors
                                                                     // ligne
                                finalClient.setSynced(true);

                                // Sauvegarder dans SQLite
                                dbHelper.syncClient(finalClient);

                                // Sauvegarder dans Firebase
                                firebaseHelper.ajouterClient(finalClient, success -> {
                                    Log.d("LoginActivity", "Client créé et sauvegardé dans Firebase");
                                });

                                Log.d("LoginActivity", "Client créé localement: " + finalClient.getNom());
                            }

                            proceedToMainActivity(finalClient, "");
                        });
                    });
        }
    }

    private void syncLocalClientWithFirebase(Client localClient, String password) {
        mAuth.fetchSignInMethodsForEmail(localClient.getEmail()).addOnCompleteListener(fetchTask -> {
            if (!fetchTask.isSuccessful()) {
                String errorMsg = "Erreur de vérification du compte ❌";
                if (fetchTask.getException() != null) {
                    errorMsg += ": " + fetchTask.getException().getMessage();
                    Log.e("LoginActivity", "Fetch sign-in methods error", fetchTask.getException());
                }
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                return;
            }

            boolean existsOnFirebase = fetchTask.getResult() != null &&
                    fetchTask.getResult().getSignInMethods() != null &&
                    !fetchTask.getResult().getSignInMethods().isEmpty();

            Log.d("LoginActivity", "Firebase fetch result: " + existsOnFirebase);

            if (existsOnFirebase) {
                // Le compte existe déjà sur Firebase, on se connecte
                mAuth.signInWithEmailAndPassword(localClient.getEmail(), password)
                        .addOnCompleteListener(loginTask -> {
                            if (!loginTask.isSuccessful()) {
                                String errorMessage = "Mot de passe incorrect ❌";
                                if (loginTask.getException() != null) {
                                    Log.e("LoginActivity", "Sign in error", loginTask.getException());
                                    if (loginTask.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                        errorMessage = "Mot de passe incorrect ❌";
                                    } else {
                                        errorMessage = "Erreur: " + loginTask.getException().getMessage();
                                    }
                                }
                                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                                return;
                            }
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user == null) {
                                Toast.makeText(this, "Erreur: Utilisateur non trouvé ❌", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            localClient.setId(user.getUid());
                            localClient.setSynced(true);
                            new DAOClient(this).modifierClient(localClient);

                            firebaseHelper.ajouterClient(localClient, success -> dbHelper.syncClient(localClient));
                            proceedToMainActivity(localClient, "(synced)");
                        });
            } else {
                // Le compte n'existe pas sur Firebase, on le crée
                mAuth.createUserWithEmailAndPassword(localClient.getEmail(), password)
                        .addOnCompleteListener(createTask -> {
                            if (!createTask.isSuccessful()) {
                                String errorMessage = "Erreur synchronisation Firebase ❌";
                                if (createTask.getException() != null) {
                                    Log.e("LoginActivity", "Create user error", createTask.getException());
                                    errorMessage += ": " + createTask.getException().getMessage();
                                }
                                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                                return;
                            }
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user == null) {
                                Toast.makeText(this, "Erreur: Utilisateur non créé ❌", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            localClient.setId(user.getUid());
                            localClient.setSynced(true);
                            new DAOClient(this).modifierClient(localClient);

                            firebaseHelper.ajouterClient(localClient, success -> dbHelper.syncClient(localClient));
                            proceedToMainActivity(localClient, "(synced)");
                        });
            }
        });
    }

    private void proceedToMainActivity(Client client, String extra) {
        // Sauvegarde de l'ID client dans SharedPreferences pour y accéder partout
        if (client != null && client.getId() != null && !client.getId().isEmpty()) {
            getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("current_client_id", client.getId())
                    .apply();
        }

        // Message de bienvenue
        Toast.makeText(this, "Bienvenue " + (client != null ? client.getNom() : "Utilisateur") + " " + extra + " ✅",
                Toast.LENGTH_SHORT).show();

        // Ouvrir MainActivityClient en lui passant l'ID (au cas où)
        Intent intent = new Intent(this, MainActivityClient.class);
        if (client != null && client.getId() != null) {
            intent.putExtra("client_id", client.getId());
        }
        startActivity(intent);
        finish();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    // =================== GOOGLE SIGN-IN ===================
    private void googleSignIn() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Google Sign-In nécessite Internet ❌", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(this, "Échec authentification Firebase ❌", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user == null)
                        return;

                    firebaseHelper.getClientById(user.getUid(), existingClient -> {
                        runOnUiThread(() -> {
                            Client client;
                            if (existingClient != null) {
                                dbHelper.syncClient(existingClient);
                                client = existingClient;
                            } else {
                                client = new Client();
                                client.setId(user.getUid());
                                client.setNom(user.getDisplayName());
                                client.setEmail(user.getEmail());
                                client.setMotDePasse("GOOGLE_SIGN_IN");
                                client.setSynced(true);

                                firebaseHelper.ajouterClient(client, success -> dbHelper.syncClient(client));
                            }
                            proceedToMainActivity(client, "(Google)");
                        });
                    });
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(Exception.class);
                if (account != null)
                    firebaseAuthWithGoogle(account);
                else
                    Toast.makeText(this, "Erreur Google Sign-In ❌", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Connexion Google échouée ❌", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
