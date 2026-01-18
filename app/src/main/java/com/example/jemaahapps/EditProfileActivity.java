package com.example.jemaahapps;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class EditProfileActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 100;
    private static final int REQUEST_GALLERY = 200;
    private static final int REQUEST_CAMERA_PERMISSION = 300;
    private static final int REQUEST_STORAGE_PERMISSION = 400;

    private ImageView profileImageView;
    private Button btnCamera, btnGallery, btnSave;
    private EditText etFullName, etEmail, etPhone;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // ✅ Store original value for change detection
    private String originalPhoneNumber = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        profileImageView = findViewById(R.id.profileImageView);
        btnCamera = findViewById(R.id.btnCamera);
        btnGallery = findViewById(R.id.btnGallery);
        btnSave = findViewById(R.id.btnSave);

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadUserProfile();
        loadProfileImage();

        btnCamera.setOnClickListener(v -> checkCameraPermissionAndOpen());
        btnGallery.setOnClickListener(v -> checkStoragePermissionAndOpen());
        btnSave.setOnClickListener(v -> savePhoneNumber());
    }

    /* ================= USER DATA ================= */

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String phone = doc.getString("phone");

                    etFullName.setText(doc.getString("fullName"));
                    etPhone.setText(phone);
                    etEmail.setText(user.getEmail());

                    // Save original phone
                    originalPhoneNumber = phone;
                });
    }

    private void savePhoneNumber() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String newPhone = etPhone.getText().toString().trim();

        if (newPhone.isEmpty()) {
            etPhone.setError("Phone number cannot be empty");
            etPhone.requestFocus();
            return;
        }

        // ✅ No changes
        if (newPhone.equals(originalPhoneNumber)) {
            Toast.makeText(this,
                    "No changes were detected. Your profile information remains unchanged.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .update("phone", newPhone)
                .addOnSuccessListener(unused -> {
                    originalPhoneNumber = newPhone;
                    Toast.makeText(this,
                            "Your profile has been successfully updated.",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Unable to update profile at this time. Please try again later.",
                                Toast.LENGTH_SHORT).show());
    }

    /* ================= PROFILE IMAGE ================= */

    private void loadProfileImage() {
        String encodedImage = getSharedPreferences("user_profile", MODE_PRIVATE)
                .getString("profile_image", null);

        if (encodedImage != null) {
            byte[] decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            profileImageView.setImageBitmap(bitmap);
        }
    }

    private void saveImageToPrefs(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        String encodedImage = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

        getSharedPreferences("user_profile", MODE_PRIVATE)
                .edit()
                .putString("profile_image", encodedImage)
                .apply();

        Toast.makeText(this,
                "Your profile picture has been updated.",
                Toast.LENGTH_SHORT).show();
    }

    /* ================= CAMERA & GALLERY ================= */

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            openCamera();
        }
    }

    private void checkStoragePermissionAndOpen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_STORAGE_PERMISSION);
            } else {
                openGallery();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            } else {
                openGallery();
            }
        }
    }

    private void openCamera() {
        startActivityForResult(
                new Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                REQUEST_CAMERA
        );
    }

    private void openGallery() {
        startActivityForResult(
                new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
                REQUEST_GALLERY
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {

            if (requestCode == REQUEST_CAMERA) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                if (photo != null) {
                    profileImageView.setImageBitmap(photo);
                    saveImageToPrefs(photo);
                }

            } else if (requestCode == REQUEST_GALLERY) {
                Uri uri = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media
                            .getBitmap(getContentResolver(), uri);
                    profileImageView.setImageBitmap(bitmap);
                    saveImageToPrefs(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
