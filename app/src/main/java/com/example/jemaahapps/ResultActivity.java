package com.example.jemaahapps;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * ResultActivity displays the scanned result (QR/barcode content) from MainActivity.
 * If the result is a valid URL, the user can open it in a browser.
 * Also provides a button to return back to MainActivity.
 * Additionally, when the QR encodes a program name, this activity:
 *  - loads the program from Firestore (/programs/{programName})
 *  - ensures the user can only join once per program
 *  - schedules a reminder 60 minutes before programStartTime.
 */
public class ResultActivity extends AppCompatActivity {

    TextView resultText;         // Displays scanned result content
    Button openBrowserBtn;       // Button to open content in browser if it’s a valid URL
    Button backBtn;              // Button to go back to MainActivity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        resultText = findViewById(R.id.resultText);
        //openBrowserBtn = findViewById(R.id.openBrowserBtn);
        backBtn = findViewById(R.id.backBtn);

        // Get the scanned result passed from MainActivity
        String contentRaw = getIntent().getStringExtra("result");
        String content = contentRaw != null ? contentRaw.trim() : "";

        // Display the scanned content
        resultText.setText("You have successfully joined " + content);

        Toast.makeText(this, "QR content: " + content, Toast.LENGTH_LONG).show();

        // Assume QR content is a programName (and document ID == programName)
        loadProgramAndHandleJoin(content);

        backBtn.setOnClickListener(v -> finish());
    }

    /**
     * Loads program document from Firestore using the scanned content as programName
     * then enforces one-join-per-user and schedules a reminder.
     */
    private void loadProgramAndHandleJoin(String programNameFromQR) {
        if (programNameFromQR == null || programNameFromQR.isEmpty()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("programs")
                .document(programNameFromQR)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        handleProgramDocument(programNameFromQR, doc);
                    } else {
                        Toast.makeText(this,
                                "No program found for: " + programNameFromQR,
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error loading program: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void handleProgramDocument(String programName, DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return;

        Timestamp ts = doc.getTimestamp("programStartTime");

        Toast.makeText(this, "Loaded " + programName, Toast.LENGTH_SHORT).show();

        if (ts == null) {
            Toast.makeText(this,
                    "Program time not set for " + programName,
                    Toast.LENGTH_LONG).show();
            return;
        }

        long startMillis = ts.toDate().getTime();

        // Enforce: user can join only once per program
        joinProgramOnce(programName, startMillis);
    }

    /**
     * Creates one attendance document per user+program (userUid_programName).
     * If it already exists, user cannot join again and no new reminder is scheduled.
     */
    private void joinProgramOnce(String programName, long startMillis) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this,
                    "You must be logged in to join the program.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        String attendanceId = uid + "_" + programName;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference ref = db.collection("attendance").document(attendanceId);

        ref.get().addOnSuccessListener(existing -> {
            if (existing.exists()) {
                // Already joined
                Toast.makeText(this,
                        "You has already scan this program QR before",
                        Toast.LENGTH_LONG).show();
            } else {
                // First time: create attendance + schedule reminder
                Map<String, Object> data = new HashMap<>();
                data.put("userId", uid);
                data.put("programName", programName);
                data.put("joinedAt", Timestamp.now());

                ref.set(data)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(this,
                                    "Joined " + programName,
                                    Toast.LENGTH_SHORT).show();
                            scheduleProgramReminder(this, programName, startMillis);
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this,
                                        "Failed to join: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show());
            }
        });
    }

    private void scheduleProgramReminder(Context context, String programName, long startMillis) {
        // 60 minutes before programStartTime
        long triggerAtMillis = startMillis - 60 * 60 * 1000L;

        if (triggerAtMillis <= System.currentTimeMillis()) {
            // already less than 60 minutes before start
            Toast.makeText(context,
                    "Program is starting soon; no 60-minute reminder scheduled.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("msg",
                "Your program \"" + programName + "\" begins in 60 minutes.");

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
