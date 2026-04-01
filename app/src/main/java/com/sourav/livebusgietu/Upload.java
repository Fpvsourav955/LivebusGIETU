package com.sourav.livebusgietu;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.InputStream;

import de.hdodenhof.circleimageview.CircleImageView;

public class Upload extends AppCompatActivity {
    private CircleImageView dialogProfileImage;
    private EditText editName, editEmail;

    private Uri selectedImageUri = null;
    private  LoadingDialog loadingDialog;
    private DatabaseReference userRef;
    private StorageReference storageRef;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            assert inputStream != null;
                            int fileSize = inputStream.available();
                            inputStream.close();

                            if (fileSize > 1048576) {
                                Toast.makeText(this, "Image size must be under 1 MB", Toast.LENGTH_SHORT).show();
                                dialogProfileImage.setImageURI(imageUri);
                                dialogProfileImage.setBorderColor(ContextCompat.getColor(this, R.color.red));
                                dialogProfileImage.setBorderWidth(6);
                                selectedImageUri = null;
                            } else {
                                dialogProfileImage.setImageURI(imageUri);
                                dialogProfileImage.setBorderColor(ContextCompat.getColor(this, R.color.normal_stroke));
                                dialogProfileImage.setBorderWidth(2);
                                selectedImageUri = imageUri;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Failed to read image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_upload);
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
        loadingDialog = new LoadingDialog(this);

        dialogProfileImage = findViewById(R.id.dialogProfileImage);
        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        AppCompatButton chooseImageButton = findViewById(R.id.chooseImageButton);
        AppCompatButton saveButton = findViewById(R.id.save);
        AppCompatButton cancelButton = findViewById(R.id.cancel);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
        storageRef = FirebaseStorage.getInstance().getReference("ProfileImages").child(currentUser.getUid());

        loadUserData();

        chooseImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });


        cancelButton.setOnClickListener(v -> finish());

        saveButton.setOnClickListener(v -> {
            loadingDialog.startLoadingDiloag();
            String name = editName.getText().toString().trim();
            String email = editEmail.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty()) {
                loadingDialog.dismissDialog();
                Toast.makeText(this, "Name and email cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            userRef.child("name").setValue(name);
            userRef.child("email").setValue(email);

            if (selectedImageUri != null) {
                deleteOldImageAndUploadNew(selectedImageUri);
            } else {
                loadingDialog.dismissDialog();
                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }



        private void loadUserData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isFinishing() && !isDestroyed()) { // ✅ Check before using Glide
                    if (snapshot.exists()) {
                        String name = snapshot.child("name").getValue(String.class);
                        String email = snapshot.child("email").getValue(String.class);
                        String imageUrl = snapshot.child("profileImage").getValue(String.class);

                        if (name != null) editName.setText(name);
                        if (email != null) editEmail.setText(email);

                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(Upload.this).load(imageUrl).into(dialogProfileImage);
                        }
                    }
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(Upload.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

            private void deleteOldImageAndUploadNew(Uri newImageUri) {
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    // Delete the old image
                    FirebaseStorage.getInstance().getReferenceFromUrl(uri.toString()).delete()
                            .addOnSuccessListener(unused -> uploadNewImage(newImageUri))
                            .addOnFailureListener(e -> uploadNewImage(newImageUri));
                }).addOnFailureListener(e -> uploadNewImage(newImageUri)); // No existing image
            }

            private void uploadNewImage(Uri newImageUri) {
                storageRef.putFile(newImageUri)
                        .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    String downloadUrl = uri.toString();
                                    userRef.child("profileImage").setValue(downloadUrl)
                                            .addOnSuccessListener(unused -> {
                                                loadingDialog.dismissDialog();
                                                Toast.makeText(Upload.this, "Profile updated", Toast.LENGTH_SHORT).show();
                                                finish();
                                            });
                                }))
                        .addOnFailureListener(e -> {
                            loadingDialog.dismissDialog();
                            Toast.makeText(Upload.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                        });
            }
    private boolean isActive = false;

    @Override
    protected void onStart() {
        super.onStart();
        isActive = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isActive = false;
    }

}

