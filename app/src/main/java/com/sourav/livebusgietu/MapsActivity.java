package com.sourav.livebusgietu;

import static android.app.PendingIntent.getActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.firebase.database.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {
    private  LoadingDialog loadingDialog;
    private GoogleMap mMap;
    private CardView busInfoCard;
    private TextView cardBusName, cardSpeed, cardNextStop, cardEta;
    private Bitmap busMarkerBitmap, stopMarkerBitmap;


    private String selectedBus = null;
    private final Map<String, Marker> markerMap = new ConcurrentHashMap<>();

    private static final Stop[] ROUTE = new Stop[] {
            new Stop("GIET Bus Stand", 19.048835, 83.833772),
            new Stop("Gunupur College", 19.059997, 83.823377),
            new Stop("SBI Road",        19.063734, 83.820572),
            new Stop("BN",              19.068345, 83.816426),
            new Stop("Bypass",          19.069816, 83.815185),
            new Stop("JJ",              19.071308, 83.813032),
            new Stop("Old Gunupur",     19.071978, 83.810045)
    };

    private static final String GOOGLE_MAPS_KEY = "AIzaSyDjSbAUDfxk4361JoDYrv8Wm-eVfstpLEw";
    private static final double NEAR_THRESHOLD_M = 70.0;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);


        busInfoCard = findViewById(R.id.busInfoCard);
        Glide.with(this)
                .load(R.drawable.card_back_bus)
                .centerCrop()
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        busInfoCard.setBackground(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
        cardBusName = findViewById(R.id.tvBusName);
        cardSpeed = findViewById(R.id.tvSpeed);
        cardNextStop = findViewById(R.id.tvNextStop);
        cardEta = findViewById(R.id.tvETA);


        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;


        BitmapDrawable busStopDrawable = (BitmapDrawable) ContextCompat.getDrawable(this, R.drawable.maps);
        BitmapDrawable busDrawable = (BitmapDrawable) ContextCompat.getDrawable(this, R.drawable.busstop);

        if (busStopDrawable != null) {
            Bitmap b = busStopDrawable.getBitmap();
            stopMarkerBitmap = (b != null) ? Bitmap.createScaledBitmap(b, 65, 65, false) : null;
        }

        if (busDrawable != null) {
            Bitmap b = busDrawable.getBitmap();
            busMarkerBitmap = (b != null) ? Bitmap.createScaledBitmap(b, 80, 80, false) : null;
        }

        // Initial camera position
        LatLng home = new LatLng(19.048835, 83.833772);
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(home)
                .zoom(15f)
                .bearing(40f)
                .tilt(0f)
                .build();
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));


        for (Stop stop : ROUTE) {
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(stop.toLatLng())
                    .title(stop.name);

            if (stopMarkerBitmap != null) {
                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(stopMarkerBitmap));
            } else {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            }
            mMap.addMarker(markerOptions);
        }

        DatabaseReference busesRef = FirebaseDatabase.getInstance().getReference("busdata");

        mMap.setOnMarkerClickListener(marker -> {

            if (marker.getTitle() != null && marker.getTitle().startsWith("Bus: ")) {
                selectedBus = marker.getTitle().replace("Bus: ", "");

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 16.2f));

                updateCardWithMarker(marker);

                return true;
            } else {

                busInfoCard.setVisibility(View.GONE);
                selectedBus = null;

                return false;
            }
        });

        mMap.setOnMapClickListener(latLng -> {

            busInfoCard.setVisibility(View.GONE);
            selectedBus = null;
        });

        busesRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for (Marker m : markerMap.values()) {
                    m.remove();
                }
                markerMap.clear();

                for (DataSnapshot busSnapshot : snapshot.getChildren()) {
                    String busName = busSnapshot.getKey();
                    if (busName == null) continue;

                    Double lat = readDouble(busSnapshot.child("lat"));
                    Double lng = readDouble(busSnapshot.child("lng"));
                    Double speed = readDouble(busSnapshot.child("speed"));

                    if (lat == null || lng == null) continue;

                    LatLng busPos = new LatLng(lat, lng);

                    MarkerOptions markerOptions = new MarkerOptions()
                            .position(busPos)
                            .title("Bus: " + busName);

                    if (busMarkerBitmap != null) {
                        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(busMarkerBitmap));
                    } else {
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    }

                    Stop nextStop = findNextStop(busPos);
                    String nextStopName = (nextStop != null) ? nextStop.name : "—";
                    String snippet = (speed != null ? "Speed: " + speed + " km/h" : "Speed: —")
                            + "\nNext: " + nextStopName
                            + "\nETA: Fetching...";
                    markerOptions.snippet(snippet);

                    Marker marker = mMap.addMarker(markerOptions);
                    if (marker != null) {
                        markerMap.put(busName, marker);

                        if (nextStop != null) {
                            fetchEta(busPos, nextStop.toLatLng(), new EtaCallback() {
                                @Override
                                public void onEtaReady(String etaText) {
                                    String newSnippet = (speed != null ? "Speed: " + speed + " km/h" : "Speed: —")
                                            + "\nNext: " + nextStopName
                                            + "\nETA: " + etaText;
                                    marker.setSnippet(newSnippet);


                                    if (busName.equals(selectedBus)) {
                                        updateCardWithMarker(marker);

                                        if (marker.isInfoWindowShown()) {
                                            marker.showInfoWindow();
                                        }
                                    }
                                }

                                @Override
                                public void onError(String msg) {
                                    marker.setSnippet("Speed: " + (speed != null ? speed + " km/h" : "—")
                                            + "\nNext: " + nextStopName
                                            + "\nETA: N/A");
                                    if (busName.equals(selectedBus)) {
                                        updateCardWithMarker(marker);
                                    }
                                }
                            });
                        }


                        if (busName.equals(selectedBus)) {

                            updateCardWithMarker(marker);
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(busPos, 16.2f));

                            marker.showInfoWindow();
                        }
                    }
                }

                if (snapshot.getChildrenCount() > 0 && selectedBus == null) {
                    LatLng gietUniversity = new LatLng(19.048835, 83.833772);
                    LatLng gunupurTrends = new LatLng(19.075105, 83.809192);

                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    builder.include(gietUniversity);
                    builder.include(gunupurTrends);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MapsActivity.this, "Failed to load bus data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // -------- Helpers --------
    @SuppressLint("SetTextI18n")
    private void updateCardWithMarker(@NonNull Marker marker) {
        String busName = Objects.requireNonNull(marker.getTitle()).replace("Bus: ", "");
        String snippet = marker.getSnippet();

        cardBusName.setText("Bus: " + busName);

        String speed = "—";
        String nextStop = "—";
        String eta = "—";

        if (snippet != null) {
            String[] lines = snippet.split("\n");
            if (lines.length > 0) speed = lines[0].replace("Speed: ", "");
            if (lines.length > 1) nextStop = lines[1].replace("Next: ", "");
            if (lines.length > 2) eta = lines[2].replace("ETA: ", "");
        }

        cardSpeed.setText("Speed: " + speed);
        cardNextStop.setText("Next Bus Stop: " + nextStop);
        cardEta.setText("ETA To Next Stop: " + eta);

        busInfoCard.setVisibility(View.VISIBLE);
    }

    private static Double readDouble(DataSnapshot snap) {
        try {
            Object o = snap.getValue();
            if (o == null) return null;
            return Double.parseDouble(o.toString().trim());
        } catch (Exception e) { return null; }
    }

    private static double haversineMeters(LatLng a, LatLng b) {
        final double R = 6371000.0;
        double dLat = Math.toRadians(b.latitude - a.latitude);
        double dLng = Math.toRadians(b.longitude - a.longitude);
        double lat1 = Math.toRadians(a.latitude);
        double lat2 = Math.toRadians(b.latitude);

        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * R * Math.asin(Math.sqrt(h));
    }

    private Stop findNextStop(LatLng busPos) {
        Stop closestStop = null;
        double minDistance = Double.MAX_VALUE;
        int closestStopIndex = -1;

        for (int i = 0; i < ROUTE.length; i++) {
            double dist = haversineMeters(busPos, ROUTE[i].toLatLng());
            if (dist < minDistance) {
                minDistance = dist;
                closestStop = ROUTE[i];
                closestStopIndex = i;
            }
        }


        if (closestStopIndex < ROUTE.length - 1) {
            double distToNext = haversineMeters(busPos, ROUTE[closestStopIndex + 1].toLatLng());

            if (distToNext < minDistance) {
                return ROUTE[closestStopIndex + 1];
            }
        }


        if (closestStopIndex > 0) {
            double distToPrevious = haversineMeters(busPos, ROUTE[closestStopIndex - 1].toLatLng());

            if (distToPrevious < minDistance) {
                return ROUTE[closestStopIndex - 1];
            }
        }


        return closestStop;
    }

    private interface EtaCallback {
        void onEtaReady(String etaText);
        void onError(String msg);
    }

    @SuppressLint("StaticFieldLeak")
    private void fetchEta(LatLng origin, LatLng dest, EtaCallback cb) {
        new AsyncTask<Void, Void, String>() {
            String error;

            @Override
            protected String doInBackground(Void... voids) {
                try {
                    String originStr = URLEncoder.encode(origin.latitude + "," + origin.longitude, "UTF-8");
                    String destStr = URLEncoder.encode(dest.latitude + "," + dest.longitude, "UTF-8");

                    String urlStr = "https://maps.googleapis.com/maps/api/directions/json"
                            + "?origin=" + originStr
                            + "&destination=" + destStr
                            + "&mode=driving"
                            + "&departure_time=now"
                            + "&key=" + GOOGLE_MAPS_KEY;

                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.setRequestMethod("GET");

                    if (conn.getResponseCode() != 200) {
                        error = "HTTP " + conn.getResponseCode();
                        return null;
                    }

                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();

                    JSONObject json = new JSONObject(sb.toString());
                    if (!"OK".equals(json.optString("status"))) {
                        error = json.optString("status");
                        return null;
                    }

                    JSONArray routes = json.getJSONArray("routes");
                    if (routes.length() == 0) { error = "No route"; return null; }
                    JSONObject leg = routes.getJSONObject(0).getJSONArray("legs").getJSONObject(0);

                    JSONObject durTraffic = leg.optJSONObject("duration_in_traffic");
                    if (durTraffic != null) return durTraffic.optString("text", "—");

                    JSONObject dur = leg.optJSONObject("duration");
                    if (dur != null) return dur.optString("text", "—");

                    error = "No duration";
                    return null;

                } catch (Exception e) {
                    error = e.getMessage();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String eta) {
                if (eta != null) cb.onEtaReady(eta);
                else cb.onError(error != null ? error : "ETA error");
            }
        }.execute();
    }

    private static class Stop {
        final String name;
        final double lat, lng;
        Stop(String n, double la, double ln) { name = n; lat = la; lng = ln; }
        LatLng toLatLng() { return new LatLng(lat, lng); }
    }
}