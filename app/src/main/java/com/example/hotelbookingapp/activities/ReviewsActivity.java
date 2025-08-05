package com.example.hotelbookingapp.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hotelbookingapp.R;
import com.example.hotelbookingapp.adapters.ReviewAdapter;
import com.example.hotelbookingapp.models.Review;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ReviewsActivity extends AppCompatActivity {

    private RecyclerView rvAllReviews;
    private Button btnLoadMore;
    private ReviewAdapter adapter;
    private FirebaseFirestore db;

    private String hotelId;

    private static final int PAGE_SIZE = 10;
    private DocumentSnapshot lastVisible;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private ImageView back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviews);
        Mapping();
        hotelId = getIntent().getStringExtra("hotelId");
        adapter = new ReviewAdapter();
        rvAllReviews.setLayoutManager(new LinearLayoutManager(this));
        rvAllReviews.setAdapter(adapter);

        loadFirstPage();
        back.setOnClickListener(v -> finish());
        btnLoadMore.setOnClickListener(v -> loadMore());
    }


    private void Mapping(){
        db = FirebaseFirestore.getInstance();
        rvAllReviews = findViewById(R.id.rvAllReviews);
        btnLoadMore = findViewById(R.id.btnLoadMore);
        back=findViewById(R.id.backtodetail);
    }
    private void loadFirstPage() {
        if (isLoading) return;
        isLoading = true;

        db.collection("ReviewHotel")
                .whereEqualTo("hotelid", hotelId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE)
                .get()
                .addOnSuccessListener(snap -> {
                    isLoading = false;
                    List<Review> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Review r = new Review();
                        Double star = d.getDouble("star");
                        r.star = star != null ? star.floatValue() : 0f;
                        r.content = d.getString("content");
                        Timestamp ts = d.getTimestamp("createdAt");
                        r.userName=d.getString("username");
                        r.createdAt = ts != null ? ts.toDate() : null;
                        r.userId = d.getString("userid");
                        list.add(r);
                    }
                    adapter.setItems(list);
                    lastVisible = snap.isEmpty() ? null : snap.getDocuments().get(snap.size() - 1);
                    hasMore = snap.size() == PAGE_SIZE;
                    btnLoadMore.setVisibility(hasMore ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> isLoading = false);
    }

    private void loadMore() {
        if (isLoading || !hasMore || lastVisible == null) return;
        isLoading = true;

        db.collection("ReviewHotel")
                .whereEqualTo("hotelid", hotelId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .startAfter(lastVisible)
                .limit(PAGE_SIZE)
                .get()
                .addOnSuccessListener(snap -> {
                    isLoading = false;
                    List<Review> more = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Review r = new Review();
                        Double star = d.getDouble("star");
                        r.star = star != null ? star.floatValue() : 0f;
                        r.content = d.getString("content");
                        Timestamp ts = d.getTimestamp("createdAt");
                        r.createdAt = ts != null ? ts.toDate() : null;
                        r.userId = d.getString("userid");
                        r.userName=d.getString("username");
                        more.add(r);
                    }
                    if (!more.isEmpty()) {
                        adapter.addItems(more);
                    }
                    lastVisible = snap.isEmpty() ? null : snap.getDocuments().get(snap.size() - 1);
                    hasMore = snap.size() == PAGE_SIZE;
                    btnLoadMore.setVisibility(hasMore ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> isLoading = false);
    }
}

