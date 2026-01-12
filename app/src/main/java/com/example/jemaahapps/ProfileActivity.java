package com.example.jemaahapps;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.CaptureActivity;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    FirebaseAuth auth;
    FirebaseUser user;
    TextView profileText;
    TextView tvUpcomingProgram;
    Button openMap, cameraBtn, galleryBtn;

    ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri imageUri = result.getData().getData();
                            decodeFromGallery(imageUri);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        profileText = findViewById(R.id.textView);
        tvUpcomingProgram = findViewById(R.id.tvUpcomingProgram);
        openMap = findViewById(R.id.button);
        cameraBtn = findViewById(R.id.cameraBtn);
        galleryBtn = findViewById(R.id.galleryBtn);

        user = auth.getCurrentUser();
        if (profileText != null) {
            if (user != null) {
                String uid = user.getUid();
                FirebaseFirestore db = FirebaseFirestore.getInstance();

                // Load profile name
                db.collection("users").document(uid).get()
                        .addOnSuccessListener(doc -> {
                            String fullName = doc.getString("fullName");
                            if (fullName != null && !fullName.isEmpty()) {
                                profileText.setText(fullName);
                            } else {
                                profileText.setText(user.getEmail());
                            }
                        })
                        .addOnFailureListener(e ->
                                profileText.setText(user.getEmail()));

                // Load upcoming / last joined program
                loadUpcomingProgram(uid);

            } else {
                profileText.setText("No user logged in");
                if (tvUpcomingProgram != null) {
                    tvUpcomingProgram.setText("No upcoming program");
                }
            }
        }

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE
        }, 1);

        cameraBtn.setOnClickListener(v -> startCameraScan());
        galleryBtn.setOnClickListener(v -> selectImageFromGallery());
    }

    // 1) Get latest joined program from `scans`
    // 2) Get its start time from `programs/{programName}` (admin-controlled)
    private void loadUpcomingProgram(String uid) {
        if (tvUpcomingProgram == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Step 1: latest scan for this user
        db.collection("scans")
                .whereEqualTo("userId", uid)
                .orderBy("scannedAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(scanSnap -> {
                    if (scanSnap.isEmpty()) {
                        tvUpcomingProgram.setText("No upcoming program");
                        return;
                    }

                    String programName = scanSnap
                            .getDocuments()
                            .get(0)
                            .getString("programName");

                    if (programName == null || programName.trim().isEmpty()) {
                        tvUpcomingProgram.setText("No upcoming program");
                        return;
                    }

                    // Step 2: program details from `programs` collection
                    // assumes doc id in `programs` is exactly the programName (e.g. "Program F")
                    db.collection("programs")
                            .document(programName)
                            .get()
                            .addOnSuccessListener(programDoc -> {
                                if (!programDoc.exists()) {
                                    // no extra info, show name only
                                    tvUpcomingProgram.setText(programName);
                                    return;
                                }

                                // Try as Timestamp first
                                Timestamp startTs = programDoc.getTimestamp("programStartTime");

                                if (startTs != null) {
                                    SimpleDateFormat sdf =
                                            new SimpleDateFormat("dd/MM/yyyy HH:mm");
                                    String timeStr = sdf.format(startTs.toDate());
                                    tvUpcomingProgram.setText(programName + " - " + timeStr);
                                } else {
                                    // Or as plain string (if admin stored it that way)
                                    String startStr = programDoc.getString("programStartTime");
                                    if (startStr != null && !startStr.isEmpty()) {
                                        tvUpcomingProgram.setText(programName + " - " + startStr);
                                    } else {
                                        tvUpcomingProgram.setText(programName);
                                    }
                                }
                            })
                            .addOnFailureListener(e ->
                                    tvUpcomingProgram.setText("Failed to load program info"));
                })
                .addOnFailureListener(e ->
                        tvUpcomingProgram.setText("Failed to load joined program"));
    }

    public void openMap(View view) {
        Intent intent = new Intent(ProfileActivity.this, MapsActivity.class);
        startActivity(intent);
    }

    public void signout(View v) {
        auth.signOut();
        Intent i = new Intent(this, LoginActivity.class);
        startActivity(i);
        finish();
    }

    void startCameraScan() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setCaptureActivity(CaptureActivity.class);
        integrator.setOrientationLocked(false);
        integrator.setPrompt("Scan a QR or Barcode");
        integrator.initiateScan();
    }

    void selectImageFromGallery() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(pickPhoto);
    }

    void decodeFromGallery(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            if (bitmap == null) {
                Toast.makeText(this, "Image is not valid", Toast.LENGTH_SHORT).show();
                return;
            }

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

            com.google.zxing.RGBLuminanceSource source =
                    new com.google.zxing.RGBLuminanceSource(width, height, pixels);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

            MultiFormatReader reader = new MultiFormatReader();

            Map<com.google.zxing.DecodeHintType, Object> hints = new java.util.HashMap<>();
            hints.put(com.google.zxing.DecodeHintType.TRY_HARDER, Boolean.TRUE);
            hints.put(com.google.zxing.DecodeHintType.POSSIBLE_FORMATS,
                    java.util.Collections.singletonList(com.google.zxing.BarcodeFormat.QR_CODE));
            reader.setHints(hints);

            Result result = reader.decodeWithState(binaryBitmap);

            openResultActivity(result.getText());

        } catch (Exception e) {
            Toast.makeText(this, "Failed to decode QR code", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            openResultActivity(result.getContents());
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    void openResultActivity(String content) {
        String programName = content != null ? content.trim() : "";
        if (programName.isEmpty()) {
            Toast.makeText(this, "Invalid QR content", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this,
                    "You must be logged in to scan.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String uid = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String fullName = doc.getString("fullName");
                    String phone = doc.getString("phone");

                    Map<String, Object> scanData = new HashMap<>();
                    scanData.put("userId", uid);
                    scanData.put("name", fullName != null ? fullName : "");
                    scanData.put("phone", phone != null ? phone : "");
                    scanData.put("programName", programName);
                    // Only scannedAt; programStartTime is stored in `programs` by admin
                    scanData.put("scannedAt", FieldValue.serverTimestamp());

                    String scanId = uid + "_" + programName;

                    db.collection("scans")
                            .document(scanId)
                            .get()
                            .addOnSuccessListener(existing -> {
                                if (existing.exists()) {
                                    Toast.makeText(this,
                                            "You has already scan this program QR before",
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    db.collection("scans")
                                            .document(scanId)
                                            .set(scanData)
                                            .addOnSuccessListener(unused ->
                                                    Toast.makeText(this,
                                                            "Scan saved for " + programName,
                                                            Toast.LENGTH_SHORT).show())
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(this,
                                                            "Failed to save scan: " + e.getMessage(),
                                                            Toast.LENGTH_LONG).show());
                                }
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Error checking scan: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load user profile", Toast.LENGTH_SHORT).show());

        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("result", programName);
        startActivity(intent);
    }
}
