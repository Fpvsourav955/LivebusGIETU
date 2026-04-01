package com.sourav.livebusgietu;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;


public class Bug extends AppCompatActivity {
    private EditText etName, etEmail, etMessage;
    private Uri imageUri;
    private ImageButton imageUploadButton;
    private  LoadingDialog loadingDialog;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bug);
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
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        imageUploadButton.setImageURI(imageUri);
                    }
                }
        );



        loadingDialog = new LoadingDialog(Bug.this);
        ImageView repback = findViewById(R.id.repback);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etMessage = findViewById(R.id.etMessage);
        AppCompatButton btnSend = findViewById(R.id.btnSend);
        imageUploadButton = findViewById(R.id.imageUploadButton);

        imageUploadButton.setOnClickListener(v -> openFileChooser());


        repback.setOnClickListener(v -> finish());
        DatabaseReference reportsRef = FirebaseDatabase.getInstance().getReference("bug_reports");

        btnSend.setOnClickListener(v -> {
            loadingDialog.startLoadingDiloag();
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String message = etMessage.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || message.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            uploadImageAndSendReport(name, email, message);
        });



    }


    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Image"));
    }

    private void uploadImageAndSendReport(String name, String email, String message) {
        if (imageUri != null) {

            StorageReference storageRef = FirebaseStorage.getInstance().getReference("bug_screenshots");
            String fileName = System.currentTimeMillis() + "." + getFileExtension(imageUri);
            StorageReference fileRef = storageRef.child(fileName);

            fileRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String downloadUrl = uri.toString();
                                saveBugReport(name, email, message, downloadUrl);
                            }))
                    .addOnFailureListener(e -> Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {

            saveBugReport(name, email, message, null);
        }
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    private void saveBugReport(String name, String email, String message, String imageUrl) {
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("bug_reports");
        String reportId = databaseRef.push().getKey();

        BugReportModel bugReport = new BugReportModel(name, email, message, imageUrl);

        if (reportId != null) {
            databaseRef.child(reportId).setValue(bugReport)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            loadingDialog.dismissDialog();
                            Toast.makeText(this, "Report sent successfully!", Toast.LENGTH_SHORT).show();
                            clearFields();
                            imageUploadButton.setImageResource(R.drawable.icons8upload100);
                            imageUri = null;

                        } else {
                            loadingDialog.dismissDialog();
                            Toast.makeText(this, "Failed to send report.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }



    private void clearFields() {
        etName.setText("");
        etEmail.setText("");
        etMessage.setText("");
    }
}