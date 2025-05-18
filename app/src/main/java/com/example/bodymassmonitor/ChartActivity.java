package com.example.bodymassmonitor;

import android.graphics.Paint;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bodymassmonitor.R;

import com.google.firebase.firestore.ListenerRegistration;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ChartActivity extends AppCompatActivity {

    private final FirestoreRepository repo = new FirestoreRepository();
    private ListenerRegistration reg;

    private final List<Measurement> list = new ArrayList<>();
    private MeasurementAdapter adapter;
    private GraphView graph;

    @Override protected void onCreate(@Nullable Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_chart);

        graph = findViewById(R.id.graph);

        adapter = new MeasurementAdapter(list, m ->
                repo.deleteMeasurement(m.getId(), new FirestoreRepository.SimpleCallback() {
                    @Override public void onSuccess() {
                        Toast.makeText(ChartActivity.this, "Törölve", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onFailure(Exception e) {
                        Toast.makeText(ChartActivity.this, "Hiba: "+e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }));

        RecyclerView rv = findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
    }

    @Override protected void onStart() {
        super.onStart();
        reg = repo.listenToMeasurements((qs, e) -> {
            if (e != null) { return; }
            list.clear();
            qs.forEach(d -> {
                Measurement m = d.toObject(Measurement.class);
                m.setId(d.getId());
                list.add(m);
            });
            Collections.sort(list, Comparator.comparing(Measurement::getDate));
            adapter.notifyDataSetChanged();
            plotGraph();
        });
    }

    @Override protected void onStop() {
        super.onStop();
        if (reg != null) reg.remove();
    }

    /** 4 sorozat – súly, BMI, zsír, izom */
    private void plotGraph() {
        graph.removeAllSeries();
        if (list.isEmpty()) return;

        int n = list.size();
        DataPoint[] wt = new DataPoint[n];
        DataPoint[] bmi = new DataPoint[n];
        DataPoint[] fat = new DataPoint[n];
        DataPoint[] mus = new DataPoint[n];

        for (int i = 0; i < n; i++) {
            double x = i;                           // egyszerű sorszám
            Measurement m = list.get(i);
            wt[i]  = new DataPoint(x, m.getWeight());
            bmi[i] = new DataPoint(x, m.getBmi());
            fat[i] = new DataPoint(x, m.getFat());
            mus[i] = new DataPoint(x, m.getMuscle());
        }

        graph.addSeries(styled(new LineGraphSeries<>(wt), 0));
        graph.addSeries(styled(new LineGraphSeries<>(bmi), 1));
        graph.addSeries(styled(new LineGraphSeries<>(fat), 2));
        graph.addSeries(styled(new LineGraphSeries<>(mus), 3));


        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(n - 1);
        graph.getGridLabelRenderer().setHorizontalAxisTitle("Mérés sorszám");
    }

    /** Vékony vonal + pontok */
    /* Paletta – 4 eltérő szín (Material 3 árnyalatok) */
    private static final int[] SERIES_COLORS = {
            0xFF1E88E5, // kék
            0xFF43A047, // zöld
            0xFFF4511E, // narancs
            0xFFE53935  // piros
    };

    /** Alap beállítás + lekerekített vonalvég, saját Paint-tel */
    private LineGraphSeries<DataPoint> styled(LineGraphSeries<DataPoint> s, int colorIdx) {
        s.setDrawDataPoints(true);
        s.setDataPointsRadius(4f);
        s.setAnimated(true);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3f);                 // vonalvastagság
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(SERIES_COLORS[colorIdx % SERIES_COLORS.length]);

        s.setCustomPaint(p);
        return s;
    }
}
