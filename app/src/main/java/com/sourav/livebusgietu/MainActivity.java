package com.sourav.livebusgietu;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.nafis.bottomnavigation.NafisBottomNavigation;

public class MainActivity extends AppCompatActivity {

    private NafisBottomNavigation bottomNavigation;
    private ActivityResultLauncher<IntentSenderRequest> activityResultLauncher;

    private static final int TAB_HOME = 1;
    private static final int TAB_MAP = 2;
    private static final int TAB_SETTINGS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTheme(R.style.Theme_LivebusGIETU);
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        registerActivityResultLauncher();
        setContentView(R.layout.activity_main);

        // -------- STATUS BAR --------
        Window window = getWindow();
        View decorView = window.getDecorView();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        new WindowInsetsControllerCompat(window, decorView)
                .setAppearanceLightStatusBars(true);

        View root = findViewById(R.id.main);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
        }

        checkForInAppUpdate();

        try {
            FirebaseApp.initializeApp(this);
        } catch (Exception ignored) {}

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation == null) return;

        bottomNavigation.add(new NafisBottomNavigation.Model(TAB_HOME, R.drawable.ic_home));
        bottomNavigation.add(new NafisBottomNavigation.Model(TAB_MAP, R.drawable.icmap));
        bottomNavigation.add(new NafisBottomNavigation.Model(TAB_SETTINGS, R.drawable.ic_settings));

        // ✅ ONLY PLACE WHERE FRAGMENTS CHANGE
        bottomNavigation.setOnShowListener(model -> {
            showFragment(model.getId());
            return null;
        });

        // ✅ THIS IS THE FIX FOR ICON NOT SELECTED
        if (savedInstanceState == null) {
            bottomNavigation.show(TAB_HOME, true);
            bottomNavigation.postDelayed(() -> {
                bottomNavigation.invalidate();
                bottomNavigation.requestLayout();
            }, 50);

        }
    }

    // ---------------- FRAGMENT HANDLER ----------------

    private void showFragment(int tabId) {

        if (isFinishing() || isDestroyed()) return;

        FragmentManager fm = getSupportFragmentManager();
        String tag = "tab:" + tabId;

        Fragment fragment = fm.findFragmentByTag(tag);

        if (fragment == null) {
            switch (tabId) {
                case TAB_HOME:
                    fragment = new HomeFragment();
                    break;
                case TAB_MAP:
                    fragment = new MapsFragment();
                    break;
                case TAB_SETTINGS:
                    fragment = new SettingsFragment();
                    break;
                default:
                    return;
            }
        }

        FragmentTransaction tx = fm.beginTransaction();

        for (Fragment f : fm.getFragments()) {
            tx.hide(f);
        }

        if (!fragment.isAdded()) {
            tx.add(R.id.fragment_container, fragment, tag);
        } else {
            tx.show(fragment);
        }

        tx.setReorderingAllowed(true);
        tx.commit();
    }

    // ---------------- MAP NAVIGATION ----------------

    public void navigateToMapAndTrackBus(String busId) {
        bottomNavigation.show(TAB_MAP, true);
    }

    // ---------------- IN-APP UPDATE ----------------

    private void checkForInAppUpdate() {
        AppUpdateManager manager = AppUpdateManagerFactory.create(this);
        manager.getAppUpdateInfo().addOnSuccessListener(info -> {
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                try {
                    manager.startUpdateFlowForResult(
                            info,
                            activityResultLauncher,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    );
                } catch (Exception ignored) {}
            }
        });
    }

    private void registerActivityResultLauncher() {
        activityResultLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartIntentSenderForResult(),
                        result -> {
                            if (result.getResultCode() != RESULT_OK) {
                                Toast.makeText(this, "Update failed!", Toast.LENGTH_SHORT).show();
                            }
                        }
                );
    }
}
