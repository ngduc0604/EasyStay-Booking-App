package com.example.hotelbookingapp;


import android.content.Context;
import android.content.Intent;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.hotelbookingapp.activities.LoginActivity;
import com.example.hotelbookingapp.activities.UserHomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;


public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        //uploadHotelsFromAssets(this);
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateCompletedBookings();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            startActivity(new Intent(this, UserHomeActivity.class));
            finish();
        }
    }

    private void updateCompletedBookings() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference bookingsRef = db.collection("bookings");

        bookingsRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                String status = snapshot.getString("status");
                String checkOutDateStr = snapshot.getString("dateCheckOut");

                if (checkOutDateStr == null || status == null) continue;
                if (!status.equals("confirm")) continue;

                try {

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    Date checkOutDate = sdf.parse(checkOutDateStr);
                    Date today = new Date();

                    if (checkOutDate != null && checkOutDate.before(today)) {
                        // Ngày trả phòng đã qua -> cập nhật trạng thái
                        snapshot.getReference().update("status", "complete")
                                .addOnSuccessListener(aVoid ->
                                        Log.d("BookingUpdate", "Đã cập nhật " + snapshot.getId() + " thành complete"))
                                .addOnFailureListener(e ->
                                        Log.e("BookingUpdate", "Lỗi cập nhật: " + e.getMessage()));
                    }

                } catch (ParseException e) {
                    Log.e("BookingUpdate", "Lỗi parse ngày: " + e.getMessage());
                }
            }
        });
    }


//    public void uploadHotelsFromAssets(Context context) {
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//
//        try {
//            InputStream is = context.getAssets().open("hotels.json");
//            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
//            StringBuilder sb = new StringBuilder();
//            String line;
//            while ((line = br.readLine()) != null) {
//                sb.append(line);
//            }
//            br.close();
//            is.close();
//
//            JSONObject root = new JSONObject(sb.toString());
//            JSONObject hotels = root.getJSONObject("hotels");
//            Iterator<String> keys = hotels.keys();
//            int total = 0;
//
//            while (keys.hasNext()) {
//                String docId = keys.next();
//                JSONObject hotelObj = hotels.getJSONObject(docId);
//                Map<String, Object> data = new HashMap<>();
//
//                Iterator<String> fieldKeys = hotelObj.keys();
//                while (fieldKeys.hasNext()) {
//                    String field = fieldKeys.next();
//                    Object value = hotelObj.get(field);
//
//                    if (value instanceof JSONArray) {
//                        JSONArray jsonArray = (JSONArray) value;
//                        List<Object> list = new ArrayList<>();
//                        for (int i = 0; i < jsonArray.length(); i++) {
//                            list.add(jsonArray.get(i));
//                        }
//                        data.put(field, list);
//                    } else {
//                        data.put(field, value);
//                    }
//                }
//
//                db.collection("hotels")
//                        .document(docId)
//                        .set(data)
//                        .addOnSuccessListener(aVoid -> Log.d("UploadHotels", "Uploaded: " + docId))
//                        .addOnFailureListener(e -> Log.e("UploadHotels", "Failed: " + docId, e));
//
//                total++;
//            }
//
//            Toast.makeText(context, "Đã bắt đầu upload " + total + " khách sạn", Toast.LENGTH_SHORT).show();
//
//        } catch (Exception e) {
//            Log.e("UploadHotels", "Lỗi khi upload", e);
//            Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
//        }
//    }
}
