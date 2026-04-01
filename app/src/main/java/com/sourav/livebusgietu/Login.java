package com.sourav.livebusgietu;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Login extends AppCompatActivity {

    LinearLayout google_btn;
    SignInClient oneTapClient;
    BeginSignInRequest signInRequest;
    private LoadingDialog loadingDialog;
    FirebaseAuth firebaseAuth;
    FirebaseDatabase database;
    AppCompatButton sign_in;
    EditText numberinput;

    private final ActivityResultLauncher<IntentSenderRequest> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    try {
                        SignInCredential credential = Identity.getSignInClient(this)
                                .getSignInCredentialFromIntent(result.getData());
                        String idToken = credential.getGoogleIdToken();
                        if (idToken != null) {
                            firebaseAuthWithGoogle(idToken);
                        } else {
                            Toast.makeText(this, "Google Sign-In failed: No ID token", Toast.LENGTH_SHORT).show();
                            loadingDialog.dismissDialog();
                        }
                    } catch (ApiException e) {
                        Toast.makeText(this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        loadingDialog.dismissDialog();
                    }
                } else {
                    loadingDialog.dismissDialog();
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_login);

        Window window = getWindow();
        View decorView = window.getDecorView();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        WindowInsetsControllerCompat insetsController = new WindowInsetsControllerCompat(window, decorView);
        insetsController.setAppearanceLightStatusBars(true);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.POST_NOTIFICATIONS"}, 101);
        }

        firebaseAuth = FirebaseAuth.getInstance();
        SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
        String loginMethod = prefs.getString("login_method", "unknown");

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null && !loginMethod.equals("unknown")) {
            startActivity(new Intent(Login.this, MainActivity.class));
            finish();
            return;
        }

        loadingDialog = new LoadingDialog(Login.this);
        google_btn = findViewById(R.id.google_btn);
        numberinput = findViewById(R.id.numberinput);
        LinearLayout layout = findViewById(R.id.inputlayout);
        numberinput.setOnFocusChangeListener((v, hasFocus) -> layout.setActivated(hasFocus));

        sign_in = findViewById(R.id.sign_in);
        database = FirebaseDatabase.getInstance();
        String webClientId = getString(R.string.web_client_id);

        oneTapClient = Identity.getSignInClient(this);
        signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                                .setSupported(true)
                                .setServerClientId(webClientId)
                                .setFilterByAuthorizedAccounts(false)
                                .build())
                .build();

        sign_in.setOnClickListener(v -> {
            String mobile = numberinput.getText().toString().trim();
            if (!mobile.matches("[6-9][0-9]{9}")) {
                numberinput.setError("Invalid Indian mobile number");
                numberinput.requestFocus();
                return;
            }

            loadingDialog.startLoadingDiloag();
            PhoneAuthOptions options = PhoneAuthOptions.newBuilder(firebaseAuth)
                    .setPhoneNumber("+91" + mobile)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this)
                    .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        @Override
                        public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {}

                        @Override
                        public void onVerificationFailed(@NonNull FirebaseException e) {
                            loadingDialog.dismissDialog();
                            Toast.makeText(Login.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                            loadingDialog.dismissDialog();

                            Intent intent = new Intent(Login.this, OtpActivity.class);
                            intent.putExtra("mobile", mobile);
                            intent.putExtra("backendotp", verificationId);
                            startActivity(intent);
                        }
                    })
                    .build();
            PhoneAuthProvider.verifyPhoneNumber(options);
        });

        google_btn.setOnClickListener(v -> {
            loadingDialog.startLoadingDiloag();
            oneTapClient.beginSignIn(signInRequest)
                    .addOnSuccessListener(result -> {
                        IntentSender intentSender = result.getPendingIntent().getIntentSender();
                        IntentSenderRequest request = new IntentSenderRequest.Builder(intentSender).build();
                        googleSignInLauncher.launch(request);
                    })
                    .addOnFailureListener(e -> {
                        loadingDialog.dismissDialog();
                        Toast.makeText(Login.this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View view = getCurrentFocus();
            if (view instanceof EditText) {
                Rect outRect = new Rect();
                view.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    view.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
                        prefs.edit().putString("login_method", "phone").apply();

                        if (user != null) {
                            HashMap<String, String> map = new HashMap<>();
                            map.put("id", user.getUid());
                            map.put("name", user.getDisplayName() != null ? user.getDisplayName() : "User");
                            map.put("email", user.getEmail() != null ? user.getEmail() : "No Email");
                            map.put("profileImage", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
                            database.getReference().child("users").child(user.getUid()).updateChildren((Map) map);
                            Toast.makeText(Login.this, "Google Sign-In Successful", Toast.LENGTH_SHORT).show();
                            loadingDialog.dismissDialog();
                            sendLoginNotification();
                            startActivity(new Intent(Login.this, MainActivity.class));
                            finish();
                        }
                    } else {
                        loadingDialog.dismissDialog();
                        Toast.makeText(Login.this, "Firebase Authentication Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendLoginNotification() {
        String channelId = "LOGIN_CHANNEL";
        String channelName = "Login Notifications";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.directionsbus)
                .setContentTitle("Welcome To Livebus")
                .setContentText("✅ Track your ride, save your time-Livebus by GIETU.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(1, builder.build());
    }

    @Override
    protected void onDestroy() {
        if (loadingDialog != null) {
            loadingDialog.dismissDialog();
        }
        super.onDestroy();
    }
}