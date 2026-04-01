package com.sourav.livebusgietu;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MapsFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "MapFragment";

    private static final String GOOGLE_MAPS_KEY = "YOAIzaSyDjSbAUDfxk4361JoDYrv8Wm-eVfstpLEw";


    private GoogleMap mMap;
    private CardView busInfoCard;
    private TextView cardBusName, cardSpeed, cardNextStop, cardEta;
    private FloatingActionButton fabRecenter;

    // --- Map & Data ---
    private Bitmap busMarkerBitmap, stopMarkerBitmap;
    private String selectedBusId = null;
    private final Map<String, Marker> busMarkers = new ConcurrentHashMap<>();
    private LatLngBounds routeBounds;
    private ValueEventListener firebaseListener;
    private DatabaseReference busesRef;


    // --- Route Definition ---
    private static final Stop[] ROUTE = new Stop[]{
            new Stop("GIET Bus Stand", 19.048835, 83.833772),
            new Stop("Gunupur College", 19.059997, 83.823377),
            new Stop("SBI Road", 19.063734, 83.820572),
            new Stop("BN", 19.068345, 83.816426),
            new Stop("Bypass", 19.069816, 83.815185),
            new Stop("JJ", 19.071308, 83.813032),
            new Stop("Old Gunupur", 19.071978, 83.810045)
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.activity_maps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        busInfoCard = view.findViewById(R.id.busInfoCard);
        cardBusName = view.findViewById(R.id.tvBusName);
        cardSpeed = view.findViewById(R.id.tvSpeed);
        cardNextStop = view.findViewById(R.id.tvNextStop);
        cardEta = view.findViewById(R.id.tvETA);
        fabRecenter = view.findViewById(R.id.fab_recenter);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        applyMapStyle();
        prepareMarkers();
        setupMapInteractions();

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Stop stop : ROUTE) {
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(stop.toLatLng())
                    .title(stop.name)
                    .icon(BitmapDescriptorFactory.fromBitmap(stopMarkerBitmap));
            mMap.addMarker(markerOptions);
            builder.include(stop.toLatLng());
        }
        routeBounds = builder.build();

        if (getView() != null) {
            getView().post(() -> mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(routeBounds, 100)));
        }

        setupFirebaseBusListener();
    }

    private void applyMapStyle() {
        try {
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style_silver));
            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }
    }

    private void prepareMarkers() {
        Context context = getContext();
        if (context == null) return;

        BitmapDrawable stopDrawable = (BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.maps);
        BitmapDrawable busDrawable = (BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.busstop);

        if (stopDrawable != null) {
            Bitmap b = stopDrawable.getBitmap();
            stopMarkerBitmap = Bitmap.createScaledBitmap(b, 80, 80, false);
        }

        if (busDrawable != null) {
            Bitmap b = busDrawable.getBitmap();
            busMarkerBitmap = Bitmap.createScaledBitmap(b, 100, 100, false);
        }
    }

    private void setupMapInteractions() {
        mMap.setOnMarkerClickListener(marker -> {
            if (marker.getTag() instanceof BusData busData) {
                selectedBusId = busData.id;
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 16.5f), 500, null);
                updateCard(busData);
                return true;
            }
            return false;
        });

        mMap.setOnMapClickListener(latLng -> {
            busInfoCard.setVisibility(View.GONE);
            selectedBusId = null;
        });

        // =================================================================
        // THE CRASH FIX IS HERE
        // This 'if' statement adds a safety check.
        // =================================================================
        if (fabRecenter != null) {
            fabRecenter.setOnClickListener(v -> {
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(routeBounds, 100));
                busInfoCard.setVisibility(View.GONE);
                selectedBusId = null;
            });
        } else {
            // This will only show up in your Logcat if the button isn't found.
            // The app will NOT crash.
            Log.e(TAG, "FloatingActionButton 'fab_recenter' could not be found. Check your activity_maps.xml file.");
        }
    }


    private void setupFirebaseBusListener() {
        busesRef = FirebaseDatabase.getInstance().getReference("busdata");
        firebaseListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot busSnapshot : snapshot.getChildren()) {
                    BusData busData = parseBusData(busSnapshot);
                    if (busData != null) {
                        updateBusOnMap(busData);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load bus data", Toast.LENGTH_SHORT).show();
                }
            }
        };
        busesRef.addValueEventListener(firebaseListener);
    }

    private void updateBusOnMap(BusData busData) {
        if (mMap == null) return;
        LatLng busPos = new LatLng(busData.lat, busData.lng);
        Marker marker = busMarkers.get(busData.id);

        if (marker == null) {
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(busPos)
                    .title(busData.id)
                    .icon(BitmapDescriptorFactory.fromBitmap(busMarkerBitmap));
            marker = mMap.addMarker(markerOptions);
            busMarkers.put(busData.id, marker);
        } else {
            marker.setPosition(busPos);
        }

        assert marker != null;
        marker.setTag(busData);

        if (busData.id.equals(selectedBusId)) {
            updateCard(busData);
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateCard(BusData busData) {
        cardBusName.setText("Bus: " + busData.id);
        cardSpeed.setText(String.format("%.1f km/h", busData.speed));

        Stop nextStop = findNextStop(new LatLng(busData.lat, busData.lng));
        if (nextStop != null) {
            cardNextStop.setText(nextStop.name);
            fetchEta(new LatLng(busData.lat, busData.lng), nextStop.toLatLng(), new EtaCallback() {
                @Override
                public void onEtaReady(String etaText) {
                    cardEta.setText(etaText);
                }

                @Override
                public void onError(String msg) {
                    cardEta.setText("N/A");
                }
            });
        } else {
            cardNextStop.setText("End of Route");
            cardEta.setText("—");
        }
        busInfoCard.setVisibility(View.VISIBLE);
    }

    private BusData parseBusData(DataSnapshot snapshot) {
        try {
            String id = snapshot.getKey();
            if (id == null) return null;

            Double lat = readDouble(snapshot.child("lat"));
            Double lng = readDouble(snapshot.child("lng"));
            Double speed = readDouble(snapshot.child("speed"));

            if (lat == null || lng == null || speed == null) return null;

            return new BusData(id, lat, lng, speed);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing bus data for " + snapshot.getKey(), e);
            return null;
        }
    }


    private static Double readDouble(DataSnapshot snap) {
        try {
            Object o = snap.getValue();
            return o != null ? Double.parseDouble(o.toString().trim()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Stop findNextStop(LatLng busPos) {
        if (ROUTE.length < 2) return null;

        int closestStopIndex = -1;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < ROUTE.length; i++) {
            double dist = haversineMeters(busPos, ROUTE[i].toLatLng());
            if (dist < minDistance) {
                minDistance = dist;
                closestStopIndex = i;
            }
        }

        double distToStart = haversineMeters(busPos, ROUTE[0].toLatLng());
        double distToEnd = haversineMeters(busPos, ROUTE[ROUTE.length - 1].toLatLng());

        if (distToEnd < distToStart) {
            if (closestStopIndex < ROUTE.length - 1) {
                return ROUTE[closestStopIndex + 1];
            } else {
                return ROUTE[closestStopIndex];
            }
        } else {
            if (closestStopIndex > 0) {
                return ROUTE[closestStopIndex - 1];
            } else {
                return ROUTE[closestStopIndex];
            }
        }
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
                            + "&destination=" +destStr
                            + "&key=" + GOOGLE_MAPS_KEY;

                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

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
                    return json.getJSONArray("routes").getJSONObject(0)
                            .getJSONArray("legs").getJSONObject(0)
                            .getJSONObject("duration").getString("text");

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

        Stop(String n, double la, double ln) {
            name = n;
            lat = la;
            lng = ln;
        }

        LatLng toLatLng() {
            return new LatLng(lat, lng);
        }
    }

    private static class BusData {
        final String id;
        final double lat, lng, speed;

        BusData(String id, double lat, double lng, double speed) {
            this.id = id;
            this.lat = lat;
            this.lng = lng;
            this.speed = speed;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Important: Remove the listener to prevent memory leaks
        if (busesRef != null && firebaseListener != null) {
            busesRef.removeEventListener(firebaseListener);
        }
    }
}

