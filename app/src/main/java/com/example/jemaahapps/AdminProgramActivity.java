package com.example.jemaahapps;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminProgramActivity extends AppCompatActivity {

    private EditText etProgramName, etProgramDate, etProgramTime;
    private Button btnSave;

    private FirebaseFirestore db;
    private String existingProgramId = null;

    // Calendar to hold selected date+time
    private final Calendar selectedCal = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_program);

        db = FirebaseFirestore.getInstance();

        etProgramName = findViewById(R.id.etProgramName);
        etProgramDate = findViewById(R.id.etProgramDate);
        etProgramTime = findViewById(R.id.etProgramTime);
        btnSave = findViewById(R.id.btnSaveProgram);

        existingProgramId = getIntent().getStringExtra("programId");
        if (existingProgramId != null) {
            loadProgramForEdit(existingProgramId);
        }

        etProgramDate.setOnClickListener(v -> showDatePicker());
        etProgramTime.setOnClickListener(v -> showTimePicker());

        btnSave.setOnClickListener(v -> saveProgram());
    }

    private void showDatePicker() {
        int year = selectedCal.get(Calendar.YEAR);
        int month = selectedCal.get(Calendar.MONTH);
        int day = selectedCal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dp = new DatePickerDialog(
                this,
                (view, y, m, d) -> {
                    selectedCal.set(Calendar.YEAR, y);
                    selectedCal.set(Calendar.MONTH, m);
                    selectedCal.set(Calendar.DAY_OF_MONTH, d);
                    etProgramDate.setText(String.format(Locale.getDefault(),
                            "%02d/%02d/%04d", d, m + 1, y));
                },
                year, month, day
        );
        dp.show();
    }

    private void showTimePicker() {
        int hour = selectedCal.get(Calendar.HOUR_OF_DAY);
        int minute = selectedCal.get(Calendar.MINUTE);

        TimePickerDialog tp = new TimePickerDialog(
                this,
                (view, h, m) -> {
                    selectedCal.set(Calendar.HOUR_OF_DAY, h);
                    selectedCal.set(Calendar.MINUTE, m);
                    selectedCal.set(Calendar.SECOND, 0);
                    etProgramTime.setText(String.format(Locale.getDefault(),
                            "%02d:%02d", h, m));
                },
                hour, minute, true
        );
        tp.show();
    }

    private void saveProgram() {
        String programName = etProgramName.getText().toString().trim();

        if (programName.isEmpty()) {
            Toast.makeText(this, "Program name is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (etProgramDate.getText().toString().trim().isEmpty()
                || etProgramTime.getText().toString().trim().isEmpty()) {
            Toast.makeText(this,
                    "Please select date and time", Toast.LENGTH_SHORT).show();
            return;
        }

        Timestamp programStartTime = new Timestamp(selectedCal.getTime());

        if (existingProgramId == null) {
            addProgram(programName, programStartTime);
        } else {
            updateProgram(existingProgramId, programName, programStartTime);
        }
    }

    private void addProgram(String programName, Timestamp programStartTime) {
        String programId = programName.trim(); // QR uses this ID

        Map<String, Object> data = new HashMap<>();
        data.put("programName", programName.trim());
        data.put("programStartTime", programStartTime);

        db.collection("programs")
                .document(programId)
                .set(data)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this,
                            "Program added successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to add program: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void updateProgram(String programId,
                               String programName,
                               Timestamp programStartTime) {

        Map<String, Object> updates = new HashMap<>();
        updates.put("programName", programName.trim());
        updates.put("programStartTime", programStartTime);

        db.collection("programs")
                .document(programId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this,
                            "Program updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to update program: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void loadProgramForEdit(String programId) {
        db.collection("programs")
                .document(programId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("programName");
                        Timestamp ts = doc.getTimestamp("programStartTime");

                        etProgramName.setText(name != null ? name : "");

                        if (ts != null) {
                            selectedCal.setTime(ts.toDate());
                            int y = selectedCal.get(Calendar.YEAR);
                            int m = selectedCal.get(Calendar.MONTH);
                            int d = selectedCal.get(Calendar.DAY_OF_MONTH);
                            int h = selectedCal.get(Calendar.HOUR_OF_DAY);
                            int min = selectedCal.get(Calendar.MINUTE);

                            etProgramDate.setText(String.format(Locale.getDefault(),
                                    "%02d/%02d/%04d", d, m + 1, y));
                            etProgramTime.setText(String.format(Locale.getDefault(),
                                    "%02d:%02d", h, min));
                        }
                    } else {
                        Toast.makeText(this,
                                "Program not found for edit",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load program: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }
}
