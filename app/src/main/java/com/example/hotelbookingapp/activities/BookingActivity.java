package com.example.hotelbookingapp.activities;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.hotelbookingapp.R;
import com.example.hotelbookingapp.models.Hotel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BookingActivity extends AppCompatActivity {

    private ImageView btnBack;
    private FirebaseFirestore db;

    private TextView tvHotelName, tvHotelAddress, tvCheckIn, tvCheckOut, tvNightsAndRooms, tvTotalPrice,tvOldPrice;
    private TextView tvGiamGia;
    private Button btnPlaceBooking;
    private Hotel selectedHotel;
    private String dateFrom, dateTo;
    private int numberOfNights;
    private String username,phone;
    private long totalPrice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        db = FirebaseFirestore.getInstance();
        mappingViews();
        getDataFromIntent();
        displayBookingInfo();

        btnBack.setOnClickListener(v -> finish());

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String userId = "";

        if (currentUser != null) {
            userId = currentUser.getUid();
        }

        String finalUserId = userId;

        btnPlaceBooking.setOnClickListener(v -> saveBookingToFirestore(finalUserId, username));
    }

    private void mappingViews() {
        btnBack = findViewById(R.id.btnBack);
        tvHotelName = findViewById(R.id.tvHotelName);
        tvHotelAddress = findViewById(R.id.tvHotelAddress);
        tvCheckIn = findViewById(R.id.tvCheckIn);
        tvCheckOut = findViewById(R.id.tvCheckOut);
        tvNightsAndRooms = findViewById(R.id.tvNightsAndRooms);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        btnPlaceBooking = findViewById(R.id.btnPlaceBooking);
        tvOldPrice=findViewById(R.id.tvoldprice);
        tvGiamGia=findViewById(R.id.tv_GiamGia);
    }

    private void getDataFromIntent() {
        Intent intent = getIntent();
        selectedHotel = (Hotel) intent.getSerializableExtra("selected_hotel");
        dateFrom = intent.getStringExtra("date_from");
        dateTo = intent.getStringExtra("date_to");
        totalPrice = intent.getLongExtra("total_price",0);
        username=intent.getStringExtra("customer_name");
        phone=intent.getStringExtra("customer_phone");

        if (dateFrom == null || dateFrom.isEmpty()) {
            dateFrom = getToday();
        }
        if (dateTo == null || dateTo.isEmpty()) {
            dateTo = getTomorrow();
        }

        numberOfNights = calculateNights(dateFrom, dateTo);
    }

    private void displayBookingInfo() {
        if (selectedHotel == null) return;
        tvHotelName.setText(selectedHotel.name);
        tvHotelAddress.setText(selectedHotel.address);
        tvCheckIn.setText(dateFrom);
        tvCheckOut.setText(dateTo);
        String nightAndRoom = numberOfNights + " đêm, 1 phòng cho 2 người lớn";
        tvNightsAndRooms.setText(nightAndRoom);
        long oldPrice=selectedHotel.getPriceOld();
        long oldTotal= oldPrice*numberOfNights;
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        tvTotalPrice.setText(format.format(totalPrice));
        tvOldPrice.setText(format.format(oldTotal));
        tvOldPrice.setPaintFlags(tvOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        long giamgia = totalPrice-oldTotal;
        tvGiamGia.setText(format.format(giamgia));
    }

    private String getToday() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String getTomorrow() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(cal.getTime());
    }

    private int calculateNights(String from, String to) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date d1 = sdf.parse(from);
            Date d2 = sdf.parse(to);
            if (d1 == null || d2 == null) return 1;
            long diff = d2.getTime() - d1.getTime();
            return Math.max(1, (int) (diff / (1000 * 60 * 60 * 24))); // ít nhất 1 đêm

        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
    }

    private void saveBookingToFirestore(String userId, String userName) {
        if (selectedHotel == null) return;
        CollectionReference bookingsRef = db.collection("bookings");
        String bookingId = bookingsRef.document().getId();
        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("id", bookingId);
        bookingData.put("hotelId", selectedHotel.id);
        bookingData.put("hotelName", selectedHotel.name);
        bookingData.put("address", selectedHotel.address);
        bookingData.put("dateCheckIn", dateFrom);
        bookingData.put("dateCheckOut", dateTo);
        bookingData.put("numberOfNights", numberOfNights);
        bookingData.put("totalPrice", totalPrice);
        bookingData.put("userId", userId);
        bookingData.put("userName", userName);
        bookingData.put("phone", phone);
        bookingData.put("status","confirm");
        bookingData.put("createdAt", com.google.firebase.Timestamp.now());

        bookingsRef.document(bookingId).set(bookingData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Đặt phòng thành công!", Toast.LENGTH_SHORT).show();
                    Intent homeIntent = new Intent(this, UserHomeActivity.class);
                    homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    Intent historyIntent = new Intent(this, BookingHistoryActivity.class);
                    startActivities(new Intent[]{homeIntent, historyIntent});
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Đặt phòng thất bại!", Toast.LENGTH_SHORT).show()
                );
    }
}
