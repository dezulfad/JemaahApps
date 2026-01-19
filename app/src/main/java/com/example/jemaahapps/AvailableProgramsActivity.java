package com.example.jemaahapps;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class AvailableProgramsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private ProgramAdapter adapter;
    private List<Program> programList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_programs);

        recyclerView = findViewById(R.id.recyclerViewPrograms);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        adapter = new ProgramAdapter(programList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadPrograms();
    }

    private void loadPrograms() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        db.collection("programs")
                .whereGreaterThan("programStartTime", new Timestamp(new Date()))  // Filter future only
                .orderBy("programStartTime")  // Order ascending by start time
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    programList.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String id = doc.getId();
                        String name = doc.getString("programName");
                        Timestamp ts = doc.getTimestamp("programStartTime");

                        if (name != null && ts != null) {
                            programList.add(new Program(id, name, ts.toDate()));
                        }
                    }

                    progressBar.setVisibility(View.GONE);

                    if (programList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Failed to load programs: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("Failed to load programs.");
                });
    }


    class ProgramAdapter extends RecyclerView.Adapter<ProgramAdapter.ProgramViewHolder> {

        private final List<Program> programs;
        private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

        ProgramAdapter(List<Program> programs) {
            this.programs = programs;
        }

        @NonNull
        @Override
        public ProgramViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_program, parent, false);
            return new ProgramViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ProgramViewHolder holder, int position) {
            Program program = programs.get(position);
            holder.tvProgramName.setText(program.name);
            holder.tvProgramDate.setText(sdf.format(program.startDate));

            holder.btnJoin.setOnClickListener(v -> joinProgram(program));
        }

        @Override
        public int getItemCount() {
            return programs.size();
        }

        class ProgramViewHolder extends RecyclerView.ViewHolder {

            TextView tvProgramName, tvProgramDate;
            Button btnJoin;

            public ProgramViewHolder(@NonNull View itemView) {
                super(itemView);
                tvProgramName = itemView.findViewById(R.id.tvProgramName);
                tvProgramDate = itemView.findViewById(R.id.tvProgramDate);
                btnJoin = itemView.findViewById(R.id.btnJoin);
            }
        }

        private void joinProgram(Program program) {
            String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
            if (userId == null) {
                Toast.makeText(AvailableProgramsActivity.this,
                        "You must be logged in to join a program",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Check if already joined
            db.collection("scans").document(userId + "_" + program.id)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Toast.makeText(AvailableProgramsActivity.this,
                                    "You have already joined this program",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // Fetch user profile info first
                            db.collection("users").document(userId).get()
                                    .addOnSuccessListener(userDoc -> {
                                        String fullName = userDoc.getString("fullName");
                                        String phone = userDoc.getString("phone");

                                        HashMap<String, Object> scanData = new HashMap<>();
                                        scanData.put("userId", userId);
                                        scanData.put("programName", program.name);
                                        scanData.put("scannedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
                                        scanData.put("name", fullName != null ? fullName : "");
                                        scanData.put("phone", phone != null ? phone : "");

                                        // Save scan/join info with name and phone
                                        db.collection("scans").document(userId + "_" + program.id)
                                                .set(scanData)
                                                .addOnSuccessListener(unused -> Toast.makeText(AvailableProgramsActivity.this,
                                                        "Successfully joined program",
                                                        Toast.LENGTH_SHORT).show())
                                                .addOnFailureListener(e -> Toast.makeText(AvailableProgramsActivity.this,
                                                        "Failed to join program: " + e.getMessage(),
                                                        Toast.LENGTH_LONG).show());
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(AvailableProgramsActivity.this,
                                            "Failed to get user info: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show());
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(AvailableProgramsActivity.this,
                            "Failed to join program: " + e.getMessage(),
                            Toast.LENGTH_LONG).show());
        }
    }

    // Simple data class to hold program info
    static class Program {
        String id;
        String name;
        Date startDate;

        Program(String id, String name, Date startDate) {
            this.id = id;
            this.name = name;
            this.startDate = startDate;
        }
    }
}
