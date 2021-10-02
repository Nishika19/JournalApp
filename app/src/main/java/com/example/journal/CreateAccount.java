package com.example.journal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import util.JournalApi;

public class CreateAccount extends AppCompatActivity {
    private EditText emailAccount, usernameAccount, passwordAccount;
    private ProgressBar progressBar;
    private Button createAccount, intent;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser currentUser;


    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = db.collection("Users");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);
        Objects.requireNonNull(getSupportActionBar()).setElevation(0);

        firebaseAuth = FirebaseAuth.getInstance();

        intent=findViewById(R.id.create_intent);
        emailAccount = findViewById(R.id.create_account_email);
        usernameAccount = findViewById(R.id.create_account_username);
        passwordAccount = findViewById(R.id.create_account_password);
        progressBar = findViewById(R.id.create_account_progress);
        createAccount = findViewById(R.id.create_account_button);

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                currentUser = firebaseAuth.getCurrentUser();

                if(currentUser!=null){
                    //user is already logged in
                }else{
                    //no user yet..
                }
            }
        };

        intent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CreateAccount.this,
                        PostJournalActivity.class);
                startActivity(intent);
            }
        });
        createAccount.setOnClickListener(v -> {
            String email = emailAccount.getText().toString().trim();
            String password = passwordAccount.getText().toString().trim();
            String username = usernameAccount.getText().toString().trim();

            if(!TextUtils.isEmpty(email)&& !TextUtils.isEmpty(password) && !TextUtils.isEmpty(username)) {

                createUserEmailAccount(email, password, username);
            }else{
                Toast.makeText(CreateAccount.this,"Empty fields are not filled",Toast.LENGTH_LONG)
                        .show();
            }
        });

    }

    private void createUserEmailAccount(String email, String password, String username){
        if(!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password) && !TextUtils.isEmpty(username)){
            progressBar.setVisibility(View.VISIBLE);

            firebaseAuth.createUserWithEmailAndPassword(email,password)
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()){
                            currentUser = firebaseAuth.getCurrentUser();
                            assert currentUser != null;
                            String currentUserId = currentUser.getUid();

                            Map<String,String> userObject = new HashMap<>();
                            userObject.put("userId", currentUserId);
                            userObject.put("username",username);

                            collectionReference.add(userObject).addOnSuccessListener(documentReference ->
                                    documentReference.get().addOnCompleteListener(task1 -> {

                                        if(Objects.requireNonNull(task1.getResult()).exists()){
                                            progressBar.setVisibility(View.INVISIBLE);
                                            String name = task1.getResult().getString("username");

                                            JournalApi journalApi = JournalApi.getInstance();
                                            journalApi.setUserId(currentUserId);
                                            journalApi.setUsername(name);

                                            Intent intent = new Intent(CreateAccount.this,
                                                    PostJournalActivity.class);
                                            intent.putExtra("username",name);
                                            intent.putExtra("userId", currentUserId);
                                            startActivity(intent);


                                        }else{
                                            progressBar.setVisibility(View.INVISIBLE);
                                        }
                                    }))
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {

                                        }
                                    });

                        }else{

                        }

                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                        }
                    });

        }
    }


    @Override
    protected void onStart() {
        super.onStart();

        currentUser = firebaseAuth.getCurrentUser();
        firebaseAuth.addAuthStateListener(authStateListener);
    }
}