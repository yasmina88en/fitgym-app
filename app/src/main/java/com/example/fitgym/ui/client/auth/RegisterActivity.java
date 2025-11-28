package com.example.fitgym.ui.client.auth;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fitgym.R;
import com.example.fitgym.data.dao.DAOClient;
import com.example.fitgym.data.db.DatabaseHelper;
import com.example.fitgym.data.db.FirebaseHelper;
import com.example.fitgym.data.model.Client;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;

import java.util.List;

public class RegisterActivity extends AppCompatActivity {

    private EditText inputNom, inputEmail, inputTelephone, inputPassword;
    private Button btnRegister;
    private TextView tvGoToLogin;
    private SignInButton btnGoogleSignIn;

    private FirebaseAuth mAuth;
    private FirebaseHelper firebaseHelper;
    private DatabaseHelper dbHelper;
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registre_client);

        inputNom = findViewById(R.id.inputNom);
        inputEmail = findViewById(R.id.inputEmail);
        inputTelephone = findViewById(R.id.inputTelephone);
        inputPassword = findViewById(R.id.inputPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);

        mAuth = FirebaseAuth.getInstance();
        firebaseHelper = new FirebaseHelper();
        dbHelper = new DatabaseHelper(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        tvGoToLogin.setOnClickListener(v -> finish());
        btnRegister.setOnClickListener(v -> inscrireClient());
        btnGoogleSignIn.setOnClickListener(v -> googleSignIn());

        if (hasInternet()) syncOfflineClients();
    }

    private boolean hasInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo net = cm.getActiveNetworkInfo();
        return net != null && net.isConnected();
    }

    // =================== INSCRIPTION EMAIL/PASSWORD ===================
    private void inscrireClient() {
        String nom = inputNom.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        String tel = inputTelephone.getText().toString().trim();
        String pass = inputPassword.getText().toString().trim();

        if (TextUtils.isEmpty(nom) || TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
            Toast.makeText(this, "Remplissez tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email invalide ❌", Toast.LENGTH_SHORT).show();
            return;
        }

        // Vérifie si le compte existe déjà localement
        Client existingLocal = dbHelper.getClientByEmail(email);
        if (existingLocal != null) {
            Toast.makeText(this, "Email déjà utilisé ❌", Toast.LENGTH_SHORT).show();
            return;
        }

        Client client = new Client();
        client.setNom(nom);
        client.setEmail(email);
        client.setTelephone(tel);
        client.setMotDePasse(pass); // on stocke temporairement le mot de passe clair
        client.setGoogleSignIn(false);

        if (!hasInternet()) {
            // OFFLINE → stocke localement avec flag "non synchronisé"
            client.setId(String.valueOf(System.currentTimeMillis()));
            client.setSynced(false);
            new DAOClient(this).ajouterClient(client);
            Toast.makeText(this, "Compte enregistré hors-ligne ✅", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ONLINE → Vérifie si déjà sur Firebase
        mAuth.fetchSignInMethodsForEmail(email).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(this, "Erreur vérification email: " + task.getException().getMessage(),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> methods = task.getResult().getSignInMethods();
            if (methods != null && !methods.isEmpty()) {
                Toast.makeText(this, "Email déjà utilisé sur Firebase ❌", Toast.LENGTH_SHORT).show();
                return;
            }

            // Crée le compte sur Firebase Auth
            mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(createTask -> {
                if (!createTask.isSuccessful()) {
                    Toast.makeText(this, "Erreur création compte: " + createTask.getException().getMessage(),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                FirebaseUser firebaseUser = mAuth.getCurrentUser();
                if (firebaseUser == null) return;

                client.setId(firebaseUser.getUid());
                client.setSynced(true);

                // Ajoute sur Firebase Database et synchronise localement
                firebaseHelper.ajouterClient(client, success -> {
                    dbHelper.syncClient(client);
                    Toast.makeText(this, "Compte créé avec succès ✅", Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        });
    }

    // =================== SYNC OFFLINE ===================
    private void syncOfflineClients() {
        DAOClient dao = new DAOClient(this);
        List<Client> offlineClients = dao.getAllOfflineClients();

        for (Client c : offlineClients) {
            if (c.isSynced()) continue;

            String email = c.getEmail() != null ? c.getEmail().trim() : "";
            String pass = c.getMotDePasse() != null ? c.getMotDePasse().trim() : "";

            if (email.isEmpty() || (!c.isGoogleSignIn() && pass.isEmpty())) continue;

            if (c.isGoogleSignIn()) {
                // Google → ajouter si non existant
                firebaseHelper.getClientById(c.getId(), existing -> {
                    if (existing == null) firebaseHelper.ajouterClient(c, success -> {});
                    c.setSynced(true);
                    dao.modifierClient(c);
                });
            } else {
                // Email/Password → créer sur Firebase
                mAuth.fetchSignInMethodsForEmail(email).addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) return;

                    List<String> providers = task.getResult().getSignInMethods();
                    if (providers != null && !providers.isEmpty()) {
                        c.setSynced(true);
                        dao.modifierClient(c);
                    } else {
                        mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(createTask -> {
                            if (createTask.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) c.setId(user.getUid());
                                c.setSynced(true);
                                dao.modifierClient(c);
                                firebaseHelper.ajouterClient(c, success -> {});
                            }
                        });
                    }
                });
            }
        }
    }

    // =================== GOOGLE SIGN-IN ===================
    private void googleSignIn() {
        if (!hasInternet()) {
            Toast.makeText(this, "Google Sign-In nécessite Internet ❌", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivityForResult(googleSignInClient.getSignInIntent(), RC_SIGN_IN);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(this, "Échec authentification Google ❌", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) return;

            Client client = new Client();
            client.setId(user.getUid());
            client.setNom(user.getDisplayName());
            client.setEmail(user.getEmail());
            client.setMotDePasse(null);
            client.setGoogleSignIn(true);
            client.setSynced(true);

            dbHelper.syncClient(client);

            firebaseHelper.getClientById(user.getUid(), existing -> {
                if (existing == null) firebaseHelper.ajouterClient(client, success -> {});
            });

            Toast.makeText(this, "Connecté avec Google ✅", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(Exception.class);
                if (account != null) firebaseAuthWithGoogle(account);
            } catch (Exception e) {
                Toast.makeText(this, "Connexion Google échouée ❌", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasInternet()) syncOfflineClients();
    }
}
