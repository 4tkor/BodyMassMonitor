package com.example.bodymassmonitor;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.view.View;

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

public class RegisterActivity extends AppCompatActivity {
    private static final String LOG_TAG = "myinfo";
    EditText userNameEt;
    EditText userEmailEt;
    EditText userPasswordEt;
    EditText userConfirmPasswordEt;

    private FirebaseAuth  mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        userNameEt = findViewById(R.id.etUsernameRegister);
        userEmailEt = findViewById(R.id.etEmailRegister);
        userPasswordEt = findViewById(R.id.etPasswordRegister);
        userConfirmPasswordEt = findViewById(R.id.etConfirmPassword);

        int secret_key = getIntent().getIntExtra("SECRET_KEY", 0);

        if (secret_key != 666) {
            finish();
        }

        mAuth = FirebaseAuth.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void back(View view) {
        finish();
    }

    public void register(View view) {
        String userName = userNameEt.getText().toString();
        String email = userEmailEt.getText().toString();
        String password= userPasswordEt.getText().toString();
        String passwordConfirm = userConfirmPasswordEt.getText().toString();


        if(userName.isEmpty()){
            userNameEt.setError("Töltsd ki az felhasználónevet");
            userNameEt.requestFocus();
            return;
        }
        if(email.isEmpty()){
            userEmailEt.setError("Töltsd ki az email-t");
            userEmailEt.requestFocus();
            return;
        }
        if(password.isEmpty()){
            userPasswordEt.setError("Töltsd ki a jelszót");
            userPasswordEt.requestFocus();
            return;
        }
        if(passwordConfirm.isEmpty()){
            userConfirmPasswordEt.setError("Töltsd ki a jelszót megerősítését");
            userConfirmPasswordEt.requestFocus();
            return;
        }

        if(!password.equals(passwordConfirm)){
            userPasswordEt.setError("");
            Log.d(LOG_TAG, "PasswordConfirm doesn't match the password");
            userConfirmPasswordEt.setError("A jelszó és megerősítése nem egyezik!");
            userConfirmPasswordEt.requestFocus();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Log.d(LOG_TAG, "user registration succesful");
                    goToHome();

                } else {
                    Log.d(LOG_TAG, "user registration failed: " + task.getException().getMessage());
                    Toast.makeText(RegisterActivity.this, "User registration failed" + task.getException().getMessage(), Toast.LENGTH_LONG).show();
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
        View myView = findViewById(R.id.cardRegister);
        myView.startAnimation(animation);



        try {
            View btn = findViewById(R.id.buttons);
            Animation slide = AnimationUtils.loadAnimation(this, R.anim.slide_in);
            btn.startAnimation(slide);

        }catch (Exception e){
            try{
                View btnBack = findViewById(R.id.btnBack);
                btnBack.startAnimation(animation);

                View btnReg = findViewById(R.id.btnRegister);
                btnReg.startAnimation(animation);


            }catch (Exception ex){}
        }


    }
}