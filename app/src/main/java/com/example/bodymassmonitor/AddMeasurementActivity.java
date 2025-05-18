package com.example.bodymassmonitor;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;
import java.util.Date;

public class AddMeasurementActivity extends AppCompatActivity {

    private TextInputEditText etDate, etWeight, etBmi, etFat, etMuscle;
    private TextInputLayout weightLayout, bmiLayout, fatLayout, muscleLayout;

    private final FirestoreRepository repo = new FirestoreRepository();
    private Date selectedDate = new Date();   // alapértelmezés ma

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        Log.d("alma", "Current user: " + (user == null ? "null" : user.getUid()));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_measurement);

        initViews();
        initDatePicker();
        findViewById(R.id.btnSave).setOnClickListener(v -> onSaveClicked());
    }

    private void initViews() {
        etDate   = findViewById(R.id.etDate);
        etWeight = findViewById(R.id.etWeight);
        etBmi    = findViewById(R.id.etBmi);
        etFat    = findViewById(R.id.etFat);
        etMuscle = findViewById(R.id.etMuscle);

        weightLayout = findViewById(R.id.weightInputLayout);
        bmiLayout    = findViewById(R.id.bmiInputLayout);
        fatLayout    = findViewById(R.id.fatInputLayout);
        muscleLayout = findViewById(R.id.muscleInputLayout);

        // kezdeti dátum kiírás
        etDate.setText(android.text.format.DateFormat.getDateFormat(this).format(selectedDate));
    }

    private void initDatePicker() {
        etDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker =
                    MaterialDatePicker.Builder.datePicker()
                            .setTitleText("Mérés dátuma")
                            .setSelection(selectedDate.getTime())
                            .build();
            picker.addOnPositiveButtonClickListener(selection -> {
                selectedDate = new Date(selection);
                etDate.setText(android.text.format.DateFormat.getDateFormat(this).format(selectedDate));
            });
            picker.show(getSupportFragmentManager(), "datePicker");
        });
    }

    private void onSaveClicked() {
        if (!validateInputs()) return;

        float weight = Float.parseFloat(etWeight.getText().toString());
        float bmi    = Float.parseFloat(etBmi.getText().toString());
        float fat    = Float.parseFloat(etFat.getText().toString());
        float muscle = Float.parseFloat(etMuscle.getText().toString());

        Measurement m = new Measurement(selectedDate, weight, bmi, fat, muscle);

        repo.addMeasurement(m, new FirestoreRepository.SimpleCallback() {
            @Override public void onSuccess() {
                Toast.makeText(AddMeasurementActivity.this, "Sikeres mentés!", Toast.LENGTH_SHORT).show();
                finish(); // vissza a Home-ra
            }
            @Override public void onFailure(Exception e) {
                Toast.makeText(AddMeasurementActivity.this, "Hiba: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Egyszerű tartomány-ellenőrzés.  Változtass, ha más határok kellenek. */
    private boolean validateInputs() {
        boolean ok = true;

        ok &= checkNumberField(weightLayout, etWeight, 20f, 300f);
        ok &= checkNumberField(bmiLayout,    etBmi,    10f,  60f );
        ok &= checkNumberField(fatLayout,    etFat,     2f,  70f );
        ok &= checkNumberField(muscleLayout, etMuscle,  10f,  70f );

        return ok;
    }

    private boolean checkNumberField(TextInputLayout layout, TextInputEditText edit,
                                     float min, float max) {
        String txt = edit.getText() == null ? "" : edit.getText().toString();
        try {
            float val = Float.parseFloat(txt);
            if (val < min || val > max) {
                layout.setError("Érték " + min + " – " + max + " között");
                return false;
            }
            layout.setError(null);
            return true;
        } catch (NumberFormatException e) {
            layout.setError("Kötelező");
            return false;
        }
    }

    public void backFromAdd(View view) {finish();}
}
