package com.example.journal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.journal.model.Journal;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import util.JournalApi;

public class PostJournalActivity extends AppCompatActivity
        implements View.OnClickListener{

    private static final int GALLERY_CODE = 1;
    private Button saveButton;
    private ProgressBar progressBar;
    private EditText userName, userAbout;
    private TextView currentUserTextView;
    private ImageView addPhotoButton, imageView;
    private String currentUserId, currentUserName,id;

    private boolean isEdit = false;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser firebaseUser;

    //Connection to Firestore
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private StorageReference storageReference;


    private final CollectionReference collectionReference = db.collection("Journal");
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_journal);

        Objects.requireNonNull(getSupportActionBar()).setElevation(0);

        userName = findViewById(R.id.username_editview);
        userAbout = findViewById(R.id.userAbout_editview);
       if(getIntent().getStringExtra("userId")!=null){
           String userID = getIntent().getStringExtra("userId");
           String name = getIntent().getStringExtra("name");
           String timeAdded = getIntent().getStringExtra("timeCreated");
           String about = getIntent().getStringExtra("about");
           userName.setText(name);
           userAbout.setText(about);
           isEdit = true;
           if(userID!=null) {
               collectionReference.whereEqualTo("userId",userID)
                       .get()
                       .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                           @Override
                           public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                               for (QueryDocumentSnapshot value : queryDocumentSnapshots) {
                                   if(value!=null) {
                                       if (Objects.requireNonNull(value.get("timeAdded")).toString().equals(timeAdded)) {
                                           id = value.getId();
                                       }
                                   }
                               }
                           }
                       });
           }
       }

        firebaseAuth = FirebaseAuth.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();
        imageView = findViewById(R.id.main_imageview);
        progressBar = findViewById(R.id.user_progressbar);

        currentUserTextView = findViewById(R.id.user_textview);
        saveButton = findViewById(R.id.user_save_button);
        saveButton.setOnClickListener(this);
        addPhotoButton = findViewById(R.id.camera_button);
        addPhotoButton.setOnClickListener(this);

        progressBar.setVisibility(View.INVISIBLE);
        if(JournalApi.getInstance()!=null){
            currentUserId = JournalApi.getInstance().getUserId();
            currentUserName = JournalApi.getInstance().getUsername();

            currentUserTextView.setText(currentUserName);
        }

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                firebaseUser = firebaseAuth.getCurrentUser();
                if(firebaseUser!=null){

                }else{

                }
            }
        };
    }

    private void updateJournal() {
        String name = userName.getText().toString().trim();
        String about = userAbout.getText().toString().trim();

          progressBar.setVisibility(View.VISIBLE);
            if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(about) && imageUri != null) {
                StorageReference filepath = storageReference
                        .child("Journal image")
                        .child("imageAdded" + Timestamp.now().getSeconds());

                filepath.putFile(imageUri)
                        .addOnSuccessListener(taskSnapshot -> filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {

                                String imageUri = uri.toString();

                                Map<String, Object> data = new HashMap<>();
                                data.put("name", name);
                                data.put("about", about);
                                data.put("imageUrl", imageUri);
                                data.put("timeAdded", new Timestamp(new Date()));

                                collectionReference.document(id).update(data)
                                        .addOnSuccessListener(documentReference -> {
                                            progressBar.setVisibility(View.INVISIBLE);
                                            Toast.makeText(PostJournalActivity.this, "Updated", Toast.LENGTH_SHORT)
                                                    .show();
                                            startActivity(new Intent(PostJournalActivity.this,
                                                    JournalListActivity.class));
                                            finish();
                                        })
                                        .addOnFailureListener(e -> Log.d("TAG", "onFailure: " + e.getMessage()));
                            }
                        }))

                        .addOnFailureListener(e ->
                                progressBar.setVisibility(View.INVISIBLE));
            } else {
                progressBar.setVisibility(View.INVISIBLE);
            }
        }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.user_save_button:
                if(!isEdit) {
                    saveJournal();
                }else{
                    updateJournal();
                }
                break;

            case R.id.camera_button:

                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent,GALLERY_CODE);
                break;
        }
    }

    private void saveJournal() {
        String name = userName.getText().toString().trim();
        String about = userAbout.getText().toString().trim();

        progressBar.setVisibility(View.VISIBLE);

        if(!TextUtils.isEmpty(name) && !TextUtils.isEmpty(about) && imageUri!=null){

            StorageReference filepath = storageReference
                    .child("Journal image")
                    .child("imageAdded"+ Timestamp.now().getSeconds());

            filepath.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {

                            String imageUri = uri.toString();

                            Journal journal = new Journal(name,about,imageUri,currentUserId,
                                    currentUserName,new Timestamp(new Date()));
                            collectionReference.add(journal)
                                    .addOnSuccessListener(documentReference -> {

                                        progressBar.setVisibility(View.INVISIBLE);
                                        startActivity(new Intent(PostJournalActivity.this,
                                                JournalListActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> Log.d("TAG", "onFailure: "+e.getMessage()));
                        }
                    }))

                    .addOnFailureListener(e -> progressBar.setVisibility(View.INVISIBLE));
        }else{
                progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==GALLERY_CODE && resultCode==RESULT_OK){
            if(data!=null){
                imageUri = data.getData();
                imageView.setImageURI(imageUri);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseUser = firebaseAuth.getCurrentUser();
        firebaseAuth.addAuthStateListener(authStateListener);

    }

    @Override
    protected void onStop() {
        super.onStop();
        if(firebaseAuth!=null){
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }
}