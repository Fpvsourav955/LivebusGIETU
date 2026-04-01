package com.sourav.livebusgietu;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BusStopAdapter extends RecyclerView.Adapter<BusStopAdapter.ViewHolder> {

    private final List<BusStop> stopList;

    public BusStopAdapter(List<BusStop> stopList) {
        this.stopList = stopList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bus_stop, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BusStop stop = stopList.get(position);
        holder.stopName.setText(stop.getName());


        holder.backgroundLine.post(() -> {
            if (stop.isReached()) {
                holder.dot.setBackgroundResource(R.drawable.dot_filled);
                holder.progressFill.setVisibility(View.VISIBLE);
                holder.stopTime.setText("Reached at " + stop.getDisplayTime());
                holder.stopTime.setVisibility(View.VISIBLE);
            } else {
                holder.dot.setBackgroundResource(R.drawable.dot_drawable);
                holder.progressFill.setVisibility(View.INVISIBLE);
                holder.stopTime.setText("EAT: " + stop.getDisplayTime());
                holder.stopTime.setVisibility(View.VISIBLE);
            }
        });

    }

    @Override
    public int getItemCount() {
        return stopList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView stopName;
        TextView stopTime;
        View dot, progressFill, backgroundLine;


        ViewHolder(View itemView) {
            super(itemView);
            stopName = itemView.findViewById(R.id.stop_name);
            dot = itemView.findViewById(R.id.dot);

            stopTime = itemView.findViewById(R.id.stop_time);
            progressFill = itemView.findViewById(R.id.progress_fill);
            backgroundLine = itemView.findViewById(R.id.background_line);
        }
    }
}
