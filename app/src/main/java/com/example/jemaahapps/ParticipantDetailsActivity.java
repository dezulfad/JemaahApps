package com.example.jemaahapps;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ParticipantDetailsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty, tvProgramTitle;

    private FirebaseFirestore db;

    private ParticipantAdapter adapter;
    private List<Participant> participantList = new ArrayList<>();

    private String programId;
    private String programName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_participant_details);

        recyclerView = findViewById(R.id.recyclerViewParticipants);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvProgramTitle = findViewById(R.id.tvProgramTitle);

        db = FirebaseFirestore.getInstance();

        adapter = new ParticipantAdapter(participantList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        programId = getIntent().getStringExtra("programId");
        programName = getIntent().getStringExtra("programName");

        if (programName != null) {
            tvProgramTitle.setText("Participants for: \n" + programName);
        }

        if (programId != null && programName != null) {
            loadParticipants();
        } else {
            Toast.makeText(this, "Invalid program data", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadParticipants() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        // Query scans collection where programName matches (or use programId if you store it)
        db.collection("scans")
                .whereEqualTo("programName", programName)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    participantList.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String name = doc.getString("name");
                        String phone = doc.getString("phone");

                        if (name != null) {
                            participantList.add(new Participant(name, phone != null ? phone : ""));
                        }
                    }

                    progressBar.setVisibility(View.GONE);

                    if (participantList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText("No participants have joined this program yet.");
                    } else {
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("Failed to load participants: " + e.getMessage());
                });
    }

    class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.ParticipantViewHolder> {

        private final List<Participant> participants;

        ParticipantAdapter(List<Participant> participants) {
            this.participants = participants;
        }

        @NonNull
        @Override
        public ParticipantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_participant, parent, false);
            return new ParticipantViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ParticipantViewHolder holder, int position) {
            Participant participant = participants.get(position);
            holder.tvName.setText(participant.name);
            holder.tvPhone.setText(participant.phone.isEmpty() ? "No phone" : participant.phone);
        }

        @Override
        public int getItemCount() {
            return participants.size();
        }

        class ParticipantViewHolder extends RecyclerView.ViewHolder {

            TextView tvName, tvPhone;

            ParticipantViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvParticipantName);
                tvPhone = itemView.findViewById(R.id.tvParticipantPhone);
            }
        }
    }

    static class Participant {
        String name;
        String phone;

        Participant(String name, String phone) {
            this.name = name;
            this.phone = phone;
        }
    }
}
