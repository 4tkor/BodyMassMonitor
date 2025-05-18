package com.example.bodymassmonitor;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class ChartActivity extends AppCompatActivity {

    private LineChart chart;
    private final FirestoreRepository repo = new FirestoreRepository();
    private ListenerRegistration reg;
    private List<Measurement> all = new ArrayList<>();

    enum Range { MONTH, QUARTER, YEAR, ALL }
    private Range currentRange = Range.ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        MaterialToolbar bar = findViewById(R.id.chartToolbar);
        bar.setNavigationOnClickListener(v -> finish());

        chart = findViewById(R.id.areaChart);
        chart.getDescription().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        Legend l = chart.getLegend();
        l.setEnabled(false);

        MaterialButtonToggleGroup group = findViewById(R.id.groupFilters);
        group.addOnButtonCheckedListener((g, id, checked) -> {
            if (!checked) return;
            if (id == R.id.btn_month) currentRange = Range.MONTH;
            else if (id == R.id.btn_quarter) currentRange = Range.QUARTER;
            else if (id == R.id.btn_year) currentRange = Range.YEAR;
            else currentRange = Range.ALL;
            draw();
        });
    }

    @Override protected void onStart() {
        super.onStart();
        reg = repo.listenToMeasurements((snap, err) -> {
            if (err != null) return;
            all = new ArrayList<>();
            snap.forEach(d -> all.add(d.toObject(Measurement.class)));
            draw();
        });
    }

    @Override protected void onStop() {
        super.onStop();
        if (reg != null) reg.remove();
    }

    private void draw() {
        List<Measurement> src = filter();
        if (src.isEmpty()) { chart.clear(); return; }

        List<Entry> fatCum = new ArrayList<>();
        List<Entry> musCum = new ArrayList<>();
        List<Entry> othCum = new ArrayList<>();

        float cumFat = 0, cumMus = 0, cumOth = 0;
        for (int i = 0; i < src.size(); i++) {
            Measurement m = src.get(i);
            float fatW = m.getWeight() * m.getFat() / 100f;
            float musW = m.getWeight() * m.getMuscle() / 100f;
            float othW = m.getWeight() - fatW - musW;
            cumFat += fatW;
            cumMus += musW;
            cumOth += othW;
            fatCum.add(new Entry(i, cumFat));
            musCum.add(new Entry(i, cumFat + cumMus));
            othCum.add(new Entry(i, cumFat + cumMus + cumOth));
        }

        LineDataSet fatSet = makeSet(fatCum, 0x55F44336); // red alpha
        LineDataSet musSet = makeSet(musCum, 0x554CAF50); // green alpha
        LineDataSet othSet = makeSet(othCum, 0x552196F3); // blue alpha

        LineData data = new LineData(fatSet, musSet, othSet);
        chart.setData(data);
        chart.invalidate();
    }

    private LineDataSet makeSet(List<Entry> vals, int color) {
        LineDataSet s = new LineDataSet(vals, "");
        s.setDrawFilled(true);
        s.setFillColor(color);
        s.setDrawValues(false);
        s.setDrawCircles(false);
        s.setColor(color);
        s.setLineWidth(1f);
        return s;
    }

    private List<Measurement> filter() {
        if (currentRange == Range.ALL) return all;
        Calendar c = Calendar.getInstance();
        switch (currentRange) {
            case MONTH:   c.add(Calendar.MONTH, -1); break;
            case QUARTER: c.add(Calendar.MONTH, -3); break;
            case YEAR:    c.add(Calendar.YEAR,  -1); break;
        }
        Date limit = c.getTime();
        return all.stream()
                .filter(m -> m.getDate().after(limit))
                .collect(Collectors.toList());
    }
}