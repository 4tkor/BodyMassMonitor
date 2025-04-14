package com.example.bodymassmonitor;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import android.content.Intent;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "myinfo";
    private static final int SECRET_KEY = 666;
    private FirebaseAuth mAuth;
    EditText emailET;
    EditText userPasswordEt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);


        emailET = findViewById(R.id.etEmail);
        userPasswordEt= findViewById(R.id.etPassword);
        Log.i(LOG_TAG,"Hi");
        mAuth = FirebaseAuth.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void registration(View view) {
        Intent intent = new Intent(this, RegisterActivity.class);
        intent.putExtra("SECRET_KEY", SECRET_KEY);
        startActivity(intent);

    }

    public void login(View view) {
        String email = emailET.getText().toString();
        String password = userPasswordEt.getText().toString();

        if(email.isEmpty()){
            emailET.setError("Töltsd ki az email-t");
            emailET.requestFocus();
            return;
        }
        if(password.isEmpty()){
            userPasswordEt.setError("Töltsd ki a jelszót");
            userPasswordEt.requestFocus();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    goToHome();
                }else{
                    Log.d(LOG_TAG, "user login failed: " + task.getException().getMessage());
                    Toast.makeText(MainActivity.this, "User Login Failed", Toast.LENGTH_LONG).show();
                }
            }
        });

    }
    private void goToHome(){
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.animation);
        View myView = findViewById(R.id.cardLogin);
        myView.startAnimation(animation);
    }
}