package com.example.bodymassmonitor;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
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
 * Home screen with stacked bar-chart, detail card, CSV import
 * …és most már törlő gombbal a kijelölt méréshez.
 */
public class HomeActivity extends AppCompatActivity
        implements ImportCsvLoader.Callback {

    // ── State ───────────────────────────────────────────────────────
    private final List<Measurement> data = new ArrayList<>();
    private final FirestoreRepository repo = new FirestoreRepository();
    private ListenerRegistration reg;
    private Measurement selected;                 // aktuálisan kijelölt mérés

    // ── UI ──────────────────────────────────────────────────────────
    private BarChart barChart;
    private MaterialCardView cardDetail;
    private TextView tvDate, tvWeight, tvBmi, tvFat, tvMuscle, tvOther;
    private Button btnDelete;
    private DateFormat df;

    // SAF file picker
    private ActivityResultLauncher<String[]> pickCsvLauncher;

    // ── Lifecycle ───────────────────────────────────────────────────
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        df = android.text.format.DateFormat.getDateFormat(this);

        /* fájlválasztó */
        pickCsvLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                this::onCsvPicked);

        /* Top app-bar */
        MaterialToolbar top = findViewById(R.id.topAppBar);
        top.setOnMenuItemClickListener(this::handleMenu);

        /* Bar-chart */
        barChart = findViewById(R.id.barChart);
        barChart.getDescription().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);

        barChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override public void onValueSelected(Entry e, Highlight h) {
                int idx = (int) e.getX();
                if (idx >= 0 && idx < data.size()) {
                    selected = data.get(idx);
                    showDetails(selected);
                    btnDelete.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onNothingSelected() {
                selected = null;
                btnDelete.setVisibility(View.GONE);
            }
        });

        /* Részletező kártya */
        cardDetail = findViewById(R.id.cardDetail);
        tvDate   = findViewById(R.id.tvDate);
        tvWeight = findViewById(R.id.tvWeight);
        tvBmi    = findViewById(R.id.tvBmi);
        tvFat    = findViewById(R.id.tvFat);
        tvMuscle = findViewById(R.id.tvMuscle);
        tvOther  = findViewById(R.id.tvOther);
        btnDelete = findViewById(R.id.btn_delete);
        btnDelete.setOnClickListener(v -> confirmDelete());

        /* FAB-ok */
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
            if (!data.isEmpty()) {
                selected = data.get(data.size() - 1);
                showDetails(selected);
                btnDelete.setVisibility(View.VISIBLE);
            } else {
                selected = null;
                btnDelete.setVisibility(View.GONE);
            }
        });
    }
    @Override protected void onStop() {
        super.onStop();
        if (reg != null) reg.remove();
    }

    // ── Chart builder ───────────────────────────────────────────────
    private void updateChart() {
        List<BarEntry> entries = new ArrayList<>();
        List<String> xLabels   = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            Measurement m = data.get(i);
            float fatW    = m.getWeight() * m.getFat()    / 100f;
            float muscleW = m.getWeight() * m.getMuscle() / 100f;
            float otherW  = m.getWeight() - fatW - muscleW;

            entries.add(new BarEntry(i, new float[]{otherW, muscleW, fatW}));
            xLabels.add(df.format(m.getDate()));
        }

        BarDataSet ds = new BarDataSet(entries, "Body composition");
        ds.setColors(new int[]{Color.BLUE, Color.RED, Color.YELLOW});
        ds.setStackLabels(new String[]{"Egyéb", "Izom", "Zsír"});

        BarData bd = new BarData(ds);
        bd.setBarWidth(0.6f);
        barChart.setData(bd);

        XAxis x = barChart.getXAxis();
        x.setGranularity(1f);
        x.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        x.setPosition(XAxis.XAxisPosition.BOTTOM);

        barChart.invalidate();
    }

    // ── Detail-card updater ─────────────────────────────────────────
    private void showDetails(Measurement m) {
        tvDate.setText(df.format(m.getDate()));
        tvWeight.setText(getString(R.string.fmt_weight, m.getWeight()));
        tvBmi.setText(getString(R.string.fmt_bmi, m.getBmi()));
        tvFat.setText(getString(R.string.fmt_fat, m.getFat()));
        tvMuscle.setText(getString(R.string.fmt_muscle, m.getMuscle()));

        float fatW    = m.getWeight() * m.getFat()    / 100f;
        float muscleW = m.getWeight() * m.getMuscle() / 100f;
        float otherW  = m.getWeight() - fatW - muscleW;
        tvOther.setText(getString(R.string.fmt_other, otherW));
    }

    // ── Delete flow ────────────────────────────────────────────────
    private void confirmDelete() {
        if (selected == null) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_confirmation_title)
                .setMessage(R.string.delete_confirmation_msg)
                .setPositiveButton(R.string.btn_delete, (d,w) -> doDelete())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void doDelete() {
        btnDelete.setEnabled(false);
        repo.deleteMeasurement(selected.getId(), new FirestoreRepository.SimpleCallback() {
            @Override public void onSuccess() {
                Snackbar.make(barChart, R.string.toast_deleted, Snackbar.LENGTH_SHORT).show();
                btnDelete.setVisibility(View.GONE);
                btnDelete.setEnabled(true);
                selected = null;
            }
            @Override public void onFailure(@NonNull Exception e) {
                Snackbar.make(barChart,
                        getString(R.string.toast_delete_failed, e.getMessage()),
                        Snackbar.LENGTH_LONG).show();
                btnDelete.setEnabled(true);
            }
        });
    }

    // ── Menu handler ───────────────────────────────────────────────
    private boolean handleMenu(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return true;
        } else if (id == R.id.action_import) {
            pickCsvLauncher.launch(
                    new String[]{"text/csv",
                            "text/comma-separated-values",
                            "text/plain"});
            return true;
        }
        return false;
    }

    // ── SAF callback ───────────────────────────────────────────────
    private void onCsvPicked(Uri uri) {
        if (uri == null) return; // user cancelled
        Snackbar.make(barChart, R.string.import_in_progress, Snackbar.LENGTH_SHORT).show();
        new ImportCsvLoader(this, getContentResolver(), repo, this).execute(uri);
    }

    // ── ImportCsvLoader.Callback ───────────────────────────────────
    @Override public void onImportFinished(ImportCsvLoader.Result r) {
        if (r.error != null) {
            Snackbar.make(barChart,
                    getString(R.string.import_failed, r.error.getMessage()),
                    Snackbar.LENGTH_LONG).show();
            return;
        }
        if (!r.duplicates.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.import_duplicates_title)
                    .setMessage(getString(R.string.import_duplicates_msg, r.duplicates.size()))
                    .setPositiveButton(R.string.btn_replace, (d,w) -> r.replaceRunnable.run())
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {
            Snackbar.make(barChart,
                    getString(R.string.import_success, r.imported),
                    Snackbar.LENGTH_SHORT).show();
        }
    }
}
