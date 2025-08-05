package com.example.hotelbookingapp.activities;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hotelbookingapp.R;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 100;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private CallbackManager callbackManager;
    private TextView tvTerms;
    private FirebaseFirestore db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        tvTerms = findViewById(R.id.tvTerms);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        FacebookSdk.setApplicationId("1650645932316856");
        FacebookSdk.setClientToken("8da7680f2df20b5db3cea7e1f0e0faa8");
        FacebookSdk.fullyInitialize();
        // 1. Cấu hình Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // dòng này quan trọng!
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        findViewById(R.id.btnGoogleSignIn).setOnClickListener(view -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });

        callbackManager = CallbackManager.Factory.create();

        findViewById(R.id.btnFacebookSignIn).setOnClickListener(v -> {
            LoginManager.getInstance().logInWithReadPermissions(
                    this,
                    Arrays.asList("email","public_profile")
            );

            LoginManager.getInstance().registerCallback(callbackManager,
                    new FacebookCallback<LoginResult>() {
                        @Override
                        public void onSuccess(LoginResult loginResult) {
                            handleFacebookAccessToken(loginResult.getAccessToken());
                        }

                        @Override
                        public void onCancel() {
                            Toast.makeText(LoginActivity.this, "Đăng nhập Facebook đã hủy", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(FacebookException error) {
                            Toast.makeText(LoginActivity.this, "Lỗi Facebook: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        clickToWeb();
    }

    // 2. Bắt kết quả đăng nhập Google
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 3. Đăng nhập Firebase bằng Google token
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(this, "Đăng nhập thành công: " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
                        checkAndCreateUser(user);
                        createBookingInfoIfNotExists(user);
                        startActivity(new Intent(this, UserHomeActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Đăng nhập thất bại", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(this, "Đăng nhập thành công: " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
                        checkAndCreateUser(user);
                        createBookingInfoIfNotExists(user);
                        startActivity(new Intent(this, UserHomeActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Đăng nhập Facebook thất bại", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkAndCreateUser(FirebaseUser user) {
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        createUserInFirestore(user);
                    }
                });
    }
    private void createUserInFirestore(FirebaseUser user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", user.getDisplayName() != null ? user.getDisplayName() : "");
        userData.put("email", user.getEmail() != null ? user.getEmail() : "");
        userData.put("phone", user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
        userData.put("gender", "");
        userData.put("address", "");
        userData.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid ->
                        Log.d("MainActivity", "Đã tạo user mới trong Firestore"))
                .addOnFailureListener(e ->
                        Log.e("MainActivity", "Lỗi khi tạo user", e));
    }
    private void createBookingInfoIfNotExists(FirebaseUser user) {
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = user.getUid();

        DocumentReference bookingRef = db.collection("bookinginfo").document(userId);
        bookingRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                // Chưa tồn tại -> thêm mới
                Map<String, Object> userData = new HashMap<>();
                userData.put("userId", userId);
                userData.put("username", user.getDisplayName() != null ? user.getDisplayName() : "");
                userData.put("email", user.getEmail() != null ? user.getEmail() : "");
                userData.put("phone", user.getPhoneNumber() != null ? user.getPhoneNumber() : "");

                bookingRef.set(userData)
                        .addOnSuccessListener(aVoid -> Log.d("MainActivity", "Đã tạo bookinginfo cho user"))
                        .addOnFailureListener(e -> Log.e("MainActivity", "Lỗi khi tạo bookinginfo", e));
            } else {
                Log.d("MainActivity", "bookinginfo đã tồn tại, không tạo lại.");
            }
        }).addOnFailureListener(e -> Log.e("MainActivity", "Lỗi khi kiểm tra bookinginfo", e));
    }

    private void clickToWeb(){

        String fullText = "Qua việc đăng nhập hoặc tạo tài khoản, bạn đồng ý với các Điều khoản và Điều kiện cũng như Chính sách An toàn và Bảo mật của chúng tôi";
        SpannableString spannable = new SpannableString(fullText);

        // "Điều khoản và Điều kiện"
        ClickableSpan termsSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://genuine-peony-575a57.netlify.app/"));
                startActivity(browserIntent);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.BLUE);
                ds.setUnderlineText(false);
            }
        };

        // "Chính sách An toàn và Bảo mật"
        ClickableSpan privacySpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://rainbow-faloodeh-210d8e.netlify.app/"));
                startActivity(browserIntent);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.BLUE);
                ds.setUnderlineText(false);
            }
        };

        // Vị trí bắt đầu và kết thúc của đoạn cần click
        spannable.setSpan(termsSpan, 58, 82, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);   // "Điều khoản và Điều kiện"
        spannable.setSpan(privacySpan, 91, 120, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); // "Chính sách An toàn và Bảo mật..."

        tvTerms.setText(spannable);
        tvTerms.setMovementMethod(LinkMovementMethod.getInstance());
        tvTerms.setHighlightColor(Color.TRANSPARENT);
    }
}
