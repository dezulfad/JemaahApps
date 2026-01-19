package com.example.jemaahapps;

import android.Manifest;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.CaptureActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 101;

    FirebaseAuth auth;
    FirebaseUser user;
    TextView profileText, tvUpcomingProgram;
    Button openMap, cameraBtn, galleryBtn, btnSetReminder;
    ImageView profileImage;

    // Firestore listener for scans
    private ListenerRegistration scansListener;

    ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            decodeFromGallery(result.getData().getData());
                        }
                    });

    // Fields for upcoming program calculation
    private String nextProgramName = null;
    private Date nextProgramDate = null;
    private int processedCount = 0;
    private int totalPrograms = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        auth = FirebaseAuth.getInstance();

        profileText = findViewById(R.id.textView);
        tvUpcomingProgram = findViewById(R.id.tvUpcomingProgram);
        openMap = findViewById(R.id.button);
        cameraBtn = findViewById(R.id.cameraBtn);
        galleryBtn = findViewById(R.id.galleryBtn);
        btnSetReminder = findViewById(R.id.btnSetReminder);
        profileImage = findViewById(R.id.profileImage);
        Button btnViewPrograms = findViewById(R.id.btnViewPrograms);
        btnViewPrograms.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, AvailableProgramsActivity.class);
            startActivity(intent);
        });

        // Open MapsActivity on map button click
        openMap.setOnClickListener(v -> openMap(v));

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_home);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true; // Already home
            } else if (id == R.id.nav_scan) {
                startCameraScan();
                return true;
            } else if (id == R.id.nav_nearby) {
                openMap(null);
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, EditProfileActivity.class));
                return true;
            }
            return false;
        });

        // Load profile image for current user
        loadProfileImage();

        profileImage.setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class)));

        user = auth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        String fullName = doc.getString("fullName");
                        profileText.setText(fullName != null ? fullName : user.getEmail());
                    });

            loadUpcomingProgram(uid); // <-- LIVE listener here
        }

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_CODE_POST_NOTIFICATIONS);
        }

        cameraBtn.setOnClickListener(v -> startCameraScan());
        galleryBtn.setOnClickListener(v -> selectImageFromGallery());

        btnSetReminder.setOnClickListener(v -> {
            String program = tvUpcomingProgram.getText().toString();
            if ("No upcoming program".equals(program)) {
                Toast.makeText(this, "No upcoming program", Toast.LENGTH_SHORT).show();
            } else {
                showDateTimePickerDialog(program);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileImage();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scansListener != null) {
            scansListener.remove();
        }
    }

    private void loadProfileImage() {
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        String encodedImage = getSharedPreferences("user_profile", MODE_PRIVATE)
                .getString("profile_image_" + uid, null);
        if (encodedImage != null) {
            byte[] decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT);
            profileImage.setImageBitmap(
                    BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length));
        }
    }

    private void loadUpcomingProgram(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        nextProgramName = null;
        nextProgramDate = null;
        processedCount = 0;

        if (scansListener != null) {
            scansListener.remove();
        }

        scansListener = db.collection("scans")
                .whereEqualTo("userId", uid)
                .addSnapshotListener((scanSnap, error) -> {
                    if (error != null) {
                        tvUpcomingProgram.setText("Failed to load programs");
                        btnSetReminder.setVisibility(View.GONE);
                        return;
                    }

                    if (scanSnap == null || scanSnap.isEmpty()) {
                        tvUpcomingProgram.setText("No Upcoming Program For Now");
                        btnSetReminder.setVisibility(View.GONE);
                        return;
                    }

                    nextProgramName = null;
                    nextProgramDate = null;
                    processedCount = 0;
                    totalPrograms = scanSnap.size();

                    Date now = new Date();

                    for (var doc : scanSnap) {
                        String programName = doc.getString("programName");
                        if (programName == null) {
                            processedCount++;
                            if (processedCount == totalPrograms) finishDisplay();
                            continue;
                        }

                        db.collection("programs").document(programName).get()
                                .addOnSuccessListener(programDoc -> {
                                    Timestamp ts = programDoc.getTimestamp("programStartTime");
                                    if (ts != null) {
                                        Date programDate = ts.toDate();
                                        if (programDate.after(now) &&
                                                (nextProgramDate == null || programDate.before(nextProgramDate))) {
                                            nextProgramDate = programDate;
                                            nextProgramName = programName;
                                        }
                                    }
                                    processedCount++;
                                    if (processedCount == totalPrograms) finishDisplay();
                                })
                                .addOnFailureListener(e -> {
                                    processedCount++;
                                    if (processedCount == totalPrograms) finishDisplay();
                                });
                    }
                });
    }

    private void finishDisplay() {
        if (nextProgramName != null && nextProgramDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            tvUpcomingProgram.setText(nextProgramName + "\n" + sdf.format(nextProgramDate));
            btnSetReminder.setVisibility(View.VISIBLE);
        } else {
            tvUpcomingProgram.setText("No Upcoming Program For Now");
            btnSetReminder.setVisibility(View.GONE);
        }
    }

    public void openMap(View view) {
        startActivity(new Intent(this, MapsActivity.class));
    }

    public void signout(View v) {
        auth.signOut();
        startActivity(new Intent(this, LoginActivity.class));
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
        galleryLauncher.launch(new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
    }

    void decodeFromGallery(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

            com.google.zxing.RGBLuminanceSource source =
                    new com.google.zxing.RGBLuminanceSource(width, height, pixels);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

            Result result = new MultiFormatReader().decode(binaryBitmap);
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

    void openResultActivity(String programName) {
        String trimmedProgramName = programName != null ? programName.trim() : "";
        if (trimmedProgramName.isEmpty()) return;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Fetch user info before saving scan
        db.collection("users").document(uid).get()
                .addOnSuccessListener(userDoc -> {
                    String fullName = userDoc.getString("fullName");
                    String phone = userDoc.getString("phone");

                    Map<String, Object> scanData = new HashMap<>();
                    scanData.put("userId", uid);
                    scanData.put("programName", trimmedProgramName);
                    scanData.put("scannedAt", FieldValue.serverTimestamp());
                    scanData.put("name", fullName != null ? fullName : "");
                    scanData.put("phone", phone != null ? phone : "");

                    db.collection("scans").document(uid + "_" + trimmedProgramName).set(scanData)
                            .addOnSuccessListener(unused -> {
                                Intent intent = new Intent(this, ResultActivity.class);
                                intent.putExtra("result", trimmedProgramName);
                                startActivity(intent);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to save scan: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to get user info: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showDateTimePickerDialog(String programName) {
        Calendar now = Calendar.getInstance();
        Calendar selected = Calendar.getInstance();

        new DatePickerDialog(this, (v, y, m, d) -> {
            selected.set(y, m, d);
            new TimePickerDialog(this, (TimePicker tp, int h, int min) -> {
                selected.set(Calendar.HOUR_OF_DAY, h);
                selected.set(Calendar.MINUTE, min);
                scheduleAlarm(selected, programName);
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show();
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void scheduleAlarm(Calendar calendar, String programName) {
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("programName", programName);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, programName.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(), pendingIntent);
            Toast.makeText(this, "Reminder set", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
