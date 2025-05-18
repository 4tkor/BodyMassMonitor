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

    public interface OnLongClick { void onLongClick(Measurement m); }
    private final List<Measurement> list; private final OnLongClick cb;
    public MeasurementAdapter(List<Measurement> list, OnLongClick cb) { this.list = list; this.cb = cb; }

    static class VH extends RecyclerView.ViewHolder {
        TextView date, values; VH(View v){ super(v); date=v.findViewById(R.id.tvDate); values=v.findViewById(R.id.tvValues);} }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t){
        View v= LayoutInflater.from(p.getContext()).inflate(R.layout.item_measurement,p,false); return new VH(v); }

    @Override public void onBindViewHolder(@NonNull VH h,int i){ Measurement m=list.get(i);
        h.date.setText(DateFormat.getDateInstance().format(m.getDate()));
        h.values.setText(String.format("%.1f kg | %.1f%% / %.1f%%",m.getWeight(),m.getFat(),m.getMuscle()));
        h.itemView.setOnLongClickListener(v->{cb.onLongClick(m);return true;}); }

    @Override public int getItemCount(){ return list.size(); }
}
