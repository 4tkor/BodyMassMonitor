package com.example.bodymassmonitor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.text.DateFormat;
import java.util.List;

public class MeasurementAdapter extends RecyclerView.Adapter<MeasurementAdapter.VH> {

    public interface OnLongClick {
        void onLongClick(Measurement m);
    }

    private final List<Measurement> data;
    private final OnLongClick longClick;

    public MeasurementAdapter(List<Measurement> data, OnLongClick longClick) {
        this.data = data;
        this.longClick = longClick;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int vType) {
        View v = LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_measurement, p, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Measurement m = data.get(pos);
        h.tvDate.setText(DateFormat.getDateInstance().format(m.getDate()));
        h.tvValues.setText(
                "kg " + m.getWeight() + " | BMI " + m.getBmi() +
                        " | zsír " + m.getFat() + "% | izom " + m.getMuscle() + "%");

        h.itemView.setOnLongClickListener(v -> {
            longClick.onLongClick(m);
            return true;
        });
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDate, tvValues;
        VH(@NonNull View item) {
            super(item);
            tvDate   = item.findViewById(R.id.tvDate);
            tvValues = item.findViewById(R.id.tvValues);
        }
    }
}
