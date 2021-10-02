package com.example.journal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.journal.adapter.RecyclerViewAdapter;
import com.example.journal.model.Journal;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import util.JournalApi;

public class JournalListActivity extends AppCompatActivity implements RecyclerViewAdapter.onCardClickListener  {
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser users;

    private String id;
    private Button delete,update;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private StorageReference storageReference;
    private List<Journal> journalList;
    private RecyclerView recyclerView;
    private RecyclerViewAdapter recyclerViewAdapter;
    private TextView noJournalAdded;
    Dialog dialog;

    private CollectionReference collectionReference = db.collection("Journal");


    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal_list);

        Objects.requireNonNull(getSupportActionBar()).setElevation(0);

        dialog = new Dialog(JournalListActivity.this);
        dialog.setContentView(R.layout.dialog);
        dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.background));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);

        delete = dialog.findViewById(R.id.delete);
        update = dialog.findViewById(R.id.update);

        journalList = new ArrayList<>();
        noJournalAdded = findViewById(R.id.no_journal_textview);
        firebaseAuth = FirebaseAuth.getInstance();
        users = firebaseAuth.getCurrentUser();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){
            case R.id.action_add:

                if(users!=null && firebaseAuth!=null){
                    startActivity(new Intent(JournalListActivity.this,PostJournalActivity.class));
                }
                break;

            case R.id.action_signout:
                if(users!=null && firebaseAuth!=null){
                    firebaseAuth.signOut();
                    startActivity(new Intent(JournalListActivity.this,LoginActivity.class));
                }
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        createList();
    }

    private void createList() {
        collectionReference.whereEqualTo("userId", JournalApi.getInstance().getUserId())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if(!queryDocumentSnapshots.isEmpty()){
                            for(QueryDocumentSnapshot journals: queryDocumentSnapshots){
                                Journal journal = journals.toObject(Journal.class);
                                journalList.add(journal);
                            }
                            recyclerViewAdapter = new RecyclerViewAdapter(JournalListActivity.this,journalList,JournalListActivity.this::onCardClick);
                            recyclerView.setAdapter(recyclerViewAdapter);
                            recyclerViewAdapter.notifyDataSetChanged();
                        }else{
                            noJournalAdded.setVisibility(View.VISIBLE);
                        }
                    }

                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
        journalList.removeAll(journalList);
    }


    @Override
    public void onCardClick(int position) {
        dialog.show();

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Journal journal = journalList.get(position);
                String timeAdded = journal.getTimeAdded().toString();
                String userId = journal.getUserId();
                collectionReference.whereEqualTo("userId",userId)
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                for (QueryDocumentSnapshot value : queryDocumentSnapshots) {
                                    if(value!=null) {
                                         Log.d("TAG1", "onSuccess: "+timeAdded);
                                          Log.d("TAG2", "onSuccess: "+value.get("timeAdded"));
                                        if (Objects.requireNonNull(value.get("timeAdded")).toString().equals(timeAdded)) {
                                            id = value.getId();

                                            if(id!=null) {
                                                collectionReference.document(id).delete();
                                                Toast.makeText(JournalListActivity.this,"Deleted",Toast.LENGTH_LONG)
                                                        .show();
                                                recyclerViewAdapter.notifyDataSetChanged();
                                                dialog.dismiss();
                                                createList();
                                            }
                                        }
                                    }
                                }
                            }
                        });
                Log.d("Delete journal", "onClick: "+id);
                //Map<String,Object> data =new HashMap<>();
                //data.put("name",FieldValue.delete());


//                collectionReference.document(id).update("name", FieldValue.delete());

            }
        });

        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Journal journal = journalList.get(position);
                String time = journal.getTimeAdded().toString();
                Intent intent =new Intent(JournalListActivity.this,PostJournalActivity.class);
                intent.putExtra("name",journal.getName());
                intent.putExtra("about",journal.getAbout());
                intent.putExtra("timeCreated",time);
                intent.putExtra("userId",journal.getUserId());
                startActivity(intent);
            }
        });

    }
}