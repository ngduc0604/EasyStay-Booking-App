package com.example.hotelbookingapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.hotelbookingapp.R;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class AccountActivity extends AppCompatActivity {
    private Button btnSignOut;
    private FirebaseUser firebaseUser;
    private TextView tvName;
    private LinearLayout tabSearch,btnAccountSet,tabBooking,tabSaved;

    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        Mapping();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        btnSignOut.setOnClickListener(v -> showSignOutDialog());
        btnAccountSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AccountActivity.this, AccountUpdateActivity.class);
                startActivity(intent);
            }
        });

        setupBottomTabs();
        tvName.setText(firebaseUser.getDisplayName());
    }

    private void showSignOutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                .setPositiveButton("Có", (dialog, which) -> signOutAll())
                .setNegativeButton("Không", null)
                .show();
    }
    public void Mapping() {
        tvName=findViewById(R.id.tv_Name);
        btnSignOut=findViewById(R.id.btn_SignOut);
        btnAccountSet=findViewById(R.id.account_update);
        View bottomNav = findViewById(R.id.layout_bottom_nav);
        tabSearch = bottomNav.findViewById(R.id.tab_search);
        tabBooking = bottomNav.findViewById(R.id.tab_booking);
        tabSaved = bottomNav.findViewById(R.id.tab_saved);
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
    }
    private void signOutAll() {
        FirebaseAuth.getInstance().signOut();
        if (mGoogleSignInClient != null) {
            mGoogleSignInClient.signOut();
        }
        LoginManager.getInstance().logOut();
        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // xóa stack
        startActivity(intent);
    }
    private void setupBottomTabs() {
        tabBooking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AccountActivity.this, BookingHistoryActivity.class);
                startActivity(intent);
            }
        });
        tabSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AccountActivity.this, UserHomeActivity.class);
                startActivity(intent);
            }
        });
        tabSaved.setOnClickListener(v -> {
            Intent intent = new Intent(AccountActivity.this, SavedHotelsActivity.class);
            startActivity(intent);
        });

    }
}




