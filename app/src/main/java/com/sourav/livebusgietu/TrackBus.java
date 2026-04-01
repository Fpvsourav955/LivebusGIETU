package com.sourav.livebusgietu;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class TrackBus extends AppCompatActivity {
    RecyclerView recyclerView;
    boolean isReversed = false;
    long startTime = -1;
    private  LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_track_bus);

        // System bar setup
        Window window = getWindow();
        View decorView = window.getDecorView();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        WindowInsetsControllerCompat insetsController = new WindowInsetsControllerCompat(window, decorView);
        insetsController.setAppearanceLightStatusBars(true);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            v.setPadding(
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            );
            return insets;
        });
        loadingDialog = new LoadingDialog(this);
        ImageView devback = findViewById(R.id.devback);
        devback.setOnClickListener(v -> finish());

        LinearLayout trackinmap = findViewById(R.id.trackinmap);
        trackinmap.setOnClickListener(v -> startActivity(new Intent(TrackBus.this, MapsActivity.class)));

        recyclerView = findViewById(R.id.recyclerView);
        TextView speedText = findViewById(R.id.speed_value);
        TextView startStatusText = findViewById(R.id.startStatusText);
        TextView stopTimeText = findViewById(R.id.stop_time);

        DatabaseReference speedRef = FirebaseDatabase.getInstance().getReference("busdata/BUS08/speed");
        DatabaseReference startTimeRef = FirebaseDatabase.getInstance().getReference("busdata/BUS08/start_time");
        loadingDialog.startLoadingDiloag();
        startTimeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object rawStartTime = snapshot.getValue();
                startTime = -1L;

                if (rawStartTime != null) {
                    try {
                        if (rawStartTime instanceof Long) {
                            startTime = (Long) rawStartTime;
                        } else if (rawStartTime instanceof String) {
                            startTime = Long.parseLong((String) rawStartTime);
                        }

                        if (startTime > 0) {
                            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                            String formattedTime = sdf.format(new Date(startTime));
                            startStatusText.setText("Started");
                            stopTimeText.setText("AT-" + formattedTime);
                            loadingDialog.dismissDialog();
                        } else {
                            startStatusText.setText("Not Started");
                            stopTimeText.setText("AT-Not Available");
                            loadingDialog.dismissDialog();
                        }
                    } catch (Exception e) {
                        startStatusText.setText("Not Started");
                        stopTimeText.setText("AT-Not Available");
                        Log.e("TrackBus", "Error parsing start_time: " + rawStartTime, e);
                        loadingDialog.dismissDialog();
                    }
                } else {
                    startStatusText.setText("Not Started");
                    stopTimeText.setText("AT-Not Available");
                    loadingDialog.dismissDialog();
                }

                loadBusStopData(speedRef, speedText);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadBusStopData(speedRef, speedText);
            }
        });

    }

    private void loadBusStopData(DatabaseReference speedRef, TextView speedText) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("busdata/BUS08/bus_stop");

        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<BusStop> stops = new ArrayList<>();
                long lastKnownTime = startTime > 0 ? startTime : System.currentTimeMillis();

                for (DataSnapshot snap : snapshot.getChildren()) {
                    String name = snap.child("name").getValue(String.class);
                    boolean isReached = Boolean.TRUE.equals(snap.child("isReached").getValue(Boolean.class));

                    double estMinutes = 0;
                    Object estRaw = snap.child("est").getValue();
                    if (estRaw instanceof Number) {
                        estMinutes = ((Number) estRaw).doubleValue();
                    } else if (estRaw instanceof String) {
                        try {
                            estMinutes = Double.parseDouble((String) estRaw);
                        } catch (Exception ignored) {
                        }
                    }

                    BusStop stop = new BusStop(name, isReached, estMinutes);

                    if (isReached) {
                        Object reachedRaw = snap.child("reached").getValue();
                        long reachedTime = getTimeSafe(reachedRaw);
                        stop.setEstimatedTime(reachedTime);

                        if (reachedTime > 0) {
                            stop.setDisplayTime(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(reachedTime)));
                            lastKnownTime = reachedTime;
                        } else {
                            stop.setDisplayTime("Not Available");
                        }
                    } else {
                        lastKnownTime += (long) (estMinutes * 60 * 1000);
                        stop.setEstimatedTime(lastKnownTime);
                        stop.setDisplayTime(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(lastKnownTime)));
                    }

                    stops.add(stop);

                }

            loadingDialog.dismissDialog();

                recyclerView.setLayoutManager(new LinearLayoutManager(TrackBus.this));
                recyclerView.setAdapter(new BusStopAdapter(stops));

                speedRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            try {
                                double speed = Double.parseDouble(Objects.requireNonNull(snapshot.getValue()).toString());
                                speedText.setText(String.format(Locale.getDefault(), "%.1f", speed));
                            } catch (Exception e) {
                                speedText.setText("0");
                            }
                        } else {
                            speedText.setText("0");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

                StringBuilder widgetRoute = new StringBuilder();
                for (BusStop stop : stops) {
                    String symbol = stop.isReached() ? "●" : "○";
                    String shortName = stop.getName();
                    if (shortName.length() > 10) shortName = shortName.split(" ")[0];
                    widgetRoute.append(symbol).append(" ").append(shortName).append("  ");
                }

                SharedPreferences prefs = getSharedPreferences("BusPrefs", MODE_PRIVATE);
                prefs.edit().putString("widget_route", widgetRoute.toString().trim()).apply();

                Context context = TrackBus.this;
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                ComponentName widget = new ComponentName(context, TrackBuswidget.class);
                int[] widgetIds = appWidgetManager.getAppWidgetIds(widget);
                Intent intent = new Intent(context, TrackBuswidget.class);
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
                sendBroadcast(intent);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    private long getTimeSafe(Object value) {
        try {
            if (value instanceof String s) {
                try {

                    return Long.parseLong(s);
                } catch (NumberFormatException e) {

                    SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault());
                    Date date = timeFormat.parse(s);
                    if (date != null) {

                        Date now = new Date();
                        date.setYear(now.getYear());
                        date.setMonth(now.getMonth());
                        date.setDate(now.getDate());
                        return date.getTime();
                    }
                }
            } else if (value instanceof Long) {
                return (Long) value;
            } else if (value instanceof Double && startTime > 0) {
                return startTime + (long) ((Double) value * 60 * 1000);
            } else if (value instanceof Integer && startTime > 0) {
                return startTime + (Integer) value * 60 * 1000L;
            }
        } catch (Exception ignored) {}
        return -1;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loadingDialog != null) {
            loadingDialog.dismissDialog();
        }
    }


}