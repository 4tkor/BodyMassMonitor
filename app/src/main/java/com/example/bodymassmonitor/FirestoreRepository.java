package com.example.bodymassmonitor;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Date;

/**
 * Convenience wrapper around the user/<uid>/measurements sub‑collection.
 *
 * <p>**NEW** helper methods for the CSV‑import:
 * <ul>
 *     <li>{@link #existsForDate(Date)} – szinkron (blokkoló) ellenőrzés, van‑e már rekord ugyanazzal a dátummal.</li>
 *     <li>{@link #addOrReplaceByDate(Measurement)} – upsert: ha van, felülír; különben beszúr.</li>
 * </ul></p>
 *
 * <p>Az "await" hívások miatt ezeket **háttérszálról** kell hívni (lásd ImportCsvLoader).</p>
 */
public class FirestoreRepository {

    private static final String COLLECTION_USERS       = "users";
    private static final String SUBCOLLECTION_MEASURES = "measurements";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth    = FirebaseAuth.getInstance();

    // ---- callback‑ek -------------------------------------------------
    public interface SimpleCallback {
        void onSuccess();
        void onFailure(@NonNull Exception e);
    }

    public interface MeasurementsListener {
        void onEvent(QuerySnapshot snapshots, Exception e);
    }

    // ------ Public CRUD (async) --------------------------------------
    public void addMeasurement(Measurement m, @NonNull SimpleCallback cb) {
        getMeasCol().add(m)
                .addOnSuccessListener(r -> cb.onSuccess())
                .addOnFailureListener(cb::onFailure);
    }

    public ListenerRegistration listenToMeasurements(@NonNull MeasurementsListener l) {
        return getMeasCol().orderBy("date").addSnapshotListener(l::onEvent);
    }

    public void deleteMeasurement(String docId, @NonNull SimpleCallback cb) {
        getMeasCol().document(docId).delete()
                .addOnSuccessListener(r -> cb.onSuccess())
                .addOnFailureListener(cb::onFailure);
    }

    // ------ Synchronous helpers for ImportCsvLoader ------------------

    /**
     * Returns {@code true} if a measurement already exists for the given calendar day.
     * <p>⚠️ Blokkoló hívás – csak háttérszálon használd!</p>
     */
    public boolean existsForDate(@NonNull Date date) throws Exception {
        QuerySnapshot snap = Tasks.await(
                getMeasCol().whereEqualTo("date", date).limit(1).get());
        return !snap.isEmpty();
    }

    /**
     * Inserts or overwrites a measurement with the same date.
     * <p>⚠️ Blokkoló hívás – csak háttérszálon használd!</p>
     */
    public void addOrReplaceByDate(@NonNull Measurement m) throws Exception {
        QuerySnapshot snap = Tasks.await(
                getMeasCol().whereEqualTo("date", m.getDate()).limit(1).get());
        if (!snap.isEmpty()) {
            // update existing
            String docId = snap.getDocuments().get(0).getId();
            Tasks.await(getMeasCol().document(docId).set(m));
        } else {
            // create new
            Tasks.await(getMeasCol().add(m));
        }
    }

    // ------ Internal helpers ----------------------------------------
    private CollectionReference getMeasCol() {
        String uid = auth.getCurrentUser().getUid();
        return db.collection(COLLECTION_USERS)
                .document(uid)
                .collection(SUBCOLLECTION_MEASURES);
    }
}
