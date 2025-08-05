package com.example.hotelbookingapp.activities;


import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.hotelbookingapp.R;
import com.example.hotelbookingapp.models.Hotel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import java.util.Locale;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class BookingDetailActivity extends AppCompatActivity {

    private TextView tvHotelName, tvAddress, tvDates, tvTotalPrice,tvUserName,tvDateBook,tvStatus;
    private ImageView imgHotel, btnBack;
    private FirebaseFirestore db;
    private String bookingId;
    private LinearLayout detail;
    private Button btnCancel,btnDanhGia;
    private String hotelIdRV;
    private FirebaseAuth auth;

    // Trạng thái review hiện tại
    private boolean hasReview = false;
    private String reviewDocId = null;
    private Float reviewStar = null;
    private String reviewContent = null;
    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_detail);
        Mapping();


        if (bookingId != null) {
            loadBookingInfo(bookingId);

        } else {
            Toast.makeText(this, "Không tìm thấy thông tin đặt phòng", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnBack.setOnClickListener(v -> finish());


        btnCancel.setOnClickListener(v -> ShowCancelDialog());

//        btnDanhGia.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent= new Intent(BookingDetailActivity.this,ReviewHotelActivity.class);
//                intent.putExtra("hotelId",hotelIdRV);
//                intent.putExtra("bookingId",bookingId);
//                startActivity(intent);
//            }
//        });

    }

    private final ActivityResultLauncher<Intent> reviewLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    checkExistingReviewAndSetup(bookingId);
                }
            });

    private void loadBookingInfo(String bookingId) {
        db.collection("bookings").document(bookingId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String hotelName = documentSnapshot.getString("hotelName");
                        String address = documentSnapshot.getString("address");
                        String checkInDate = documentSnapshot.getString("dateCheckIn");
                        String checkOutDate = documentSnapshot.getString("dateCheckOut");
                        Long totalPrice = documentSnapshot.getLong("totalPrice");
                        String hotelId = documentSnapshot.getString("hotelId");
                        hotelIdRV=hotelId;
                        String username = documentSnapshot.getString("userName");
                        userName= documentSnapshot.getString("userName");
                        Timestamp createDay = documentSnapshot.getTimestamp("createdAt");
                        String status= documentSnapshot.getString("status");
                        if (status != null && status.equals("confirm")) {
                            btnCancel.setVisibility(View.VISIBLE);
                        } else {
                            btnCancel.setVisibility(View.GONE);
                        }
                        if(status!=null && status.equals("complete")){
                            btnDanhGia.setVisibility(View.VISIBLE);
                            checkExistingReviewAndSetup(bookingId);
                        }else{
                            btnDanhGia.setVisibility(View.GONE);
                        }
                        if (createDay != null) {
                            Date date = createDay.toDate();
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                            String formattedDate = sdf.format(date);
                            tvDateBook.setText("Ngày đặt: " + formattedDate);
                        }
                        tvHotelName.setText(hotelName);
                        tvAddress.setText(address);
                        tvDates.setText("Nhận: " + checkInDate + " - Trả: " + checkOutDate);
                        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                        tvTotalPrice.setText(format.format(totalPrice));
                        tvUserName.setText("Tên khách hàng: "+username);
                        if(status.equals("confirm")){
                            tvStatus.setTextColor(Color.BLUE);
                            tvStatus.setText("Trạng thái: Đã xác nhận");
                        }else if(status.equals("complete")){
                            tvStatus.setText("Trạng thái: Đã hoàn thành");
                        }else{
                            tvStatus.setText("Trạng thái: Đã hủy");
                        }

                        loadHotelImage(hotelId);

                    } else {
                        Toast.makeText(this, "Không tìm thấy thông tin đặt phòng", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải thông tin đặt phòng", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void loadHotelImage(String hotelId) {
        db.collection("hotels").document(hotelId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Hotel hotel = documentSnapshot.toObject(Hotel.class);
                        Glide.with(this).load(hotel.imageUrls.get(0)).into(imgHotel);
                        detail.setOnClickListener(v -> {
                            Intent intent = new Intent(this, HotelDetailActivity.class);
                            intent.putExtra("selected_hotel", hotel);
                            startActivity(intent);
                        });
                    }
                });
    }
    public void Mapping(){
        detail= findViewById(R.id.detail_booking);
        tvHotelName = findViewById(R.id.tvDetailHotelName);
        tvAddress = findViewById(R.id.tvDetailAddress);
        tvDates = findViewById(R.id.tvDetailDate);
        tvTotalPrice = findViewById(R.id.tvDetailTotalPrice);
        imgHotel = findViewById(R.id.imgHotelDetail);
        btnBack = findViewById(R.id.back_to_history);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        bookingId = getIntent().getStringExtra("bookingId");
        tvUserName=findViewById(R.id.tv_User_Booking);
        tvDateBook=findViewById(R.id.tv_Booking_Date);
        btnCancel=findViewById(R.id.btn_cancel);
        tvStatus=findViewById(R.id.tvstatus);
        btnDanhGia=findViewById(R.id.btn_danhgia);
    }

    private void CancelBooking(){
        db.collection("bookings").document(bookingId)
                .update("status", "cancel")
                .addOnSuccessListener(unused -> {
                    Toast.makeText(BookingDetailActivity.this, "Đã hủy đặt phòng", Toast.LENGTH_SHORT).show();
                    btnCancel.setEnabled(false);
                    Intent intent=new Intent(BookingDetailActivity.this,BookingHistoryActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // xóa stack
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(BookingDetailActivity.this, "Lỗi khi hủy đặt phòng", Toast.LENGTH_SHORT).show();
                });
    }

    private void ShowCancelDialog(){
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xác nhận hủy")
                .setMessage("Bạn có chắc chắn muốn hủy đặt phòng không?")
                .setPositiveButton("Có", (dialog, which) -> CancelBooking())
                .setNegativeButton("Không", null)
                .show();
    }



    private void checkExistingReviewAndSetup(String bookingId) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            hasReview = false;
            setupReviewButton();
            return;
        }
        String uid = user.getUid();
        String docId = bookingId + "_" + uid;

        db.collection("ReviewHotel")
                .document(docId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        hasReview = true;
                        reviewDocId = doc.getId();
                        Number starNum = (Number) doc.get("star");
                        reviewStar = (starNum == null) ? null : starNum.floatValue();
                        reviewContent = doc.getString("content");
                    } else {
                        hasReview = false;
                        reviewDocId = null;
                        reviewStar = null;
                        reviewContent = null;
                    }
                    setupReviewButton();
                })
                .addOnFailureListener(e -> {
                    hasReview = false;
                    setupReviewButton();
                });
    }

    private void setupReviewButton() {
        if (hasReview) {
            btnDanhGia.setText("Sửa đánh giá");
        } else {
            btnDanhGia.setText("Đánh giá");
        }
        btnDanhGia.setOnClickListener(v -> {
            Intent intent = new Intent(BookingDetailActivity.this, ReviewHotelActivity.class);
            intent.putExtra("hotelId", hotelIdRV);
            intent.putExtra("bookingId", bookingId);
            intent.putExtra("userName",userName);
            if (hasReview) {
                intent.putExtra("mode", "edit");
                intent.putExtra("reviewDocId", reviewDocId);
                if (reviewStar != null) intent.putExtra("star", reviewStar);
                if (reviewContent != null) intent.putExtra("content", reviewContent);
            } else {
                intent.putExtra("mode", "create");
            }

            reviewLauncher.launch(intent); // <-- thay vì startActivity(intent)
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (btnDanhGia.getVisibility() == View.VISIBLE) {
            checkExistingReviewAndSetup(bookingId);
        }
    }


}
