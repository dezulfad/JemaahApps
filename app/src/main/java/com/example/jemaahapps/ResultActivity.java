package com.example.jemaahapps;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DateFormat;
import java.util.HashMap;
import java.util.Map;

public class ResultActivity extends AppCompatActivity {

    TextView resultText;
    Button backBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        resultText = findViewById(R.id.resultText);
        backBtn = findViewById(R.id.backBtn);

        String contentRaw = getIntent().getStringExtra("result");
        String programName = contentRaw != null ? contentRaw.trim() : "";

        if (programName.isEmpty()) {
            Toast.makeText(this, "Invalid QR code", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        resultText.setText("Scanning program: " + programName);

        loadProgramAndHandleJoin(programName);

        backBtn.setOnClickListener(v -> finish());
    }

    /**
     * Load program document using program name from QR
     */
    private void loadProgramAndHandleJoin(String programName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("programs")
                .document(programName)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        handleProgramDocument(programName, doc);
                    } else {
                        Toast.makeText(this,
                                "No program found for: " + programName,
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error loading program: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    /**
     * Validate program time and block past programs
     */
    private void handleProgramDocument(String programName, DocumentSnapshot doc) {
        Timestamp ts = doc.getTimestamp("programStartTime");

        if (ts == null) {
            Toast.makeText(this,
                    "Program time not set for " + programName,
                    Toast.LENGTH_LONG).show();
            return;
        }

        long startMillis = ts.toDate().getTime();
        long now = System.currentTimeMillis();

        // 🚫 BLOCK PAST PROGRAMS
        if (startMillis < now) {
            String endedAt = DateFormat.getDateTimeInstance().format(ts.toDate());
            Toast.makeText(this,
                    "This program has already ended.\nEnded on: " + endedAt,
                    Toast.LENGTH_LONG).show();
            return;
        }

        // ✅ Program is valid
        joinProgramOnce(programName, startMillis);
    }

    /**
     * Ensure user joins only once per program
     */
    private void joinProgramOnce(String programName, long startMillis) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this,
                    "You must be logged in to join a program.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        String attendanceId = uid + "_" + programName;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference ref = db.collection("attendance").document(attendanceId);

        ref.get().addOnSuccessListener(existing -> {
            if (existing.exists()) {
                Toast.makeText(this,
                        "You have already joined this program.",
                        Toast.LENGTH_LONG).show();
            } else {
                Map<String, Object> data = new HashMap<>();
                data.put("userId", uid);
                data.put("programName", programName);
                data.put("joinedAt", Timestamp.now());

                ref.set(data)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(this,
                                    "Successfully joined " + programName,
                                    Toast.LENGTH_SHORT).show();
                            scheduleProgramReminder(this, programName, startMillis);
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this,
                                        "Failed to join program.",
                                        Toast.LENGTH_LONG).show());
            }
        });
    }

    /**
     * Schedule reminder 60 minutes before program start
     */
    private void scheduleProgramReminder(Context context, String programName, long startMillis) {
        long triggerAtMillis = startMillis - 60 * 60 * 1000L;

        if (triggerAtMillis <= System.currentTimeMillis()) {
            Toast.makeText(context,
                    "Program starts soon. No reminder scheduled.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("programName", programName);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                programName.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
        }
    }
}