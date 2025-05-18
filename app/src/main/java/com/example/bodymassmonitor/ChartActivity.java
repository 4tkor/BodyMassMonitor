package com.example.bodymassmonitor;

import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IFillFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.appbar.MaterialToolbar;

import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class ChartActivity extends AppCompatActivity {

    private LineChart chart;
    private TextView  tvComp;
    private final List<Measurement> data = new ArrayList<>();
    private final FirestoreRepository repo = new FirestoreRepository();

    private DateFormat df;
    private long startEpoch;
    private static final long ONE_DAY = 86_400_000L;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        df     = android.text.format.DateFormat.getDateFormat(this);
        chart  = findViewById(R.id.lineChart);
        tvComp = findViewById(R.id.tvComposition);
        configureChart();

        MaterialToolbar bar = findViewById(R.id.topChartBar);

        /* ← ÚJ: tegyük action-barrá, és kapcsoljuk be a „home as up” ikont */
        setSupportActionBar(bar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        /* a nav-ikon most már biztosan létezik, erre lépünk vissza */
        bar.setNavigationOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed());

        bar.setNavigationOnClickListener(v -> finish());

        // real-time Firestore feed
        repo.listenToMeasurements((snap, ex) -> {
            if (ex != null) return;
            data.clear();
            snap.forEach(d -> data.add(d.toObject(Measurement.class)));
            updateChart();
        });
    }

    /* ---------- Chart look & feel ---------- */
    private void configureChart() {
        chart.getDescription().setEnabled(false);
        chart.setExtraOffsets(16f,16f,16f,56f); // alul hely a feliratnak
        chart.setDrawBorders(false);
        chart.setDrawGridBackground(false);

        // Y
        YAxis y = chart.getAxisLeft();
        y.setAxisMinimum(0f);
        y.setSpaceTop(15f);
        y.setTextSize(13f);
        y.setTextColor(Color.DKGRAY);
        chart.getAxisRight().setEnabled(false);

        // X
        XAxis x = chart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setGranularity(1f);
        x.setTextSize(13f);
        x.setTextColor(Color.DKGRAY);
        x.setLabelRotationAngle(-45f);
        x.setDrawGridLines(false);

        // legend
        Legend lg = chart.getLegend();
        lg.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        lg.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        lg.setForm(Legend.LegendForm.SQUARE);
        lg.setTextSize(14f);
    }

    /* ---------- Build stacked area ---------- */
    private void updateChart() {
        if (data.isEmpty()) return;

        List<Entry> eOther  = new ArrayList<>();
        List<Entry> eMuscle = new ArrayList<>();
        List<Entry> eFat    = new ArrayList<>();

        // gyors hozzáférés az alsó réteg Y-jaihoz (x→y)
        HashMap<Float, Float> mapOther  = new HashMap<>();
        HashMap<Float, Float> mapMuscle = new HashMap<>();

        startEpoch = data.get(0).getDate().getTime();

        for (Measurement m : data) {
            float x = (m.getDate().getTime() - startEpoch) / (float) ONE_DAY;

            float fatKg    = m.getWeight() * m.getFat()    / 100f;
            float muscleKg = m.getWeight() * m.getMuscle() / 100f;
            float otherKg  = m.getWeight() - fatKg - muscleKg;

            float yOther  = otherKg;
            float yMuscle = otherKg + muscleKg;
            float yFat    = otherKg + muscleKg + fatKg;

            eOther .add(new Entry(x, yOther));
            eMuscle.add(new Entry(x, yMuscle));
            eFat   .add(new Entry(x, yFat));

            mapOther.put(x,  yOther);
            mapMuscle.put(x, yMuscle);
        }

        /* --- DataSets --- */
        LineDataSet dsOther = makeSet(eOther, getString(R.string.area_other),
                Color.parseColor("#2196F3"), 200, null);         // kék, baseline=0

        LineDataSet dsMuscle = makeSet(eMuscle, getString(R.string.area_muscle),
                Color.parseColor("#F44336"), 180,
                new LayerFillFormatter(mapOther));               // piros, baseline=Other

        LineDataSet dsFat = makeSet(eFat, getString(R.string.area_fat),
                Color.parseColor("#FFC107"), 160,
                new LayerFillFormatter(mapMuscle));              // sárga, baseline=Muscle

        chart.setData(new LineData(dsOther, dsMuscle, dsFat));

        // X-tengely dátumformázó
        chart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                long ts = startEpoch + (long) value * ONE_DAY;
                return df.format(new Date(ts));
            }
        });

        // kijelölés kezelése
        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override public void onValueSelected(Entry e, Highlight h) {
                int idx = Math.round(e.getX());
                if (idx>=0 && idx < data.size()) {
                    Measurement m = data.get(idx);
                    float fatKg    = m.getWeight() * m.getFat()    / 100f;
                    float muscleKg = m.getWeight() * m.getMuscle() / 100f;
                    tvComp.setText(getString(
                            R.string.chart_composition_fmt,
                            fatKg, m.getFat(),
                            muscleKg, m.getMuscle()));
                }
            }
            @Override public void onNothingSelected() { tvComp.setText(""); }
        });

        chart.invalidate();
    }

    /* ---------- Helpers ---------- */
    private LineDataSet makeSet(List<Entry> entries, String lbl,
                                int color, int alpha,
                                IFillFormatter ff) {
        LineDataSet ds = new LineDataSet(entries, lbl);
        ds.setMode(LineDataSet.Mode.STEPPED);
        ds.setDrawCircles(false);
        ds.setLineWidth(1.2f);
        ds.setColor(color);
        ds.setDrawFilled(true);
        ds.setFillColor(color);
        ds.setFillAlpha(alpha);
        ds.setDrawValues(false);
        if (ff != null) ds.setFillFormatter(ff);
        return ds;
    }

    /**  Kitöltés az alatta lévő réteg Y-jaiig. */
    private static class LayerFillFormatter implements IFillFormatter {
        private final HashMap<Float, Float> base;
        LayerFillFormatter(HashMap<Float, Float> map){ this.base = map; }
        @Override public float getFillLinePosition(ILineDataSet ds, LineDataProvider prov) {
            return 0f;   // nem használt
        }
        public float getFillLinePosition(ILineDataSet ds, float x, float y) {
            Float b = base.get(x);
            return b == null ? 0f : b;
        }
    }
}
