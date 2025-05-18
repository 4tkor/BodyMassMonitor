package com.example.bodymassmonitor;

import androidx.annotation.NonNull;

import com.example.bodymassmonitor.Measurement;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

public class  FirestoreRepository {

    private static final String COLLECTION_USERS        = "users";
    private static final String SUBCOLLECTION_MEASURES  = "measurements";

    private final FirebaseFirestore db   = FirebaseFirestore.getInstance();
    private final FirebaseAuth      auth = FirebaseAuth.getInstance();

    // ---- callback-ek ----
    public interface SimpleCallback {
        void onSuccess();
        void onFailure(@NonNull Exception e);
    }

    // asynchronous list-load callback (real-time)
    public interface MeasurementsListener {
        void onEvent(QuerySnapshot snapshots, Exception e);
    }

    // ------ CRUD -----------------------------------------------------------

    public void addMeasurement(Measurement m, @NonNull SimpleCallback cb) {
        String uid = auth.getCurrentUser().getUid();
        CollectionReference colRef = db.collection(COLLECTION_USERS)
                .document(uid)
                .collection(SUBCOLLECTION_MEASURES);
        colRef.add(m)
                .addOnSuccessListener(unused -> cb.onSuccess())
                .addOnFailureListener(cb::onFailure);
    }

    /** Valós idejű feliratkozás – a hívó oldalon meg kell őrizni a
     *  visszaadott ListenerRegistration-t, és onStop()-ban levenni. */
    public ListenerRegistration listenToMeasurements(@NonNull MeasurementsListener l) {
        String uid = auth.getCurrentUser().getUid();
        CollectionReference colRef = db.collection(COLLECTION_USERS)
                .document(uid)
                .collection(SUBCOLLECTION_MEASURES);
        return colRef.orderBy("date")
                .addSnapshotListener(l::onEvent);
    }

    public void deleteMeasurement(String docId, @NonNull SimpleCallback cb) {
        String uid = auth.getCurrentUser().getUid();
        db.collection(COLLECTION_USERS).document(uid)
                .collection(SUBCOLLECTION_MEASURES).document(docId)
                .delete()
                .addOnSuccessListener(unused -> cb.onSuccess())
                .addOnFailureListener(cb::onFailure);
    }
}
