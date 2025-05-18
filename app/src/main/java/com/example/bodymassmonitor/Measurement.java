package com.example.bodymassmonitor;

import java.util.Date;

public class Measurement {

    private String id;      // Firestore dokumentum-ID (olvasáskor töltjük)
    private Date   date;
    private float  weight;  // kg
    private float  bmi;
    private float  fat;     // %
    private float  muscle;  // %

    public Measurement() {}

    public Measurement(Date date, float weight, float bmi,
                       float fat, float muscle) {
        this.date   = date;
        this.weight = weight;
        this.bmi    = bmi;
        this.fat    = fat;
        this.muscle = muscle;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public float getBmi() {
        return bmi;
    }

    public void setBmi(float bmi) {
        this.bmi = bmi;
    }

    public float getFat() {
        return fat;
    }

    public void setFat(float fat) {
        this.fat = fat;
    }

    public float getMuscle() {
        return muscle;
    }

    public void setMuscle(float muscle) {
        this.muscle = muscle;
    }
}
