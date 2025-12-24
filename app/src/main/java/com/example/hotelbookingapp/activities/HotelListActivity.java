package com.example.hotelbookingapp.activities;

import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hotelbookingapp.R;
import com.example.hotelbookingapp.adapters.HotelAdapter;
import com.example.hotelbookingapp.models.Hotel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HotelListActivity extends AppCompatActivity {

    private RecyclerView recyclerHotelList;
    private HotelAdapter hotelAdapter;
    private List<Hotel> hotelList = new ArrayList<>();
    private List<Hotel> originalHotelList = new ArrayList<>();

    private FirebaseFirestore db;
    private ImageView btnBack;
    private Spinner spinnerSort, spinnerFilter;
    private int numberOfNights = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hotel_list);
        btnBack=findViewById(R.id.btnBackHome);
        recyclerHotelList = findViewById(R.id.recyclerHotelList);
        recyclerHotelList.setLayoutManager(new LinearLayoutManager(this));

        spinnerSort = findViewById(R.id.spinnerSort);
        spinnerFilter = findViewById(R.id.spinnerFilter);

        String area = getIntent().getStringExtra("area_name");
        String fromDate = getIntent().getStringExtra("date_from");
        String toDate = getIntent().getStringExtra("date_to");
        numberOfNights = calculateNights(fromDate, toDate);

        TextView tvArea = findViewById(R.id.tvArea);
        TextView tvDateRange = findViewById(R.id.tvDateRange);
        tvArea.setText(area.toUpperCase());
        if(fromDate.equals("")&&toDate.equals("")){
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            Date today = calendar.getTime();
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            Date tomorrow = calendar.getTime();
            String todayStr = sdf.format(today);
            String tomorrowStr = sdf.format(tomorrow);
            tvDateRange.setText(todayStr + " - " + tomorrowStr);
        }else{
            tvDateRange.setText(fromDate + " - " + toDate);
        }

        hotelAdapter = new HotelAdapter(this, hotelList, numberOfNights, fromDate, toDate);
        recyclerHotelList.setAdapter(hotelAdapter);


        db = FirebaseFirestore.getInstance();
        fetchHotels(area);

        setupSpinners();
        btnBack.setOnClickListener(v -> finish());
    }

    private void fetchHotels(String areaName) {
        db.collection("hotels")
                .whereIn("area",  Arrays.asList(areaName,areaName.toLowerCase()))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    hotelList.clear();
                    originalHotelList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Hotel hotel = doc.toObject(Hotel.class);
                        hotelList.add(hotel);
                        originalHotelList.add(hotel); // dùng để reset khi lọc
                    }
                    hotelAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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


    private void setupSpinners() {
        ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(this,
                R.array.sort_options, android.R.layout.simple_spinner_item);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(sortAdapter);

        ArrayAdapter<CharSequence> filterAdapter = ArrayAdapter.createFromResource(this,
                R.array.filter_star_options, android.R.layout.simple_spinner_item);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(filterAdapter);

        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, android.view.View view, int position, long id) {
                if (position == 0) {
                    Collections.sort(hotelList, Comparator.comparingDouble(Hotel::getPriceNew));
                } else if (position == 1) {
                    Collections.sort(hotelList, (h1, h2) -> Double.compare(h2.getPriceNew(), h1.getPriceNew()));
                }
                hotelAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, android.view.View view, int position, long id) {
                hotelList.clear();

                if (position == 0) {
                    hotelList.addAll(originalHotelList); // Tất cả
                } else {
                    int starFilter = position; // vì item "1 sao" ở vị trí 1
                    for (Hotel h : originalHotelList) {
                        if (h.getStars() == starFilter) {
                            hotelList.add(h);
                        }
                    }
                }

                // Kết hợp với sắp xếp hiện tại
                spinnerSort.setSelection(spinnerSort.getSelectedItemPosition());
                hotelAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }
}
