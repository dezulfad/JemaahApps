package com.example.jemaahapps;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminUpcomingProgramsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private FirebaseFirestore db;

    private ProgramAdapter adapter;
    private List<ProgramWithCount> programList = new ArrayList<>();

    private Map<String, ListenerRegistration> participantCountListeners = new HashMap<>();

    private SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_upcoming_programs);

        recyclerView = findViewById(R.id.recyclerViewPrograms);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        db = FirebaseFirestore.getInstance();

        adapter = new ProgramAdapter(programList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadProgramsWithLiveParticipantCount();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove all Firestore listeners when activity is destroyed
        for (ListenerRegistration listener : participantCountListeners.values()) {
            listener.remove();
        }
        participantCountListeners.clear();
    }

    private void loadProgramsWithLiveParticipantCount() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        db.collection("programs")
                .whereGreaterThan("programStartTime", new Timestamp(new Date()))
                .orderBy("programStartTime")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            progressBar.setVisibility(View.GONE);
                            tvEmpty.setVisibility(View.VISIBLE);
                            tvEmpty.setText("Failed to load programs: " + error.getMessage());
                            return;
                        }
                        if (value == null || value.isEmpty()) {
                            progressBar.setVisibility(View.GONE);
                            tvEmpty.setVisibility(View.VISIBLE);
                            tvEmpty.setText("No upcoming programs");
                            programList.clear();
                            adapter.notifyDataSetChanged();
                            return;
                        }

                        // Clear old data and listeners
                        programList.clear();
                        for (ListenerRegistration listener : participantCountListeners.values()) {
                            listener.remove();
                        }
                        participantCountListeners.clear();

                        for (QueryDocumentSnapshot doc : value) {
                            String id = doc.getId();
                            String name = doc.getString("programName");
                            Timestamp ts = doc.getTimestamp("programStartTime");

                            if (name != null && ts != null) {
                                programList.add(new ProgramWithCount(id, name, ts.toDate(), 0));
                            }
                        }

                        adapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);

                        // Setup live listeners for participant counts
                        setupParticipantCountListeners();
                    }
                });
    }

    private void setupParticipantCountListeners() {
        for (ProgramWithCount program : programList) {
            ListenerRegistration listener = db.collection("scans")
                    .whereEqualTo("programName", program.name)
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException error) {
                            if (error != null) {
                                Toast.makeText(AdminUpcomingProgramsActivity.this,
                                        "Failed to update participant count: " + error.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (snapshots != null) {
                                program.participantCount = snapshots.size();
                                adapter.notifyDataSetChanged();
                            }
                        }
                    });
            participantCountListeners.put(program.id, listener);
        }
    }

    class ProgramAdapter extends RecyclerView.Adapter<ProgramAdapter.ProgramViewHolder> {

        private final List<ProgramWithCount> programs;

        ProgramAdapter(List<ProgramWithCount> programs) {
            this.programs = programs;
        }

        @NonNull
        @Override
        public ProgramViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_program, parent, false);
            return new ProgramViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ProgramViewHolder holder, int position) {
            ProgramWithCount program = programs.get(position);
            holder.tvProgramName.setText(program.name);
            holder.tvProgramDate.setText(sdf.format(program.startDate));
            holder.tvParticipantCount.setText("Participants: " + program.participantCount);

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(AdminUpcomingProgramsActivity.this, ParticipantDetailsActivity.class);
                intent.putExtra("programId", program.id);
                intent.putExtra("programName", program.name);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return programs.size();
        }

        class ProgramViewHolder extends RecyclerView.ViewHolder {

            TextView tvProgramName, tvProgramDate, tvParticipantCount;

            public ProgramViewHolder(@NonNull View itemView) {
                super(itemView);
                tvProgramName = itemView.findViewById(R.id.tvProgramName);
                tvProgramDate = itemView.findViewById(R.id.tvProgramDate);
                tvParticipantCount = itemView.findViewById(R.id.tvParticipantCount);
            }
        }
    }

    static class ProgramWithCount {
        String id;
        String name;
        Date startDate;
        int participantCount;

        ProgramWithCount(String id, String name, Date startDate, int participantCount) {
            this.id = id;
            this.name = name;
            this.startDate = startDate;
            this.participantCount = participantCount;
        }
    }
}
