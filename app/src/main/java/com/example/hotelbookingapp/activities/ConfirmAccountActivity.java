package com.example.hotelbookingapp.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotelbookingapp.R;
import com.example.hotelbookingapp.models.Hotel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ConfirmAccountActivity extends AppCompatActivity {

    private EditText edtName, edtEmail, edtPhone;
    private TextView tvTotalPrice;
    private CheckBox checkboxSaveInfo;
    private ImageView btnBack;
    private Button btnNext;

    private Hotel selectedHotel;
    private String dateFrom, dateTo;
    private long totalPrice;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_account);
        Mapping();
        btnBack.setOnClickListener(v -> finish());
        setDefaultUserInfo();
        getDataFromIntent();
        btnNext.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String email = edtEmail.getText().toString().trim();
            String phone = edtPhone.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
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

            if(checkboxSaveInfo.isChecked()){
                saveUserInfoToBookingInfo();
            }

            Intent intent = new Intent(this, BookingActivity.class);
            intent.putExtra("selected_hotel", selectedHotel);
            intent.putExtra("date_from", dateFrom);
            intent.putExtra("date_to", dateTo);
            intent.putExtra("total_price", totalPrice);
            intent.putExtra("customer_name", name);
            intent.putExtra("customer_email", email);
            intent.putExtra("customer_phone", phone);
            startActivity(intent);
        });
    }

    private void Mapping() {
        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        checkboxSaveInfo = findViewById(R.id.checkboxSaveInfo);
        btnBack = findViewById(R.id.back_to_detail);
        btnNext = findViewById(R.id.btnConfirmBooking);
    }


    private void setDefaultUserInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("bookinginfo").document(user.getUid());

        userRef.get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String username = snapshot.getString("username");
                        String email = snapshot.getString("email");
                        String phone = snapshot.getString("phone");

                        if (username != null && !username.isEmpty()) edtName.setText(username);
                        else if (user.getDisplayName() != null) edtName.setText(user.getDisplayName());

                        if (email != null && !email.isEmpty()) edtEmail.setText(email);
                        else if (user.getEmail() != null) edtEmail.setText(user.getEmail());

                        if (phone != null && !phone.isEmpty()) edtPhone.setText(phone);
                        else if (user.getPhoneNumber() != null) edtPhone.setText(user.getPhoneNumber());

                    } else {
                        // Không có document -> fallback FirebaseAuth
                        if (user.getDisplayName() != null) edtName.setText(user.getDisplayName());
                        if (user.getEmail() != null) edtEmail.setText(user.getEmail());
                        if (user.getPhoneNumber() != null) edtPhone.setText(user.getPhoneNumber());
                    }
                })
                .addOnFailureListener(e -> {
                    // Lỗi khi đọc Firestore -> fallback FirebaseAuth
                    if (user.getDisplayName() != null) edtName.setText(user.getDisplayName());
                    if (user.getEmail() != null) edtEmail.setText(user.getEmail());
                    if (user.getPhoneNumber() != null) edtPhone.setText(user.getPhoneNumber());
                });
    }


    private void getDataFromIntent() {
        Intent intent = getIntent();
        selectedHotel = (Hotel) intent.getSerializableExtra("selected_hotel");
        dateFrom = intent.getStringExtra("date_from");
        dateTo = intent.getStringExtra("date_to");
        totalPrice = intent.getLongExtra("total_price",0);
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        tvTotalPrice.setText(format.format(totalPrice));
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

    private void saveUserInfoToBookingInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getUid());
        userInfo.put("username", name);
        userInfo.put("email", email);
        userInfo.put("phone", phone);


        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("bookinginfo").document(user.getUid())
                .set(userInfo)
                .addOnSuccessListener(unused ->
                        Log.d("BookingInfo", "Thông tin người dùng đã được lưu vào Firestore"))
                .addOnFailureListener(e ->
                        Log.e("BookingInfo", "Lỗi khi lưu thông tin người dùng", e));
    }

}
