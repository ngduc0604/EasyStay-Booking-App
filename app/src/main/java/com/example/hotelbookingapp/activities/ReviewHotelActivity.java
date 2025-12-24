package com.example.hotelbookingapp.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hotelbookingapp.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ReviewHotelActivity extends AppCompatActivity {
    public static final String EXTRA_HOTEL_ID = "hotelId";
    public static final String EXTRA_BOOKING_ID = "bookingId";

    private String hotelId;
    private String bookingId;

    private RatingBar ratingBar;
    private EditText edtComment;
    private Button btnSubmit;
    private Button btnDelete; // <-- nút xóa
    private ImageView btnBack;

    private String username;
    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Nhận từ BookingDetailActivity (nếu sửa)
    private String mode;            // "create" hoặc "edit"
    private String reviewDocId;     // có thể null; nếu null ta dùng docId = bookingId_userId

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_review_hotel);

        // --- Firebase ---
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        // --- Nhận extras ---
        Intent intent = getIntent();
        hotelId = intent.getStringExtra(EXTRA_HOTEL_ID);
        bookingId = intent.getStringExtra(EXTRA_BOOKING_ID);
        username=intent.getStringExtra("userName");
        mode = intent.getStringExtra("mode");
        reviewDocId = intent.getStringExtra("reviewDocId");

        if (hotelId == null || bookingId == null) {
            Toast.makeText(this, "Thiếu dữ liệu đặt phòng.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // --- Bind view ---
        ratingBar = findViewById(R.id.ratingBar);
        edtComment = findViewById(R.id.edtComment);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnDelete = findViewById(R.id.btnDelete); // <-- nút xóa
        btnBack = findViewById(R.id.back_to_bookdetail);

        btnSubmit.setEnabled(false);
        btnBack.setOnClickListener(v -> finish());

        // Prefill khi sửa
        if ("edit".equalsIgnoreCase(mode)) {
            float oldStar = intent.getFloatExtra("star", 0f);
            String oldContent = intent.getStringExtra("content");
            if (oldStar > 0f) ratingBar.setRating(oldStar);
            if (oldContent != null) edtComment.setText(oldContent);
            // Hiện nút xóa khi đang sửa
            btnDelete.setVisibility(Button.VISIBLE);
        } else {
            btnDelete.setVisibility(Button.GONE);
        }

        // Enable nút gửi khi hợp lệ
        TextWatcher watcher = new SimpleTextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                toggleSubmit();
            }
        };
        edtComment.addTextChangedListener(watcher);
        ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> toggleSubmit());

        // GỬI: chưa có thì thêm mới, có rồi thì cập nhật (docId = bookingId_userId)
        btnSubmit.setOnClickListener(v -> submitReviewCreateOrUpdate());

        // XÓA: chỉ khi ở chế độ sửa
        btnDelete.setOnClickListener(v -> showDeleteDialogInReview());
    }

    private void toggleSubmit() {
        boolean ok = ratingBar.getRating() > 0f
                && edtComment.getText() != null
                && edtComment.getText().toString().trim().length() > 0;
        btnSubmit.setEnabled(ok);
    }

    /**
     * Lưu review theo nguyên tắc:
     * - docId = bookingId_userId
     * - Nếu doc chưa tồn tại -> set(...) kèm createdAt
     * - Nếu đã tồn tại -> update(...) kèm updatedAt
     */
    private void submitReviewCreateOrUpdate() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để gửi đánh giá.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        float star = ratingBar.getRating();
        String content = edtComment.getText() == null ? "" : edtComment.getText().toString().trim();

        btnSubmit.setEnabled(false);

        String docId = bookingId + "_" + userId;
        DocumentReference ref = db.collection("ReviewHotel").document(docId);

        ref.get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // ĐÃ CÓ: cập nhật
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("star", star);
                        updates.put("content", content);
                        updates.put("updatedAt", FieldValue.serverTimestamp());

                        ref.update(updates)
                                .addOnSuccessListener(unused -> {
                                    recalculateHotelRating(hotelId);
                                    Toast.makeText(this, "Đã cập nhật đánh giá."+hotelId, Toast.LENGTH_SHORT).show();
                                    sendResultAndFinish(star, content, false, docId);

                                })
                                .addOnFailureListener(e -> {
                                    btnSubmit.setEnabled(true);
                                    Toast.makeText(this, "Cập nhật thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });

                    } else {
                        // CHƯA CÓ: thêm mới
                        Map<String, Object> data = new HashMap<>();
                        data.put("hotelid", hotelId);
                        data.put("bookingid", bookingId);
                        data.put("userid", userId);
                        data.put("star", star);
                        data.put("username",username);
                        data.put("content", content);
                        data.put("createdAt", FieldValue.serverTimestamp());

                        ref.set(data)
                                .addOnSuccessListener(unused -> {
                                    recalculateHotelRating(hotelId);
                                    Toast.makeText(this, "Đã gửi đánh giá. Cảm ơn bạn!", Toast.LENGTH_SHORT).show();
                                    sendResultAndFinish(star, content, false, docId);
                                })
                                .addOnFailureListener(e -> {
                                    btnSubmit.setEnabled(true);
                                    Toast.makeText(this, "Gửi đánh giá thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    Toast.makeText(this, "Lỗi kiểm tra đánh giá: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // --- Xóa trong màn review ---
    private void showDeleteDialogInReview() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xóa đánh giá")
                .setMessage("Bạn có chắc chắn muốn xóa đánh giá này không?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteReviewInReviewScreen())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteReviewInReviewScreen() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Bạn cần đăng nhập.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Ưu tiên reviewDocId nếu được truyền từ trước; nếu không, dùng key cố định bookingId_userId
        String uid = user.getUid();
        String docId = (reviewDocId != null && !reviewDocId.isEmpty())
                ? reviewDocId
                : (bookingId + "_" + uid);

        btnDelete.setEnabled(false);
        db.collection("ReviewHotel").document(docId)
                .delete()
                .addOnSuccessListener(unused -> {
                    recalculateHotelRating(hotelId);
                    Toast.makeText(this, "Đã xóa đánh giá.", Toast.LENGTH_SHORT).show();
                    // Thông báo về màn trước để refresh
                    sendResultAndFinish(0f, null, true, docId);
                })
                .addOnFailureListener(e -> {
                    btnDelete.setEnabled(true);
                    Toast.makeText(this, "Xóa thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void sendResultAndFinish(Float star, String content, boolean deleted, String docId) {
        Intent result = new Intent();
        result.putExtra("hotelId", hotelId);
        result.putExtra("bookingId", bookingId);
        result.putExtra("deleted", deleted);
        result.putExtra("reviewDocId", docId);
        if (!deleted) {
            result.putExtra("rating", star);
            result.putExtra("comment", content);
        }
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    // TextWatcher rút gọn
    public abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void afterTextChanged(Editable s) {}
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

    private void recalculateHotelRating(String hotelId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("ReviewHotel")
                .whereEqualTo("hotelid", hotelId)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    int ratingCount = querySnapshot.size();
                    double totalStar = 0.0;

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Double star = doc.getDouble("star");
                        if (star != null) {
                            totalStar += star;
                        }
                    }

                    double rating = 0.0;
                    if (ratingCount > 0) {
                        double avgStar = totalStar / ratingCount; // 0–5
                        rating = Math.round(avgStar * 2 * 10) / 10.0; // 0–10, 1 decimal
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("rating", rating);
                    updates.put("ratingCount", ratingCount);

                    db.collection("hotels")
                            .document(hotelId)
                            .update(updates);
                })
                .addOnFailureListener(e ->
                        Log.e("HotelRating", "Recalculate rating failed", e)
                );
    }

}
