package com.example.jemaahapps;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdminActivity extends AppCompatActivity {

    private RecyclerView rvScans;
    private FirebaseFirestore db;
    private ListenerRegistration scansListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.adminRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rvScans = findViewById(R.id.rvScans);
        rvScans.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();

        // Button that opens AdminProgramActivity to add/manage programs
        Button btnManagePrograms = findViewById(R.id.btnManagePrograms);
        btnManagePrograms.setOnClickListener(v -> {
            Intent i = new Intent(AdminActivity.this, AdminProgramActivity.class);
            startActivity(i);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        startListeningForScans();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (scansListener != null) {
            scansListener.remove();
            scansListener = null;
        }
    }

    private void startListeningForScans() {
        // remove previous listener if any
        if (scansListener != null) {
            scansListener.remove();
        }

        scansListener = db.collection("scans")
                .orderBy("programName")
                .addSnapshotListener(this, (querySnapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(this,
                                "Failed to load scans: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (querySnapshot == null) return;

                    // Step 1: convert docs to ScanItem list
                    List<ScanItem> items = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String name = doc.getString("name");
                        String phone = doc.getString("phone");
                        String programName = doc.getString("programName");

                        items.add(new ScanItem(
                                name != null ? name : "",
                                phone != null ? phone : "",
                                programName != null ? programName : "Unknown"
                        ));
                    }

                    // Step 2: group by programName
                    Map<String, List<ScanItem>> grouped = new LinkedHashMap<>();
                    for (ScanItem item : items) {
                        String key = item.getProgramName();
                        if (!grouped.containsKey(key)) {
                            grouped.put(key, new ArrayList<>());
                        }
                        grouped.get(key).add(item);
                    }

                    // Step 3: build rows (header + items)
                    List<ScanRow> rows = new ArrayList<>();
                    for (Map.Entry<String, List<ScanItem>> entry : grouped.entrySet()) {
                        String program = entry.getKey();
                        int count = entry.getValue().size();

                        // Build header label: "Program A - 10 people"
                        String headerLabel;
                        if (count == 1) {
                            headerLabel = program + " - 1 person";
                        } else {
                            headerLabel = program + " - " + count + " people";
                        }

                        rows.add(ScanRow.header(headerLabel)); // header row with count

                        for (ScanItem si : entry.getValue()) {
                            rows.add(ScanRow.item(si.getName(), si.getPhone())); // child rows
                        }
                    }

                    rvScans.setAdapter(new ScanGroupedAdapter(rows));
                });
    }

    public void logoutAdmin(View v) {
        // stop listening first
        if (scansListener != null) {
            scansListener.remove();
            scansListener = null;
        }

        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(AdminActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
