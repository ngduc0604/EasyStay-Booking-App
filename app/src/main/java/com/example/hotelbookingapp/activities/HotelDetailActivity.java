package com.example.hotelbookingapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import org.osmdroid.config.Configuration;
import org.osmdroid.views.MapView;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.hotelbookingapp.R;
import com.example.hotelbookingapp.adapters.ImageAdapter;
import com.example.hotelbookingapp.adapters.ReviewAdapter;
import com.example.hotelbookingapp.dialogs.ImagePreviewDialog;
import com.example.hotelbookingapp.dialogs.MapFullScreenDialog;
import com.example.hotelbookingapp.fragments.DatePickerFragment;
import com.example.hotelbookingapp.models.Hotel;
import com.example.hotelbookingapp.models.Review;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HotelDetailActivity extends AppCompatActivity {
    //private FrameLayout mapContainer;
    private View mapOverlay;
    private TextView tvHotelName, tvRating, tvStars, tvAddress, tvDescription,tvcheckIn,tvcheckOut,tvOfNight,tvPrice;
    private ImageView btnBack,btnHeart,btnShare;
    private MapView mapView;
    private GridView gridPhotos;
    private Button btnSelectRoom;
    private boolean isFavorite = false;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private long total;

    private Calendar checkInDate = Calendar.getInstance();
    private Calendar checkOutDate = Calendar.getInstance();
    private boolean checkOutSet = false;


    private RecyclerView rvReviews;
    private TextView tvSeeMoreReviews;
    private ReviewAdapter reviewAdapter;
    private static final int PREVIEW_LIMIT = 3;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actitvity_hotel_detail);

        mappingViews();
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Hotel hotel = (Hotel) getIntent().getSerializableExtra("selected_hotel");
        if (hotel != null) {
            populateHotelInfo(hotel);
            loadPreviewReviews(hotel.id);
            setupSeeMoreClick(hotel.id);
            if (mAuth.getCurrentUser() != null) {
                checkFavoriteStatus(hotel.id);
            }
        }
        btnBack.setOnClickListener(v -> finish());

        mapOverlay.setOnClickListener(v -> {
            if (hotel != null) {
                MapFullScreenDialog dialog = new MapFullScreenDialog(hotel.latitude, hotel.longitude, hotel.name);
                dialog.show(getSupportFragmentManager(), "map_full_screen");
            }
        });

        btnSelectRoom.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(this, "Vui lòng đăng nhập để sử dụng chức năng này", Toast.LENGTH_SHORT).show();
                new Intent(this, LoginActivity.class);
                return;
            }
            Hotel selectedHotel = getIntent().getSerializableExtra("selected_hotel") != null ?
                    (Hotel) getIntent().getSerializableExtra("selected_hotel") : null;
