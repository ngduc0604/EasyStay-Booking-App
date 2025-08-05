package com.example.hotelbookingapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hotelbookingapp.R;
import com.example.hotelbookingapp.adapters.SavedHotelsAdapter;
import com.example.hotelbookingapp.models.Hotel;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SavedHotelsActivity extends AppCompatActivity {
    private ActivityResultLauncher<Intent> hotelDetailLauncher;

    private LinearLayout tabSearch,tabBooking, tabAccount;
    private RecyclerView recyclerView;
    private SavedHotelsAdapter adapter;
    private List<Hotel> savedHotels = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TextView tvEmptyMessage;
    private ImageView btnBack2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_hotels);
        Mapping();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SavedHotelsAdapter(savedHotels, this::onHotelClicked);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        loadSavedHotels();
        btnBack2.setOnClickListener(v -> finish());
        setupBottomTabs();

        hotelDetailLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        loadSavedHotels();
                    }
                }
        );
    }
    private void Mapping(){
        btnBack2=findViewById(R.id.back_to_userhome);
        recyclerView = findViewById(R.id.recyclerSavedHotels);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);

        View bottomNav = findViewById(R.id.layout_bottom_nav);
        tabSearch = bottomNav.findViewById(R.id.tab_search);
        tabBooking = bottomNav.findViewById(R.id.tab_booking);
        tabAccount = bottomNav.findViewById(R.id.tab_account);
    }

    private void loadSavedHotels() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("favorites")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(favoriteSnapshots -> {
                    savedHotels.clear();

                    if (favoriteSnapshots.isEmpty()) {
                        adapter.notifyDataSetChanged();
                        tvEmptyMessage.setVisibility(View.VISIBLE);
                        return;
                    }

                    List<Task<DocumentSnapshot>> hotelTasks = new ArrayList<>();

                    for (DocumentSnapshot favDoc : favoriteSnapshots) {
                        String hotelId = favDoc.getString("hotelId");
                        if (hotelId != null) {
                            Task<DocumentSnapshot> hotelTask = db.collection("hotels").document(hotelId).get();
                            hotelTasks.add(hotelTask);
                        }
                    }

                    Tasks.whenAllSuccess(hotelTasks)
                            .addOnSuccessListener(results -> {
                                for (Object result : results) {
                                    DocumentSnapshot doc = (DocumentSnapshot) result;
                                    if (doc.exists()) {
                                        Hotel hotel = doc.toObject(Hotel.class);
                                        hotel.id = doc.getId();
                                        savedHotels.add(hotel);
                                    }
                                }
                                adapter.notifyDataSetChanged();
                                tvEmptyMessage.setVisibility(savedHotels.isEmpty() ? View.VISIBLE : View.GONE);
                            });
                });
    }


    private void onHotelClicked(Hotel hotel) {
        Intent intent = new Intent(this, HotelDetailActivity.class);
        intent.putExtra("selected_hotel", hotel);
        hotelDetailLauncher.launch(intent);
    }



    private void setupBottomTabs() {

        tabSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SavedHotelsActivity.this, UserHomeActivity.class);
                startActivity(intent);
            }
        });

        tabAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SavedHotelsActivity.this, AccountActivity.class);
                startActivity(intent);
            }
        });

        tabBooking.setOnClickListener(v -> {
            Intent intent = new Intent(this, BookingHistoryActivity.class);
            startActivity(intent);
        });
    }
}

