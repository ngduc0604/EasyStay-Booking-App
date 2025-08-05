package com.example.hotelbookingapp.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotelbookingapp.R;
import com.example.hotelbookingapp.fragments.DatePickerFragment;
import com.example.hotelbookingapp.models.Area;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;

public class UserHomeActivity extends AppCompatActivity {
    private final Area[] areas = {
            new Area(R.drawable.img_hanoi, "Hà Nội"),
            new Area(R.drawable.img_hcm, "Hồ Chí Minh"),
            new Area(R.drawable.img_danang, "Đà Nẵng"),
            new Area(R.drawable.img_sapa, "Sa Pa"),
            new Area(R.drawable.img_hue, "Huế")
    };
    private LinearLayout layoutLastSearch,continueSearch;
    private TextView tvLastSearchName,tvLastSearchDate;

    private ImageView imageViewLastSearch;


    private LinearLayout tabSearch, tabSaved, tabBooking, tabAccount;
    private LinearLayout imageContainer;
    private ScrollView scrollView;
    private EditText etLocation;
    private FirebaseUser firebaseUser;
    private TextView tvDateFrom, tvDateTo,tvGenius;
    private Button btnSearch;

    private String dateFrom = "", dateTo = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_home);
        mapping();
        loadLastSearchInfo();
        setupBottomTabs();
        setupSearchBox();
        showAreaList();

    }

    private void mapping() {
        scrollView = findViewById(R.id.scroll_content);
        imageContainer = findViewById(R.id.imageContainer);
        tvGenius=findViewById(R.id.tv_genius);
        View bottomNav = findViewById(R.id.layout_bottom_nav); // lấy include bottom nav
        tabSearch = bottomNav.findViewById(R.id.tab_search);
        tabSaved = bottomNav.findViewById(R.id.tab_saved);
        tabBooking = bottomNav.findViewById(R.id.tab_booking);
        tabAccount = bottomNav.findViewById(R.id.tab_account);

        View searchBox = findViewById(R.id.searchBox);
        etLocation = searchBox.findViewById(R.id.et_location);
        tvDateFrom = searchBox.findViewById(R.id.tv_date_from);
        tvDateTo = searchBox.findViewById(R.id.tv_date_to);
        btnSearch = searchBox.findViewById(R.id.btn_search);
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        tvGenius.setText(firebaseUser.getDisplayName()+" ơi, bạn đang là Genius Cấp 1 trong chương trình khách hàng thân thiết của chúng tôi");
        layoutLastSearch = findViewById(R.id.layout_last_search);
        continueSearch=findViewById(R.id.continue_search);
        tvLastSearchName=findViewById(R.id.tv_last_search_name);
        tvLastSearchDate=findViewById(R.id.tv_last_search_date);
        imageViewLastSearch=findViewById(R.id.image_last_search);

    }

    private void setupBottomTabs() {
        tabSearch.setOnClickListener(v -> {
            scrollView.post(() -> {
                scrollView.smoothScrollTo(0, etLocation.getTop());
                etLocation.requestFocus();
            });
        });
        tabAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UserHomeActivity.this, AccountActivity.class);
                startActivity(intent);
            }
        });
        tabSaved.setOnClickListener(v -> {
            Intent intent = new Intent(UserHomeActivity.this, SavedHotelsActivity.class);
            startActivity(intent);
        });
        tabBooking.setOnClickListener(v -> {
            Intent intent = new Intent(this, BookingHistoryActivity.class);
            startActivity(intent);
        });
    }

