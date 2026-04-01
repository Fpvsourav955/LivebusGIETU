package com.sourav.livebusgietu;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;
import androidx.appcompat.widget.AppCompatButton;

public class SettingsFragment extends Fragment {

    private CircleImageView profileImage;
    private TextView username, emailOrPhone;
    private DatabaseReference userRef;
    private ValueEventListener userListener;

    public SettingsFragment() {

    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        SharedPreferences prefs = requireActivity().getSharedPreferences("userPrefs", Context.MODE_PRIVATE);
        String loginMethod = prefs.getString("login_method", "unknown");

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(getContext(), Login.class));
            requireActivity().finish();
            return view;
        }
        LoadingDialog loadingDialog = new LoadingDialog(requireActivity());
        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());

        profileImage = view.findViewById(R.id.profileimage);
        username = view.findViewById(R.id.username);
        emailOrPhone = view.findViewById(R.id.emailorphone);
        AppCompatButton editProfile = view.findViewById(R.id.editprofile);
        AppCompatButton signOut = view.findViewById(R.id.signout);
        ImageView devTeam = view.findViewById(R.id.devteam);
        ImageView Bugreport= view.findViewById(R.id.bugs);
        LinearLayout Policy = view.findViewById(R.id.policy);
        LinearLayout shareapp=view.findViewById(R.id.shareapp);
        shareapp.setOnClickListener(v->{
            String appUrl = "https://play.google.com/store/apps/details?id=com.sourav.livebusgietu" + requireContext().getPackageName();

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out this amazing Live Bus App!");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Hey, download this app to track buses live:\n" + appUrl);

            try {
                startActivity(Intent.createChooser(shareIntent, "Share App via"));
            } catch (Exception e) {
                Toast.makeText(getContext(), "No app found to share", Toast.LENGTH_SHORT).show();
            }
        });
        Policy.setOnClickListener(v ->{
            Intent intent=new Intent(requireContext(), Terms.class);
            startActivity(intent);

        });
        Bugreport.setOnClickListener( v->{
            Intent intent=new Intent(requireContext(),Bug.class);
            startActivity(intent);
        });
        devTeam.setOnClickListener(v->{
            Intent intent=new Intent(requireContext(),DevTeam.class);
            startActivity(intent);
        });

        if (loginMethod.equals("phone")) {
            editProfile.setEnabled(true);
            editProfile.setAlpha(1f);
            editProfile.setText("Edit Profile");
        } else {
            editProfile.setEnabled(false);
            editProfile.setAlpha(0.5f);
            editProfile.setText("Edit Disabled");
        }

        editProfile.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), Upload.class);
            startActivity(intent);
        });

        signOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            prefs.edit().clear().apply();
            requireActivity().getSharedPreferences("userProfile", MODE_PRIVATE).edit().clear().apply();

            GoogleSignInClient client = GoogleSignIn.getClient(requireActivity(), GoogleSignInOptions.DEFAULT_SIGN_IN);
            client.signOut().addOnCompleteListener(task -> {
                Intent intent = new Intent(requireActivity(), Login.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            });
        });

        if (isAdded() && getContext() != null) {
            loadUserData();
        }

        return view;
    }

    private void loadUserData() {
        if (!isAdded() || getContext() == null) return;
        SharedPreferences prefs = requireActivity().getSharedPreferences("userProfile", MODE_PRIVATE);
        username.setText(prefs.getString("name", "User"));
        emailOrPhone.setText(prefs.getString("email", "No Email"));
        String photoUrl = prefs.getString("photoUrl", null);

        if (photoUrl != null) {
            Glide.with(this).load(photoUrl).into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.user);
        }

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                Context context = getContext();
                if (context == null) return;
                if (!snapshot.exists()) return;

                String name = snapshot.child("name").getValue(String.class);
                String email = snapshot.child("email").getValue(String.class);
                String photoUrl = snapshot.child("profileImage").getValue(String.class);

                username.setText(name != null ? name : "User");
                emailOrPhone.setText(email != null ? email : "No Email");

                if (photoUrl != null && !photoUrl.isEmpty()) {
                    Glide.with(requireContext()).load(photoUrl).into(profileImage);
                }
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("name", name);
                editor.putString("email", email);
                editor.putString("photoUrl", photoUrl);
                editor.apply();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                String name = snapshot.child("name").getValue(String.class);
                String email = snapshot.child("email").getValue(String.class);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        };
        userRef.addValueEventListener(userListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (userListener != null) {
           userRef.removeEventListener(userListener);
        }
    }
}
