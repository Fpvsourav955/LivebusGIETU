package com.sourav.livebusgietu;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.NotificationCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class OtpActivity extends AppCompatActivity {

    EditText inputnumber1, inputnumber2, inputnumber3, inputnumber4, inputnumber5, inputnumber6;
    String getotpbackend;
    LoadingDialog loadingDialog;
    AppCompatButton Resendlabl, Verify;
    TextView resendTimer, phoneText,otpErrorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_otp);
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
        FirebaseApp.initializeApp(this);

        loadingDialog = new LoadingDialog(this);
        ImageView otpback= findViewById(R.id.otpback);
        otpback.setOnClickListener(v-> finish());
        inputnumber1 = findViewById(R.id.input1);
        inputnumber2 = findViewById(R.id.input2);
        inputnumber3 = findViewById(R.id.input3);
        inputnumber4 = findViewById(R.id.input4);
        inputnumber5 = findViewById(R.id.input5);
        inputnumber6 = findViewById(R.id.input6);
        otpErrorText = findViewById(R.id.otpErrorText);

        resendTimer = findViewById(R.id.resendTimerTextView);
        Resendlabl = findViewById(R.id.resendBtn);
        Verify = findViewById(R.id.verifyBtn);
        phoneText = findViewById(R.id.phoneText);
        phoneText.setText(String.format("+91%s", getIntent().getStringExtra("mobile")));

        getotpbackend = getIntent().getStringExtra("backendotp");
        startResendCountdown();



        if (getotpbackend == null) {
            Toast.makeText(this, "Error: OTP backend not received", Toast.LENGTH_LONG).show();
            finish();
        }

        numbertomove();

        Verify.setOnClickListener(v -> {
            String code = inputnumber1.getText().toString().trim() +
                    inputnumber2.getText().toString().trim() +
                    inputnumber3.getText().toString().trim() +
                    inputnumber4.getText().toString().trim() +
                    inputnumber5.getText().toString().trim() +
                    inputnumber6.getText().toString().trim();

            if (code.length() != 6) {
                Toast.makeText(OtpActivity.this, "Please enter all 6 digits", Toast.LENGTH_SHORT).show();
                return;
            }

            loadingDialog.startLoadingDiloag();
            PhoneAuthCredential phoneAuthCredential = PhoneAuthProvider.getCredential(getotpbackend, code);
            FirebaseAuth.getInstance().signInWithCredential(phoneAuthCredential).addOnCompleteListener(task -> {
                loadingDialog.dismissDialog();
                if (task.isSuccessful()) {
                    sendLoginNotification();

                    getSharedPreferences("userPrefs", MODE_PRIVATE)
                            .edit()
                            .putString("login_method", "phone")
                            .apply();

                    getSharedPreferences("userProfile", MODE_PRIVATE)
                            .edit()
                            .clear()
                            .apply();

                    Intent intent = new Intent(OtpActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

            } else {
                    showOtpError();
                }
            });
        });

        Resendlabl.setOnClickListener(v -> {
            Resendlabl.setEnabled(false);
            startResendCountdown();

            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    "+91" + getIntent().getStringExtra("mobile"),
                    60,
                    TimeUnit.SECONDS,
                    OtpActivity.this,
                    new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                        @Override
                        public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {

                        }

                        @Override
                        public void onVerificationFailed(@NonNull FirebaseException e) {
                            Toast.makeText(OtpActivity.this, "Failed to resend OTP: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Resendlabl.setEnabled(true);
                        }

                        @Override
                        public void onCodeSent(@NonNull String newBackendOtp, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                            getotpbackend = newBackendOtp;
                            Toast.makeText(OtpActivity.this, "OTP sent successfully", Toast.LENGTH_SHORT).show();
                            startResendCountdown();
                        }
                    }
            );
        });

    }

    private void startResendCountdown() {
        resendTimer.setVisibility(View.VISIBLE);
        Resendlabl.setEnabled(false);

        new CountDownTimer(60000, 1000) {
            @SuppressLint("DefaultLocale")
            public void onTick(long millisUntilFinished) {
                long secondsLeft = millisUntilFinished / 1000;
                resendTimer.setText(String.format("Resend Code by SMS (0:%02d)", secondsLeft));
            }

            @SuppressLint("SetTextI18n")
            public void onFinish() {
                resendTimer.setText("You can resend the OTP now");
                Resendlabl.setEnabled(true);
            }
        }.start();


}


    private void showOtpError() {
        inputnumber1.setBackgroundResource(R.drawable.otp_box_background_error);
        inputnumber2.setBackgroundResource(R.drawable.otp_box_background_error);
        inputnumber3.setBackgroundResource(R.drawable.otp_box_background_error);
        inputnumber4.setBackgroundResource(R.drawable.otp_box_background_error);
        inputnumber5.setBackgroundResource(R.drawable.otp_box_background_error);
        inputnumber6.setBackgroundResource(R.drawable.otp_box_background_error);

        if (otpErrorText != null) {
            otpErrorText.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show();
        }
    }


    private void numbertomove() {
        TextWatcher clearErrorWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                inputnumber1.setBackgroundResource(R.drawable.otp_box_background);
                inputnumber2.setBackgroundResource(R.drawable.otp_box_background);
                inputnumber3.setBackgroundResource(R.drawable.otp_box_background);
                inputnumber4.setBackgroundResource(R.drawable.otp_box_background);
                inputnumber5.setBackgroundResource(R.drawable.otp_box_background);
                inputnumber6.setBackgroundResource(R.drawable.otp_box_background);
                findViewById(R.id.otpErrorText).setVisibility(View.GONE);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };

        inputnumber1.addTextChangedListener(clearErrorWatcher);
        inputnumber2.addTextChangedListener(clearErrorWatcher);
        inputnumber3.addTextChangedListener(clearErrorWatcher);
        inputnumber4.addTextChangedListener(clearErrorWatcher);
        inputnumber5.addTextChangedListener(clearErrorWatcher);
        inputnumber6.addTextChangedListener(clearErrorWatcher);

        moveFocusOnInput(inputnumber1, inputnumber2);
        inputnumber2.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_DEL && inputnumber2.getText().toString().isEmpty()) {
                inputnumber1.requestFocus();
            }
            return false;
        });
        moveFocusOnInput(inputnumber2, inputnumber3);
        inputnumber3.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_DEL && inputnumber3.getText().toString().isEmpty()) {
                inputnumber2.requestFocus();
            }
            return false;
        });
        moveFocusOnInput(inputnumber3, inputnumber4);
        inputnumber4.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_DEL && inputnumber4.getText().toString().isEmpty()) {
                inputnumber3.requestFocus();
            }
            return false;
        });
        moveFocusOnInput(inputnumber4, inputnumber5);
        inputnumber5.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_DEL && inputnumber5.getText().toString().isEmpty()) {
                inputnumber4.requestFocus();
            }
            return false;
        });

        moveFocusOnInput(inputnumber5, inputnumber6);
        inputnumber6.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_DEL && inputnumber6.getText().toString().isEmpty()) {
                inputnumber5.requestFocus();
            }
            return false;
        });
    }

    private void moveFocusOnInput(EditText current, EditText next) {
        current.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().trim().isEmpty()) next.requestFocus();
            }
            @Override public void afterTextChanged(Editable s) {}
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
