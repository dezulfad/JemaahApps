package com.example.jemaahapps;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    EditText e1, e2;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        e1 = findViewById(R.id.editText);   // email
        e2 = findViewById(R.id.editText2);  // password

        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser fUser = mAuth.getCurrentUser();
        if (fUser == null) {
            // No one logged in yet -> stay on login screen
            return;
        }

        String uid = fUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String role = doc.getString("role");
                        if ("admin".equals(role)) {
                            startActivity(new Intent(LoginActivity.this, AdminActivity.class));
                        } else {
                            startActivity(new Intent(LoginActivity.this, ProfileActivity.class));
                        }
                        finish();
                    } else {
                        // No role stored -> treat as normal user
                        startActivity(new Intent(LoginActivity.this, ProfileActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    // Optional: show error or stay on login
                    Toast.makeText(LoginActivity.this,
                            "Failed to load role", Toast.LENGTH_SHORT).show();
                });
    }

    public void loginUser(View v) {
        String email = e1.getText().toString().trim();
        String password = e2.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(LoginActivity.this,
                    "Blank not allowed", Toast.LENGTH_SHORT).show();
        } else {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(LoginActivity.this,
                                        "User logged in successfully", Toast.LENGTH_SHORT).show();

                                FirebaseUser fUser = mAuth.getCurrentUser();
                                if (fUser == null) return;

                                String uid = fUser.getUid();
                                FirebaseFirestore db = FirebaseFirestore.getInstance();

                                db.collection("users").document(uid).get()
                                        .addOnSuccessListener(doc -> {
                                            if (doc.exists()) {
                                                String role = doc.getString("role");
                                                if ("admin".equals(role)) {
                                                    startActivity(new Intent(LoginActivity.this, AdminActivity.class));
                                                } else {
                                                    startActivity(new Intent(LoginActivity.this, ProfileActivity.class));
                                                }
                                                finish();
                                            } else {
                                                // default if no role stored
                                                startActivity(new Intent(LoginActivity.this, ProfileActivity.class));
                                                finish();
                                            }
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(LoginActivity.this,
                                                        "Failed to load role", Toast.LENGTH_SHORT).show());
                            } else {
                                Toast.makeText(LoginActivity.this,
                                        "User could not be login", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }
    public void goToRegister(View v) {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }
}
