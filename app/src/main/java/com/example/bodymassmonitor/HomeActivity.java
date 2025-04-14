package com.example.bodymassmonitor;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;


public class HomeActivity extends AppCompatActivity {

    private static final String LOG_TAG = "myinfo";
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null){
            Log.d(LOG_TAG,"Authenticated user");
        }else{
            Log.d(LOG_TAG,"Unauthenticated user");
            finish();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void logout(View view) {
        Log.d(LOG_TAG, "Logout clicked!");
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

    }

    @Override
    protected void onStart() {
        super.onStart();
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        View myView = findViewById(R.id.todo);
        myView.startAnimation(animation);
    }

}