private void setupSearchBox() {
    tvDateFrom.setOnClickListener(v -> {
        long todayMillis = System.currentTimeMillis() - 1000;
        DatePickerFragment datePicker = DatePickerFragment.newInstance((view, year, month, day) -> {
            dateFrom = String.format("%02d/%02d/%04d", day, month + 1, year);
            tvDateFrom.setText(dateFrom);
            dateTo = "";
            tvDateTo.setText("");
            tvDateTo.setHint("Ngày đi");
        }, todayMillis);

        datePicker.show(getSupportFragmentManager(), "dateFromPicker");
    });

    tvDateTo.setOnClickListener(v -> {
        if (dateFrom.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ngày nhận phòng trước", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] parts = dateFrom.split("/");
        int day = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]) - 1;
        int year = Integer.parseInt(parts[2]);

        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        cal.add(Calendar.DAY_OF_MONTH, 1); // Ngày đi phải sau ngày đến ít nhất 1 ngày

        DatePickerFragment datePicker = DatePickerFragment.newInstance((view, y, m, d) -> {
            dateTo = String.format("%02d/%02d/%04d", d, m + 1, y);
            tvDateTo.setText(dateTo);
        }, cal.getTimeInMillis());

        datePicker.show(getSupportFragmentManager(), "dateToPicker");
    });

    btnSearch.setOnClickListener(v -> {
        String area = etLocation.getText().toString().trim();
        if (area.isEmpty() || dateFrom.isEmpty() || dateTo.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập địa điểm và chọn ngày", Toast.LENGTH_SHORT).show();
            return;
        }
        getSharedPreferences("SEARCH_PREF", MODE_PRIVATE)
                .edit()
                .putString("last_area", area)
                .putString("last_date_from", dateFrom)
                .putString("last_date_to", dateTo)
                .apply();

        Intent intent = new Intent(UserHomeActivity.this, HotelListActivity.class);
        intent.putExtra("area_name", area);
        intent.putExtra("date_from", dateFrom);
        intent.putExtra("date_to", dateTo);
        startActivity(intent);
    });
}


    private void showAreaList() {
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Area area : areas) {
            View view = inflater.inflate(R.layout.item_area, imageContainer, false);

            ImageView imageView = view.findViewById(R.id.imageArea);
            TextView textView = view.findViewById(R.id.textAreaName);

            imageView.setImageResource(area.getImageResId());
            textView.setText(area.getName());

            view.setOnClickListener(v -> {
                Intent intent = new Intent(UserHomeActivity.this, HotelListActivity.class);
                intent.putExtra("area_name", area.getName());
                intent.putExtra("date_from", dateFrom);
                intent.putExtra("date_to", dateTo);
                startActivity(intent);
            });

            imageContainer.addView(view);
        }
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
    private void loadLastSearchInfo() {
        // Đọc từ SharedPreferences
        String lastArea = getSharedPreferences("SEARCH_PREF", MODE_PRIVATE).getString("last_area", "");
        String lastFrom = getSharedPreferences("SEARCH_PREF", MODE_PRIVATE).getString("last_date_from", "");
        String lastTo = getSharedPreferences("SEARCH_PREF", MODE_PRIVATE).getString("last_date_to", "");

        if (!lastArea.isEmpty()) {
            etLocation.setText(lastArea);
        }

        if (!lastFrom.isEmpty()) {
            dateFrom = lastFrom;
            tvDateFrom.setText(dateFrom);
        }

        if (!lastTo.isEmpty()) {
            dateTo = lastTo;
            tvDateTo.setText(dateTo);
        }
        if (!lastArea.isEmpty() && !lastFrom.isEmpty() && !lastTo.isEmpty()) {
            layoutLastSearch.setVisibility(View.VISIBLE);
            tvLastSearchName.setText(lastArea.toUpperCase());
            tvLastSearchDate.setText(lastFrom+" - "+lastTo);


            int imageRes = getImageResourceForLocation(lastArea);
            imageViewLastSearch.setImageResource(imageRes);
            continueSearch.setOnClickListener(v -> {
                Intent intent = new Intent(UserHomeActivity.this, HotelListActivity.class);
                intent.putExtra("area_name", lastArea);
                intent.putExtra("date_from", lastFrom);
                intent.putExtra("date_to", lastTo);
                startActivity(intent);
            });
        } else {
            layoutLastSearch.setVisibility(View.GONE);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        loadLastSearchInfo();
    }
    private int getImageResourceForLocation(String location) {
        if (location == null) return R.drawable.img_hcm;
        location = location.toLowerCase().trim();
        if (location.contains("hà nội")) return R.drawable.img_hanoi;
        if (location.contains("hồ chí minh") || location.contains("hcm") || location.contains("sài gòn")) return R.drawable.img_hcm;
        if (location.contains("đà nẵng")) return R.drawable.img_danang;
        if (location.contains("sa pa")) return R.drawable.img_sapa;
        if (location.contains("huế")) return R.drawable.img_hue;
        if(location.contains("vũng tàu")) return R.drawable.img_nhatrang;
        if(location.contains("hội an")) return R.drawable.img_hoian;
        if(location.contains("ninh bình")) return R.drawable.img_ninhbinh;
        return R.drawable.img_hcm; // fallback
    }

}
