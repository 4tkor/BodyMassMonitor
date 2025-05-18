package com.example.bodymassmonitor;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Background CSV parser + Firestore importer.
 * <p>
 * Elfogadja a fejlécet és működik fejléc nélküli fájlokkal is, továbbá kezeli a ',' és ';' szeparátort.
 */
public class ImportCsvLoader {

    public static class Result {
        public int imported;
        public List<Measurement> duplicates = new ArrayList<>();
        public Throwable error;
        /** Runnable to call if user decides to overwrite duplicates. */
        Runnable replaceRunnable;
    }

    public interface Callback {
        void onImportFinished(Result result);
    }

    private final Callback cb;
    private final ContentResolver cr;
    private final FirestoreRepository repo;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public ImportCsvLoader(@NonNull Callback owner, @NonNull ContentResolver cr,
                           @NonNull FirestoreRepository repo, @NonNull Callback cb) {
        this.cb = cb;
        this.cr = cr;
        this.repo = repo;
    }

    public void execute(Uri uri) {
        io.execute(() -> load(uri));
    }

    private void load(Uri uri) {
        Result res = new Result();
        try (InputStream is = cr.openInputStream(uri);
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());

            String line = br.readLine();
            if (line == null) throw new IllegalArgumentException("Üres fájl");

            // Ha az első sor nem dátummal kezdődik, akkor fejléc → átugorjuk.
            if (!line.matches("^\\d{4}\\.\\d{2}\\.\\d{2}.*")) {
                line = br.readLine();
            }

            List<Measurement> toImport = new ArrayList<>();
            while (line != null) {
                if (!TextUtils.isEmpty(line.trim())) {
                    // Támogatja a pontosvesszőt és vesszőt is
                    String[] parts = line.split("[;,]");
                    if (parts.length < 4) {
                        throw new IllegalArgumentException("Sor formátuma hibás: " + line);
                    }
                    Date date = sdf.parse(parts[0].trim());
                    float fat = Float.parseFloat(parts[1].replace(',', '.'));
                    float muscle = Float.parseFloat(parts[2].replace(',', '.'));
                    float weight = Float.parseFloat(parts[3].replace(',', '.'));
                    float bmi = (parts.length >= 5 && !parts[4].isEmpty())
                            ? Float.parseFloat(parts[4].replace(',', '.'))
                            : weight / ((float) Math.pow(1.70, 2)); // fallback BMI

                    Measurement m = new Measurement();
                    m.setDate(date);
                    m.setFat(fat);
                    m.setMuscle(muscle);
                    m.setWeight(weight);
                    m.setBmi(bmi);
                    toImport.add(m);
                }
                line = br.readLine();
            }

            // Check duplicates
            for (Measurement m : toImport) {
                if (repo.existsForDate(m.getDate())) {
                    res.duplicates.add(m);
                }
            }

            Runnable inserter = () -> {
                try {
                    for (Measurement m : toImport) {
                        repo.addOrReplaceByDate(m);
                    }
                    res.imported = toImport.size();
                } catch (Exception e) {
                    res.error = e;
                }
                main.post(() -> cb.onImportFinished(res));
            };

            if (!res.duplicates.isEmpty()) {
                res.replaceRunnable = inserter;
                main.post(() -> cb.onImportFinished(res));
            } else {
                inserter.run();
            }
        } catch (Throwable t) {
            res.error = t;
            main.post(() -> cb.onImportFinished(res));
        }
    }
}