//            String dateFrom = getIntent().getStringExtra("date_from");
//            String dateTo = getIntent().getStringExtra("date_to");
            if (selectedHotel == null) {
                Toast.makeText(this, "Không tìm thấy thông tin khách sạn", Toast.LENGTH_SHORT).show();
                return;
            }

            String dateFrom = tvcheckIn.getText().toString().trim();
            String dateTo = tvcheckOut.getText().toString().trim();

            Intent intent = new Intent(this, ConfirmAccountActivity.class);
            intent.putExtra("selected_hotel", selectedHotel);
            intent.putExtra("date_from", dateFrom);
            intent.putExtra("date_to", dateTo);
            intent.putExtra("total_price", total);
            startActivity(intent);
        });
        btnHeart.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(this, "Vui lòng đăng nhập để sử dụng chức năng này", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                return;
            }
            toggleFavorite(hotel.id);
        });
        btnShare.setOnClickListener(v -> {
            String hotelUrl = "https://esasystay.com//hotel?id="+hotel.id;
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Chia sẻ khách sạn");
            shareIntent.putExtra(Intent.EXTRA_TEXT, hotelUrl);
            startActivity(Intent.createChooser(shareIntent, "Chia sẻ liên kết qua"));
        });

        tvcheckIn.setOnClickListener(v -> {
            long minDate = System.currentTimeMillis();
            DatePickerFragment datePickerFragment = DatePickerFragment.newInstance((view, year, month, dayOfMonth) -> {
                checkInDate.set(year, month, dayOfMonth);
                String selectedDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year);
                tvcheckIn.setText(selectedDate);

                if (!checkOutSet || !checkOutDate.after(checkInDate)) {
                    checkOutDate.setTimeInMillis(checkInDate.getTimeInMillis());
                    checkOutDate.add(Calendar.DAY_OF_MONTH, 1);
                    String coDate = String.format("%02d/%02d/%04d", checkOutDate.get(Calendar.DAY_OF_MONTH),
                            checkOutDate.get(Calendar.MONTH) + 1, checkOutDate.get(Calendar.YEAR));
                    tvcheckOut.setText(coDate);
                    checkOutSet = true;
                }
                updatePriceAndNights();
            }, checkInDate.getTimeInMillis());

            datePickerFragment.show(getSupportFragmentManager(), "checkInPicker");
        });

        tvcheckOut.setOnClickListener(v -> {
            long minDate = checkInDate.getTimeInMillis() + 86400000L; // Phải sau checkin ít nhất 1 ngày

            DatePickerFragment datePickerFragment = DatePickerFragment.newInstance((view, year, month, dayOfMonth) -> {
                checkOutDate.set(year, month, dayOfMonth);
                String selectedDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year);
                tvcheckOut.setText(selectedDate);
                checkOutSet = true;

                updatePriceAndNights();
            }, minDate);

            datePickerFragment.show(getSupportFragmentManager(), "checkOutPicker");
        });


    }

    private void mappingViews() {
        tvHotelName = findViewById(R.id.tvHotelName);
        tvRating = findViewById(R.id.tvRating);
        tvStars = findViewById(R.id.tvStars);
        tvAddress = findViewById(R.id.tvAddress);
        tvDescription = findViewById(R.id.tvDescription);
        btnBack = findViewById(R.id.btnBack);
        mapView = findViewById(R.id.imgMap);
        btnSelectRoom = findViewById(R.id.btnSelectRoom);
        gridPhotos = findViewById(R.id.gridPhotos);
       // mapContainer = findViewById(R.id.mapContainer);
        mapOverlay = findViewById(R.id.mapOverlay);
        tvcheckIn=findViewById(R.id.tvCheckIn);
        tvcheckOut=findViewById(R.id.tvCheckOut);
        tvOfNight=findViewById(R.id.tv_numberNight);
        tvPrice=findViewById(R.id.tv_price);
        btnHeart = findViewById(R.id.btnHeart);
        btnShare=findViewById(R.id.btn_Share);

        rvReviews = findViewById(R.id.rvReviews);
        tvSeeMoreReviews = findViewById(R.id.tvSeeMoreReviews);
        reviewAdapter = new ReviewAdapter();
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setAdapter(reviewAdapter);
        tvSeeMoreReviews.setVisibility(View.GONE);
    }

    private void populateHotelInfo(Hotel hotel) {
        tvHotelName.setText(hotel.name);
        tvRating.setText(String.valueOf(hotel.rating));
        tvStars.setText(hotel.stars + " ★");
        tvAddress.setText(hotel.address);
        tvDescription.setText(hotel.description);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String dateFrom = getIntent().getStringExtra("date_from");
        String dateTo = getIntent().getStringExtra("date_to");

        if (dateFrom == null || dateFrom.isEmpty()) {
            dateFrom = sdf.format(new Date());
        }
        if (dateTo == null || dateTo.isEmpty()) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, 1);
            dateTo = sdf.format(cal.getTime());
        }


        tvcheckIn.setText(dateFrom);
        tvcheckOut.setText(dateTo);

        try {
            Date d1 = sdf.parse(dateFrom);
            Date d2 = sdf.parse(dateTo);
            if (d1 != null) checkInDate.setTime(d1);
            if (d2 != null) {
                checkOutDate.setTime(d2);
                checkOutSet = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Date d1 = sdf.parse(dateFrom);
            Date d2 = sdf.parse(dateTo);
            int numberOfNights = 1;
            if (d1 != null && d2 != null) {
                long diff = d2.getTime() - d1.getTime();
                numberOfNights = Math.max(1, (int) (diff / (1000 * 60 * 60 * 24)));
            }
            tvOfNight.setText("Giá cho "+numberOfNights + " đêm");
            //double price= hotel.priceNew*numberOfNights;
            total = hotel.priceNew * numberOfNights;
            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            tvPrice.setText(format.format(total));
        } catch (Exception e) {
            e.printStackTrace();
            tvOfNight.setText("Giá cho 1 đêm");
        }

        ImageAdapter adapter = new ImageAdapter(this, hotel.imageUrls, position -> {
            ImagePreviewDialog dialog = new ImagePreviewDialog(hotel.imageUrls, position);
            dialog.show(getSupportFragmentManager(), "image_preview");
        });
        gridPhotos.setAdapter(adapter);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        GeoPoint hotelLocation = new GeoPoint(hotel.latitude, hotel.longitude);
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(hotelLocation);

        Marker marker = new Marker(mapView);
        marker.setPosition(hotelLocation);
        marker.setTitle(hotel.name);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(marker);
    }
    private void loadPreviewReviews(String hotelId) {
        db.collection("ReviewHotel")
                .whereEqualTo("hotelid", hotelId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(PREVIEW_LIMIT + 1) // lấy dư 1 để biết có “xem thêm” không
                .get()
                .addOnSuccessListener(snap -> {
                    List<Review> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Review r = new Review();
                        Double star = d.getDouble("star");
                        r.star = star != null ? star.floatValue() : 0f;
                        r.content = d.getString("content");
                        r.userName = d.getString("username");  // nếu có
                        Timestamp ts = d.getTimestamp("createdAt");
                        r.createdAt = ts != null ? ts.toDate() : null;
                        r.userId = d.getString("userid");
                        list.add(r);
                    }
                    boolean hasMore = list.size() > PREVIEW_LIMIT;
                    if (hasMore) list = list.subList(0, PREVIEW_LIMIT);

                    reviewAdapter.setItems(list);
                    tvSeeMoreReviews.setVisibility(hasMore ? View.VISIBLE : View.GONE);

                    // Nếu không có review nào, có thể ẩn cả header + link (tuỳ UX)
                    if (list.isEmpty()) {
                        tvSeeMoreReviews.setVisibility(View.GONE);
                        // Ví dụ: ẩn header nếu muốn
                         findViewById(R.id.tvReviewsHeader).setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    // Giữ im lặng hoặc log nhẹ để debug
                    // Log.e("HotelDetail", "loadPreviewReviews error", e);
                    reviewAdapter.setItems(new ArrayList<>());
                    tvSeeMoreReviews.setVisibility(View.GONE);
                });
    }



    private void setupSeeMoreClick(String hotelId) {
        tvSeeMoreReviews.setOnClickListener(v -> {
            Intent i = new Intent(this, ReviewsActivity.class);
            i.putExtra("hotelId", hotelId);
            startActivity(i);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    private void toggleFavorite(String hotelId) {
        String uid = mAuth.getCurrentUser().getUid();
        String docId = uid + "_" + hotelId;

        if (isFavorite) {
            db.collection("favorites").document(docId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        isFavorite = false;
                        btnHeart.setImageResource(R.drawable.ic_hear_white);
                        Toast.makeText(this, "Đã bỏ khỏi yêu thích", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                    });
        } else {
            Map<String, Object> favData = new HashMap<>();
            favData.put("userId", uid);
            favData.put("hotelId", hotelId);
            db.collection("favorites").document(docId)
                    .set(favData)
                    .addOnSuccessListener(aVoid -> {
                        isFavorite = true;
                        btnHeart.setImageResource(R.drawable.ic_heart_red);
                        Toast.makeText(this, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                    });
        }
    }
    private void checkFavoriteStatus(String hotelId) {
        String uid = mAuth.getCurrentUser().getUid();
        String docId = uid + "_" + hotelId;

        db.collection("favorites").document(docId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        isFavorite = true;
                        btnHeart.setImageResource(R.drawable.ic_heart_red);
                    } else {
                        isFavorite = false;
                        btnHeart.setImageResource(R.drawable.ic_hear_white);
                    }
                });
    }

    private void updatePriceAndNights() {
        try {
            long diff = checkOutDate.getTimeInMillis() - checkInDate.getTimeInMillis();
            int numberOfNights = Math.max(1, (int) (diff / (1000 * 60 * 60 * 24)));
            tvOfNight.setText("Giá cho " + numberOfNights + " đêm");

            Hotel hotel = (Hotel) getIntent().getSerializableExtra("selected_hotel");
            if (hotel != null) {
                total = hotel.priceNew * numberOfNights;
                NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                tvPrice.setText(format.format(total));
            }
        } catch (Exception e) {
            e.printStackTrace();
            tvOfNight.setText("Giá cho 1 đêm");
        }
    }

}

