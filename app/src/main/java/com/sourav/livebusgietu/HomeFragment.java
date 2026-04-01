package com.sourav.livebusgietu;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class HomeFragment extends Fragment {

    private TextView tvTitle;
    private TextView tvBusId, tvRoute, tvStatus;
    private TextView tvSpeedVal;
    private TextView tvCurrentStop, tvNextStop, tvEta;
    private TextView tvDestination; // id: destination
    private TextView tvFuel, tvCoordinates;
    private BusData lastShownBus;

    private LoadingDialog loadingDialog;

    private DatabaseReference busDataRef;
    private ValueEventListener busDataListener;

    private final List<BusData> busList = new ArrayList<>();

    private final Map<String, BusState> busStateMap = new HashMap<>();
    private BusData currentPrimaryBus;

    private static final long OFFLINE_THRESHOLD_MS = 15* 60 * 1000L;
    private static final long ONTIME_MINUTES = 2;
    private static final long DELAYED_MINUTES = 10;

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
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvSubtitle = view.findViewById(R.id.tvSubtitle);
        tvBusId = view.findViewById(R.id.tvBusId);
        tvRoute = view.findViewById(R.id.tvRoute);
        tvStatus = view.findViewById(R.id.tvStatus);
        LottieAnimationView lottieLiveBus = view.findViewById(R.id.lottieLiveBus);
        TextView tvGpsState = view.findViewById(R.id.tvGpsState);
        tvSpeedVal = view.findViewById(R.id.tvSpeedVal);
        tvCurrentStop = view.findViewById(R.id.tvCurrentStop);
        tvNextStop = view.findViewById(R.id.tvNextStop);
        tvEta = view.findViewById(R.id.tvEta);
        tvDestination = view.findViewById(R.id.destination);
        tvFuel = view.findViewById(R.id.tvFuel);
        tvCoordinates = view.findViewById(R.id.tvCoordinates);
        TextView tvAlertTitle = view.findViewById(R.id.tvAlertTitle);
        TextView tvAlertBody = view.findViewById(R.id.tvAlertBody);

        // instantiate your custom LoadingDialog with Activity
        Activity act = requireActivity();
        loadingDialog = new LoadingDialog(act);

        if (tvTitle != null && TextUtils.isEmpty(tvTitle.getText())) tvTitle.setText("Live Bus Tracker");
        if (tvSubtitle != null && TextUtils.isEmpty(tvSubtitle.getText())) tvSubtitle.setText("Real-time monitoring");
        if (tvBusId != null) tvBusId.setText("BUS08");
        if (tvRoute != null) tvRoute.setText("Route 42A");
        if (tvStatus != null) tvStatus.setText("Active");
        if (tvGpsState != null) tvGpsState.setText("Connected");
        if (tvSpeedVal != null) tvSpeedVal.setText("0 km/h");
        if (tvCurrentStop != null) tvCurrentStop.setText("—");
        if (tvNextStop != null) tvNextStop.setText("—");
        if (tvEta != null) tvEta.setText("ETA: —");
        if (tvDestination != null) tvDestination.setText("Direction: —");
        if (tvFuel != null) tvFuel.setText("—");
        if (tvCoordinates != null) tvCoordinates.setText("—");
        if (tvAlertTitle != null) tvAlertTitle.setText("System Alert");
        if (tvAlertBody != null) tvAlertBody.setText("All systems operational. No issues detected.");

        if (lottieLiveBus != null) {
            lottieLiveBus.setOnClickListener(v -> {
                if (currentPrimaryBus != null && getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToMapAndTrackBus(currentPrimaryBus.getId());
                } else {
                    Toast.makeText(requireContext(), "No active bus to track.", Toast.LENGTH_SHORT).show();
                }
            });
        }


        busDataRef = FirebaseDatabase.getInstance().getReference("busdata");


        try {
            loadingDialog.startLoadingDiloag();
        } catch (Exception ignored) { /* defensive - if dialog fails, continue without crash */ }

        attachBusDataListener();

        // greeting
        fetchAndSetUserGreeting();
    }

    private void attachBusDataListener() {
        if (busDataRef == null) {
            try { loadingDialog.dismissDialog(); } catch (Exception ignored) {}
            return;
        }

        busDataListener = new ValueEventListener() {
            boolean firstLoadDone = false;

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                busList.clear();
                long now = System.currentTimeMillis();

                for (DataSnapshot child : snapshot.getChildren()) {
                    BusData b = parseBusData(child);
                    if (b == null) continue;
                    BusState prev = busStateMap.get(b.getId());

                    int detectedDirection = 0; // 1 = toward higher index, -1 = toward lower index
                    if (prev != null) {
                        // compute bearing from prev -> current
                        double moveBearing = bearing(prev.lat, prev.lng, b.getLat(), b.getLng());
                        int currIdx = getClosestStopIndex(b.getLat(), b.getLng());
                        if (currIdx != -1) {
                            double bestAngle = Double.MAX_VALUE;
                            int bestDir = 0;
                            // check next neighbor
                            if (currIdx < ROUTE.length - 1) {
                                double candBearing = bearing(b.getLat(), b.getLng(), ROUTE[currIdx + 1].lat, ROUTE[currIdx + 1].lng);
                                double diff = smallestAngleBetween(moveBearing, candBearing);
                                if (Math.abs(diff) < Math.abs(bestAngle)) { bestAngle = diff; bestDir = 1; }
                            }
                            // check previous neighbor
                            if (currIdx > 0) {
                                double candBearing = bearing(b.getLat(), b.getLng(), ROUTE[currIdx - 1].lat, ROUTE[currIdx - 1].lng);
                                double diff = smallestAngleBetween(moveBearing, candBearing);
                                if (Math.abs(diff) < Math.abs(bestAngle)) { bestAngle = diff; bestDir = -1; }
                            }
                            detectedDirection = bestDir; // may be 0
                        }
                    }

                    if (detectedDirection == 0 && prev != null) detectedDirection = prev.lastDirection;


                    BusState nextState = new BusState(b.getLat(), b.getLng(), b.getTimestamp(), now, detectedDirection);
                    busStateMap.put(b.getId(), nextState);

                    busList.add(b);
                }


                currentPrimaryBus = selectPrimaryBus(busList);

                requireActivity().runOnUiThread(() -> {
                    if (currentPrimaryBus != null) {
                        lastShownBus = currentPrimaryBus;
                        updatePrimaryCardUI(currentPrimaryBus);
                    } else if (lastShownBus != null) {
                        updatePrimaryCardUI(lastShownBus);
                    }


                });


                if (!firstLoadDone) {
                    try { loadingDialog.dismissDialog(); } catch (Exception ignored) {}
                    firstLoadDone = true;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                try { loadingDialog.dismissDialog(); } catch (Exception ignored) {}
                Toast.makeText(requireContext(), "Failed to load bus data.", Toast.LENGTH_SHORT).show();
            }
        };

        busDataRef.addValueEventListener(busDataListener);
    }


    @SuppressLint("SetTextI18n")
    private void updatePrimaryCardUI(@NonNull BusData bus) {

        if (tvBusId != null)
            tvBusId.setText(bus.getName() != null ? bus.getName() : bus.getId());

        if (tvRoute != null)
            tvRoute.setText(bus.getRoute() != null ? bus.getRoute() : "Route not available");

        // ---------------- STATUS (FIREBASE + INDIA TIME) ----------------
        long now = System.currentTimeMillis();
        long ts = bus.getTimestamp();
        String statusText;

        if (ts <= 0) {
            statusText = "Active";
        } else {

            long diffMs = Math.abs(now - ts);
            long diffMin = diffMs / (60 * 1000);

            if (diffMin <= 6) {
                statusText = "Active";
            } else if (diffMin <= 30) {
                statusText = "Last seen " + diffMin + " min ago";
            } else {
                statusText = "Offline";
            }
        }

        if (tvStatus != null)
            tvStatus.setText(statusText);


        // ---------------- SPEED & LOCATION ----------------
        if (tvSpeedVal != null)
            tvSpeedVal.setText(String.format(Locale.getDefault(), "%.0f km/h", bus.getSpeed()));

        if (tvCoordinates != null)
            tvCoordinates.setText(
                    String.format(Locale.getDefault(), "%.5f, %.5f", bus.getLat(), bus.getLng())
            );

        // ---------------- CURRENT & NEXT STOP ----------------
        CurrentNext cn = computeCurrentAndNextStopsImproved(bus);

        if (tvCurrentStop != null) {
            tvCurrentStop.setText(
                    cn.currentIndex >= 0 ? ROUTE[cn.currentIndex].name : "—"
            );
        }

        if (cn.nextIndex >= 0) {
            if (tvNextStop != null)
                tvNextStop.setText(ROUTE[cn.nextIndex].name);

            double distKm = calculateDistance(
                    bus.getLat(), bus.getLng(),
                    ROUTE[cn.nextIndex].lat, ROUTE[cn.nextIndex].lng
            );

            if (bus.getSpeed() > 1.0) {
                long mins = Math.max(1, Math.round((distKm / bus.getSpeed()) * 60));
                if (tvEta != null) tvEta.setText("ETA: ~" + mins + " mins");
            } else {
                if (tvEta != null) tvEta.setText("ETA: Bus stopped");
            }

        } else {
            if (tvNextStop != null) tvNextStop.setText("—");
            if (tvEta != null) tvEta.setText("ETA: —");
        }

        // ---------------- DIRECTION ----------------
        String directionText;
        BusState st = busStateMap.get(bus.getId());

        if (st != null && st.lastDirection != 0) {
            directionText = (st.lastDirection > 0)
                    ? ROUTE[0].name + " → " + ROUTE[ROUTE.length - 1].name
                    : ROUTE[ROUTE.length - 1].name + " → " + ROUTE[0].name;
        } else {
            double dToGIET = calculateDistance(bus.getLat(), bus.getLng(),
                    ROUTE[0].lat, ROUTE[0].lng);
            double dToOld = calculateDistance(bus.getLat(), bus.getLng(),
                    ROUTE[ROUTE.length - 1].lat, ROUTE[ROUTE.length - 1].lng);

            directionText = dToGIET < dToOld
                    ? ROUTE[ROUTE.length - 1].name + " → " + ROUTE[0].name
                    : ROUTE[0].name + " → " + ROUTE[ROUTE.length - 1].name;
        }

        if (tvDestination != null)
            tvDestination.setText(directionText);

        // ---------------- FUEL ----------------
        if (tvFuel != null)
            tvFuel.setText(simulateFuelForBus(bus.getId()) + "%");
    }


    private CurrentNext computeCurrentAndNextStopsImproved(BusData b) {
        int currIdx = getClosestStopIndex(b.getLat(), b.getLng());
        if (currIdx == -1) return new CurrentNext(-1, -1);

        BusState prev = busStateMap.get(b.getId());
        int chosenNext = -1;

        if (prev != null) {
            double[] moveVec = toMetersVector(prev.lat, prev.lng, b.getLat(), b.getLng());

            double bestScore = Double.NEGATIVE_INFINITY;
            int bestDir = 0;

            if (currIdx < ROUTE.length - 1) {
                double[] candVec = toMetersVector(b.getLat(), b.getLng(), ROUTE[currIdx + 1].lat, ROUTE[currIdx + 1].lng);
                double dot = dotProduct(moveVec, candVec);
                double score = normalizeDot(moveVec, candVec, dot);
                if (score > bestScore) { bestScore = score; bestDir = 1; }
            }

            if (currIdx > 0) {
                double[] candVec = toMetersVector(b.getLat(), b.getLng(), ROUTE[currIdx - 1].lat, ROUTE[currIdx - 1].lng);
                double dot = dotProduct(moveVec, candVec);
                double score = normalizeDot(moveVec, candVec, dot);
                if (score > bestScore) { bestScore = score; bestDir = -1; }
            }

            if (bestScore > 0.0) {
                if (bestDir == 1) chosenNext = currIdx + 1;
                else if (currIdx > 0) chosenNext = currIdx - 1;
                busStateMap.put(b.getId(), new BusState(b.getLat(), b.getLng(), b.getTimestamp(), System.currentTimeMillis(), bestDir));
            } else {
                chosenNext = neighborDistanceHeuristicAndStore(b, currIdx);
            }
        } else {
            chosenNext = neighborDistanceHeuristicAndStore(b, currIdx);
        }

        if (chosenNext == currIdx) chosenNext = -1;
        if (chosenNext < 0 || chosenNext >= ROUTE.length) chosenNext = -1;
        return new CurrentNext(currIdx, chosenNext);
    }

    private double[] toMetersVector(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double meanLat = Math.toRadians((lat1 + lat2) / 2.0);
        double x = dLon * Math.cos(meanLat) * R;
        double y = dLat * R;
        return new double[]{x, y};
    }

    private double dotProduct(double[] a, double[] b) {
        return a[0]*b[0] + a[1]*b[1];
    }

    private double normalizeDot(double[] a, double[] b, double rawDot) {
        double magA = Math.hypot(a[0], a[1]);
        double magB = Math.hypot(b[0], b[1]);
        if (magA == 0 || magB == 0) return -1.0;
        double cos = rawDot / (magA * magB);
        double weight = Math.min(magA, magB);
        return cos * (1.0 + Math.min(weight / 50.0, 0.5));
    }

    private int neighborDistanceHeuristicAndStore(BusData b, int currIdx) {
        int chosenNext = -1;
        double distToNextNeighbor = Double.MAX_VALUE;
        double distToPrevNeighbor = Double.MAX_VALUE;
        if (currIdx < ROUTE.length - 1)
            distToNextNeighbor = calculateDistance(b.getLat(), b.getLng(), ROUTE[currIdx + 1].lat, ROUTE[currIdx + 1].lng);
        if (currIdx > 0)
            distToPrevNeighbor = calculateDistance(b.getLat(), b.getLng(), ROUTE[currIdx - 1].lat, ROUTE[currIdx - 1].lng);

        if (distToNextNeighbor < distToPrevNeighbor && currIdx < ROUTE.length - 1) {
            chosenNext = currIdx + 1;
            busStateMap.put(b.getId(), new BusState(b.getLat(), b.getLng(), b.getTimestamp(), System.currentTimeMillis(), 1));
        } else if (currIdx > 0) {
            chosenNext = currIdx - 1;
            busStateMap.put(b.getId(), new BusState(b.getLat(), b.getLng(), b.getTimestamp(), System.currentTimeMillis(), -1));
        } else {
            chosenNext = -1;
            busStateMap.put(b.getId(), new BusState(b.getLat(), b.getLng(), b.getTimestamp(), System.currentTimeMillis(), 0));
        }
        return chosenNext;
    }

    private double bearing(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double lambda1 = Math.toRadians(lon1);
        double lambda2 = Math.toRadians(lon2);
        double y = Math.sin(lambda2 - lambda1) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
        double theta = Math.atan2(y, x);
        double bearing = Math.toDegrees(theta);
        return (bearing + 360.0) % 360.0;
    }

    private double smallestAngleBetween(double a, double b) {
        double diff = (b - a + 540.0) % 360.0 - 180.0;
        return diff;
    }

    private BusData selectPrimaryBus(List<BusData> all) {
        if (all == null || all.isEmpty()) return null;

        // Always return first available bus
        return all.get(0);
    }


    private CurrentNext computeCurrentAndNextStops(BusData b) { return computeCurrentAndNextStopsImproved(b); }

    private int getClosestStopIndex(double lat, double lng) {
        int best = -1;
        double min = Double.MAX_VALUE;
        for (int i = 0; i < ROUTE.length; i++) {
            double d = calculateDistance(lat, lng, ROUTE[i].lat, ROUTE[i].lng);
            if (d < min) { min = d; best = i; }
        }
        return best;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        if (lat1 == lat2 && lon1 == lon2) return 0;
        double theta = lon1 - lon2;
        double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2))
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
        dist = Math.acos(Math.max(-1.0, Math.min(1.0, dist)));
        dist = Math.toDegrees(dist);
        dist = dist * 60 * 1.1515 * 1.609344;
        return dist;
    }

    private BusData parseBusData(DataSnapshot snap) {
        try {
            String id = snap.getKey();
            if (id == null) return null;
            String name = snap.child("name").exists() ? snap.child("name").getValue(String.class) : id;
            String route = snap.child("route").exists() ? snap.child("route").getValue(String.class) : "Route not available";

            Object latObj = snap.child("lat").getValue();
            Object lngObj = snap.child("lng").getValue();
            if (lngObj == null) lngObj = snap.child("log").getValue(); // fallback typo "log"
            Object speedObj = snap.child("speed").getValue();

            if (latObj == null || lngObj == null) {
                // fallback to last known or 0
                latObj = 0.0;
                lngObj = 0.0;
            }


            double lat = parseDouble(latObj);
            double lng = parseDouble(lngObj);
            double speed = speedObj != null ? parseDouble(speedObj) : 0.0;

            long timestamp = System.currentTimeMillis(); // fallback

            Object ts = snap.child("timestamp").getValue();
            if (ts instanceof Long) {
                timestamp = (Long) ts;
                if (String.valueOf(timestamp).length() < 13) timestamp *= 1000L;
            } else if (ts instanceof Integer) {
                timestamp = ((Integer) ts).longValue() * 1000L;
            } else if (ts instanceof String) {
                String s = (String) ts;
                if (s.toLowerCase().contains("am") || s.toLowerCase().contains("pm")) {
                    timestamp = parseTimeToTodayMillis(s);
                } else {
                    try {
                        long parsed = Long.parseLong(s);
                        if (String.valueOf(parsed).length() < 13) parsed *= 1000L;
                        timestamp = parsed;
                    } catch (NumberFormatException e) {
                        timestamp = parseTimeToTodayMillis(s);
                    }
                }
            }

            return new BusData(id, name, route, lat, lng, speed, timestamp);
        } catch (Exception e) {
            return null;
        }
    }

    private double parseDouble(Object o) {
        if (o == null) return 0.0;
        try {
            if (o instanceof Double) return (Double) o;
            if (o instanceof Float) return ((Float) o).doubleValue();
            if (o instanceof Long) return ((Long) o).doubleValue();
            if (o instanceof Integer) return ((Integer) o).doubleValue();
            return Double.parseDouble(o.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private long parseTimeToTodayMillis(String timeString) {
        try {
            // Firebase format: "11:33 PM"
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));

            Date timePart = sdf.parse(timeString);
            if (timePart == null) return System.currentTimeMillis();

            Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
            Calendar t = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
            t.setTime(timePart);

            // Apply today’s date
            t.set(Calendar.YEAR, now.get(Calendar.YEAR));
            t.set(Calendar.MONTH, now.get(Calendar.MONTH));
            t.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

            // 🚨 INDIA FIX:
            // If parsed time is in the FUTURE, subtract 1 day
            if (t.after(now)) {
                t.add(Calendar.DAY_OF_MONTH, -1);
            }

            return t.getTimeInMillis();

        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }


    private int simulateFuelForBus(String busId) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int minutes = hour * 60 + minute;
        int start = 6 * 60, end = 20 * 60;
        if (minutes <= start) return 100;
        if (minutes >= end) return 10;
        double frac = (double) (minutes - start) / (end - start);
        double raw = 100.0 - frac * 90.0;
        int jitter = Math.abs(busId != null ? busId.hashCode() : 1) % 7 - 3;
        raw += jitter * 0.7;
        return (int) Math.max(10, Math.min(100, Math.round(raw)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (busDataRef != null && busDataListener != null) busDataRef.removeEventListener(busDataListener);
        try { if (loadingDialog != null) loadingDialog.dismissDialog(); } catch (Exception ignored) {}
    }

    // ------------------ helper classes ------------------

    private static class Stop {
        final String name;
        final double lat, lng;
        Stop(String name, double lat, double lng) { this.name = name; this.lat = lat; this.lng = lng; }
    }

    private static class CurrentNext {
        final int currentIndex, nextIndex;
        CurrentNext(int c, int n) { this.currentIndex = c; this.nextIndex = n; }
    }

    public static class BusData {
        private final String id, name, route;
        private final double lat, lng, speed;
        private final long timestamp;
        public BusData(String id, String name, String route, double lat, double lng, double speed, long timestamp) {
            this.id = id; this.name = name; this.route = route; this.lat = lat; this.lng = lng; this.speed = speed; this.timestamp = timestamp;
        }
        public String getId() { return id; }
        public String getName() { return name; }
        public String getRoute() { return route; }
        public double getLat() { return lat; }
        public double getLng() { return lng; }
        public double getSpeed() { return speed; }
        public long getTimestamp() { return timestamp; }
    }

    private static class BusState {
        final double lat, lng;
        final long lastTimestamp;
        final long lastSeenAt;
        final int lastDirection;
        BusState(double lat, double lng, long lastTimestamp, long lastSeenAt, int lastDirection) {
            this.lat = lat; this.lng = lng; this.lastTimestamp = lastTimestamp; this.lastSeenAt = lastSeenAt; this.lastDirection = lastDirection;
        }
    }


    private void fetchAndSetUserGreeting() {
        final String fallback = "Student";
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { setTitleGreeting(fallback); return; }
        String name = user.getDisplayName();
        if (name != null && !name.trim().isEmpty()) { setTitleGreeting(name.trim().split("\\s+")[0]); return; }
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(user.getUid()).child("name");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                String full = snapshot.exists() ? snapshot.getValue(String.class) : null;
                if (full != null && !full.trim().isEmpty()) setTitleGreeting(full.trim().split("\\s+")[0]);
                else setTitleGreeting(fallback);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { setTitleGreeting(fallback); }
        });
    }

    @SuppressLint("SetTextI18n")
    private void setTitleGreeting(@NonNull String firstName) {
        if (tvTitle != null) tvTitle.setText("LiveBus — Hello, " + firstName + "!");
    }
}
