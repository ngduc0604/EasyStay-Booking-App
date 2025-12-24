package com.example.hotelbookingapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.hotelbookingapp.R;
import com.example.hotelbookingapp.models.Hotel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView mapView;
    private IMapController mapController;
    private Location userLocation;
    private BottomSheetDialog hotelDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_map);

        mapView = findViewById(R.id.map_View);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapController = mapView.getController();
        mapController.setZoom(12.0);
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        requestPermissionsIfNecessary();
    }

    private void requestPermissionsIfNecessary() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        } else {
            getUserLocationAndLoadHotels();
        }
    }

    private void getUserLocationAndLoadHotels() {
        FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Ứng dụng cần quyền vị trí", Toast.LENGTH_SHORT).show();
            return;
        }

        locationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                this.userLocation = location;

                GeoPoint userGeoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                mapController.setCenter(userGeoPoint);

                Drawable icon = ContextCompat.getDrawable(this, R.drawable.ic_location_red);
                Bitmap smallBitmap = Bitmap.createScaledBitmap(((BitmapDrawable) icon).getBitmap(), 48, 48, false);
                Drawable scaledIcon = new BitmapDrawable(getResources(), smallBitmap);

                Marker userMarker = new Marker(mapView);
                userMarker.setPosition(userGeoPoint);
                userMarker.setTitle("Vị trí của bạn");
                userMarker.setIcon(scaledIcon);
                userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                mapView.getOverlays().add(userMarker);

                loadNearbyHotels();
            } else {
                Toast.makeText(this, "Không thể lấy vị trí hiện tại", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadNearbyHotels() {
        FirebaseFirestore.getInstance()
                .collection("hotels")
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    for (QueryDocumentSnapshot snapshot : querySnapshots) {
                        Hotel hotel = snapshot.toObject(Hotel.class);
                        if (hotel.getLatitude() != 0.0 && hotel.getLongitude() != 0.0) {
                            Location hotelLoc = new Location("");
                            hotelLoc.setLatitude(hotel.getLatitude());
                            hotelLoc.setLongitude(hotel.getLongitude());

                            float distance = userLocation.distanceTo(hotelLoc) / 1000;
                            if (distance <= 20) {
                                addHotelMarker(hotel);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải khách sạn", Toast.LENGTH_SHORT).show());
    }

    private void addHotelMarker(Hotel hotel) {
        Drawable icon = ContextCompat.getDrawable(this, R.drawable.ic_hotel_location);
        Bitmap smallBitmap = Bitmap.createScaledBitmap(((BitmapDrawable) icon).getBitmap(), 48, 48, false);
        Drawable scaledIcon = new BitmapDrawable(getResources(), smallBitmap);

        GeoPoint point = new GeoPoint(hotel.getLatitude(), hotel.getLongitude());
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setIcon(scaledIcon);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        String formattedPrice = NumberFormat.getNumberInstance(Locale.US).format(hotel.priceNew) + "đ/đêm";
        marker.setTitle(hotel.getName() + "\n" + formattedPrice);

        marker.setOnMarkerClickListener((marker1, mapView1) -> {
            showHotelBottomDialog(hotel);
            return true;
        });

        mapView.getOverlays().add(marker);
        mapView.invalidate();
    }

    private void showHotelBottomDialog(Hotel hotel) {
        if (hotelDialog != null && hotelDialog.isShowing()) {
            hotelDialog.dismiss();
        }
        hotelDialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_hotel_preview, null);
        hotelDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        hotelDialog.setContentView(view);
        View parent = (View) view.getParent();
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parent);
        parent.setBackgroundColor(Color.TRANSPARENT); // Tránh nền đen sát mép

        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        ImageView image = view.findViewById(R.id.hotel_image);
        TextView name = view.findViewById(R.id.hotel_name);
        TextView price = view.findViewById(R.id.hotel_price);
        TextView address = view.findViewById(R.id.tv_Hotel_Address);
        TextView rating = view.findViewById(R.id.tv_Hotel_Rating);
        TextView star = view.findViewById(R.id.tv_Hotel_Stars);
        TextView oldPrice = view.findViewById(R.id.tv_Old_Price);

        name.setText(hotel.getName().toString());
        price.setText(format.format(hotel.getPriceNew()) + " / đêm");
        address.setText(hotel.getAddress().toString());
        rating.setText(String.valueOf(hotel.getRating()));
        star.setText(hotel.getStars() + "★");
        oldPrice.setText(format.format(hotel.getPriceOld()) + " / đêm");
        oldPrice.setPaintFlags(oldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        if (hotel.getImageUrls() != null && !hotel.getImageUrls().isEmpty()) {
            Glide.with(this).load(hotel.getImageUrls().get(0)).into(image);
        }

        view.setOnClickListener(v -> {
            Intent intent = new Intent(MapActivity.this, HotelDetailActivity.class);
            intent.putExtra("selected_hotel", (Serializable) hotel);
            startActivity(intent);
            hotelDialog.dismiss();
        });


        hotelDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            getUserLocationAndLoadHotels();
        }
    }
}
