package com.example.hotelbookingapp.activities;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotelbookingapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class AccountUpdateActivity extends AppCompatActivity {

    private EditText etName, etPhone, etAddress, etEmail;
    private Spinner spGender;
    private Button btnSave;
    private ImageView btnBack;

    private FirebaseUser firebaseUser;
    private DocumentReference userRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);
        initViews();
        loadUserData();
        setListeners();
    }
    private void initViews() {
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);
        etEmail = findViewById(R.id.etEmail);
        spGender = findViewById(R.id.spGender);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.back_to_account);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            userRef = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(firebaseUser.getUid());
        }
    }
    private void loadUserData() {
        if (firebaseUser == null) return;

        userRef.get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        etName.setText(snapshot.getString("name"));
                        etPhone.setText(snapshot.getString("phone"));
                        etAddress.setText(snapshot.getString("address"));
                        etEmail.setText(snapshot.getString("email"));

                        String gender = snapshot.getString("gender");
                        if (gender != null) {
                            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spGender.getAdapter();
                            int position = adapter.getPosition(gender);
                            spGender.setSelection(position >= 0 ? position : 0);
                        }
                    } else {
                        // Nếu chưa có trong Firestore, load từ FirebaseAuth
                        etName.setText(firebaseUser.getDisplayName());
                        etEmail.setText(firebaseUser.getEmail());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show()
                );
    }
    private void setListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> saveUserData());
    }
    private void saveUserData() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String gender = spGender.getSelectedItem().toString();

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }
        String phoneRegex = "^0\\d{9}$";

        if (!phone.matches(phoneRegex)) {
            Toast.makeText(this, "Số điện thoại không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

        if (!email.matches(emailRegex)) {
            Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("phone", phone);
        userData.put("address", address);
        userData.put("email", email);
        userData.put("gender", gender);
        userData.put("updatedAt", FieldValue.serverTimestamp());

        userRef.set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    finish(); // Quay lại trang trước
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi lưu thông tin", Toast.LENGTH_SHORT).show()
                );
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
}
