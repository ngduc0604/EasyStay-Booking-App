package com.example.hotelbookingapp.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hotelbookingapp.R;
import com.example.hotelbookingapp.adapters.BookingHistoryAdapter;
import com.example.hotelbookingapp.models.Booking;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;


public class BookingHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerBookingHistory;
    private TextView tvEmptyMessage;
    private FirebaseFirestore db;
    private BookingHistoryAdapter adapter;
    private List<Booking> bookingList = new ArrayList<>();
    private Button btnConfirmed, btnCompleted, btnCanceled;
    private List<Booking> allBookings = new ArrayList<>(); // danh sách gốc

    private LinearLayout tabSearch, tabSaved, tabAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_history);
        Mapping();
        btnConfirmed.setTextColor(Color.BLUE);
        recyclerBookingHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookingHistoryAdapter(this, bookingList, booking -> {
            Intent intent = new Intent(BookingHistoryActivity.this, BookingDetailActivity.class);
            intent.putExtra("bookingId", booking.getId());
            startActivity(intent);
        });
        recyclerBookingHistory.setAdapter(adapter);

        loadUserBookings();

        findViewById(R.id.back_to_userhome).setOnClickListener(v -> finish());

        setupBottomTabs();
        btnConfirmed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterBookingsByStatus("confirm");
                btnConfirmed.setTextColor(Color.BLUE);
                btnCanceled.setTextColor(Color.GRAY);
                btnCompleted.setTextColor(Color.GRAY);
            }
        });
        btnCanceled.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterBookingsByStatus("cancel");
                btnConfirmed.setTextColor(Color.GRAY);
                btnCanceled.setTextColor(Color.BLUE);
                btnCompleted.setTextColor(Color.GRAY);
            }
        });
        btnCompleted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterBookingsByStatus("complete");
                btnConfirmed.setTextColor(Color.GRAY);
                btnCanceled.setTextColor(Color.GRAY);
                btnCompleted.setTextColor(Color.BLUE);
            }
        });

    }

    private void Mapping(){
        btnConfirmed = findViewById(R.id.btn_confirm);
        btnCompleted = findViewById(R.id.btn_completed);
        btnCanceled = findViewById(R.id.btn_cancelled);
        recyclerBookingHistory = findViewById(R.id.recyclerBookingHistory);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        db = FirebaseFirestore.getInstance();
    }


    private void loadUserBookings() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;

        CollectionReference bookingsRef = db.collection("bookings");
        bookingsRef.whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allBookings.clear();
                    for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                        Booking booking = new Booking();
                        booking.setId(snapshot.getString("id"));
                        booking.setHotelId(snapshot.getString("hotelId"));
                        booking.setHotelName(snapshot.getString("hotelName"));
                        booking.setCheckInDate(snapshot.getString("dateCheckIn"));
                        booking.setCheckOutDate(snapshot.getString("dateCheckOut"));
                        booking.setTotalPrice(snapshot.getLong("totalPrice"));
                        booking.setStatus(snapshot.getString("status"));
                        allBookings.add(booking);
                    }


                    filterBookingsByStatus("confirm");
                });
    }
    private void filterBookingsByStatus(String status) {
        bookingList.clear();
        for (Booking b : allBookings) {
            if (status.equals(b.getStatus())) {
                bookingList.add(b);
            }
        }
        adapter.notifyDataSetChanged();
        tvEmptyMessage.setVisibility(bookingList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void setupBottomTabs() {
        View bottomNav = findViewById(R.id.layout_bottom_nav);
        tabSearch = bottomNav.findViewById(R.id.tab_search);
        tabSaved = bottomNav.findViewById(R.id.tab_saved);
        tabAccount = bottomNav.findViewById(R.id.tab_account);

        tabSearch.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserHomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        tabSaved.setOnClickListener(v -> {
            Intent intent = new Intent(this, SavedHotelsActivity.class);
            startActivity(intent);
        });

        tabAccount.setOnClickListener(v -> {
            Intent intent = new Intent(this, AccountActivity.class);
            startActivity(intent);
        });
    }

}
