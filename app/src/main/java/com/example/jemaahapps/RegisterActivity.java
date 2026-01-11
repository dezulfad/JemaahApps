package com.example.jemaahapps;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
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

public class RegisterActivity extends AppCompatActivity {

    EditText e1, e2;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        e1 = findViewById(R.id.editText);   // Email
        e2 = findViewById(R.id.editText2);  // Password

        mAuth = FirebaseAuth.getInstance();

        // -------------------------------------
        // SIMPLE EMAIL VALIDATION (live check)
        // -------------------------------------
        e1.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String email = s.toString();
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    e1.setError("Invalid email address");
                } else {
                    e1.setError(null);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // -------------------------------------
        // SIMPLE PASSWORD VALIDATION (live check)
        // -------------------------------------
        e2.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() < 6) {
                    e2.setError("Minimum 6 characters");
                } else {
                    e2.setError(null);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    public void createUser(View v){

        String email = e1.getText().toString().trim();
        String password = e2.getText().toString().trim();

        // Final validation before register
        if(email.isEmpty()){
            e1.setError("Email cannot be empty");
            e1.requestFocus();
            return;
        }

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            e1.setError("Invalid email address");
            e1.requestFocus();
            return;
        }

        if(password.isEmpty()){
            e2.setError("Password cannot be empty");
            e2.requestFocus();
            return;
        }

        if(password.length() < 6){
            e2.setError("Minimum 6 characters");
            e2.requestFocus();
            return;
        }

        // Firebase create user
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            Toast.makeText(getApplicationContext(),"User created successfully",Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                            finish();
                        } else {
                            Toast.makeText(getApplicationContext(),"Registration failed",Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }
}