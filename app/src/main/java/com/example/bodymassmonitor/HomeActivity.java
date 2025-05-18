package com.example.bodymassmonitor;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Home screen: stacked bar‑chart of body‑composition measurements with a detail card underneath.
 * <p>
 * Taps on any bar segment (or bar position) populate the detail card; newest record is shown on start.
 * Real‑time updates come from {@link FirestoreRepository#listenToMeasurements}.
 */
public class HomeActivity extends AppCompatActivity {

    // ── State ──────────────────────────────────────────────────────────
    private final List<Measurement> data = new ArrayList<>();
    private final FirestoreRepository repo = new FirestoreRepository();
    private ListenerRegistration reg;

    // ── UI ─────────────────────────────────────────────────────────────
    private BarChart barChart;
    private MaterialCardView cardDetail;
    private TextView tvDate, tvWeight, tvBmi, tvFat, tvMuscle, tvOther;
    private DateFormat df;

    // ── Lifecycle ──────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        df = android.text.format.DateFormat.getDateFormat(this);

        // Top‑app‑bar menu ------------------------------------------------
        MaterialToolbar top = findViewById(R.id.topAppBar);
        top.setOnMenuItemClickListener(this::handleMenu);

        // Bar‑chart -------------------------------------------------------
        barChart = findViewById(R.id.barChart);
        barChart.getDescription().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);

        barChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override public void onValueSelected(Entry e, Highlight h) {
                int idx = (int) e.getX();
                if (idx >= 0 && idx < data.size()) showDetails(data.get(idx));
            }
            @Override public void onNothingSelected() { /* no‑op */ }
        });

        // Detail‑card -----------------------------------------------------
        cardDetail = findViewById(R.id.cardDetail);
        tvDate    = findViewById(R.id.tvDate);
        tvWeight  = findViewById(R.id.tvWeight);
        tvBmi     = findViewById(R.id.tvBmi);
        tvFat     = findViewById(R.id.tvFat);
        tvMuscle  = findViewById(R.id.tvMuscle);
        tvOther   = findViewById(R.id.tvOther);

        // FABs ------------------------------------------------------------
        FloatingActionButton fabAdd   = findViewById(R.id.fab_add);
        FloatingActionButton fabStats = findViewById(R.id.fab_stats);

        fabAdd.setOnClickListener(v ->
                startActivity(new Intent(this, AddMeasurementActivity.class)));
        fabStats.setOnClickListener(v ->
                startActivity(new Intent(this, ChartActivity.class)));
    }

    @Override protected void onStart() {
        super.onStart();
        reg = repo.listenToMeasurements((qs, ex) -> {
            if (ex != null) return;
            data.clear();
            qs.forEach(d -> {
                Measurement m = d.toObject(Measurement.class);
                m.setId(d.getId());
                data.add(m);
            });
            updateChart();
            if (!data.isEmpty()) showDetails(data.get(data.size() - 1)); // newest record
        });
    }

    @Override protected void onStop() {
        super.onStop();
        if (reg != null) reg.remove();
    }

    // ── Chart builder ──────────────────────────────────────────────────
    private void updateChart() {
        List<BarEntry> entries = new ArrayList<>();
        List<String>   xLabels = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            Measurement m = data.get(i);
            float fatW    = m.getWeight() * m.getFat()    / 100f;
            float muscleW = m.getWeight() * m.getMuscle() / 100f;
            float otherW  = m.getWeight() - fatW - muscleW;

            entries.add(new BarEntry(i, new float[]{fatW, muscleW, otherW}));
            xLabels.add(df.format(m.getDate()));
        }

        BarDataSet ds = new BarDataSet(entries, "Body composition");
        ds.setColors(new int[]{Color.RED, Color.GREEN, Color.BLUE});
        ds.setStackLabels(new String[]{"Fat", "Muscle", "Other"});

        BarData bd = new BarData(ds);
        bd.setBarWidth(0.6f);
        barChart.setData(bd);

        XAxis x = barChart.getXAxis();
        x.setGranularity(1f);
        x.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        x.setPosition(XAxis.XAxisPosition.BOTTOM);

        barChart.invalidate();
    }

    // ── Detail‑card updater ───────────────────────────────────────────
    private void showDetails(Measurement m) {
        tvDate.setText(df.format(m.getDate()));
        tvWeight.setText(String.format("Súly: %.1f kg", m.getWeight()));
        tvBmi.setText(String.format("BMI: %.1f", m.getBmi()));
        tvFat.setText(String.format("Zsír: %.1f%%", m.getFat()));
        tvMuscle.setText(String.format("Izom: %.1f%%", m.getMuscle()));

        float fatW    = m.getWeight() * m.getFat()    / 100f;
        float muscleW = m.getWeight() * m.getMuscle() / 100f;
        float otherW  = m.getWeight() - fatW - muscleW;
        tvOther.setText(String.format("Egyéb: %.1f kg", otherW));
    }

    // ── Menu handler (if/else avoids switch‑constant quirk) ────────────
    private boolean handleMenu(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return true;
        } else if (id == R.id.action_import) {
            Snackbar.make(barChart, "Import funkció később jön 🚧", Snackbar.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }
}
