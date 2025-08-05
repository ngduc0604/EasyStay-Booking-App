package com.example.hotelbookingapp.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hotelbookingapp.R;
import com.example.hotelbookingapp.models.Booking;
import com.google.firebase.firestore.FirebaseFirestore;

import org.checkerframework.checker.units.qual.C;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class BookingHistoryAdapter extends RecyclerView.Adapter<BookingHistoryAdapter.ViewHolder> {
    private final List<Booking> bookings;
    private final OnBookingClickListener listener;
    private final Context context;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface OnBookingClickListener {
        void onBookingClick(Booking booking);
    }

    public BookingHistoryAdapter(Context context, List<Booking> bookings, OnBookingClickListener listener) {
        this.context = context;
        this.bookings = bookings;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Booking booking = bookings.get(position);
        holder.tvHotelName.setText(booking.getHotelName());
        holder.tvBookingDate.setText("Nhận: " + booking.getCheckInDate() + " - Trả: " + booking.getCheckOutDate());
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        holder.tvTotalPrice.setText(format.format(booking.getTotalPrice()));
        if(booking.getStatus().equals("confirm")){
            holder.tvStatus.setText("Trạng thái: Đã xác nhận");
            holder.tvStatus.setTextColor(Color.BLUE);
        }else if(booking.getStatus().equals("complete")){
            holder.tvStatus.setText("Trạng thái: Đã hoàn thành");
            holder.tvStatus.setTextColor(Color.GRAY);
        }else{
            holder.tvStatus.setText("Trạng thái: Đã hủy");
            holder.tvStatus.setTextColor(Color.GRAY);
        }

        db.collection("hotels").document(booking.getHotelId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String address = documentSnapshot.getString("address");
                        holder.tvAddress.setText(address);
                    } else {
                        holder.tvAddress.setText("Không có địa chỉ");
                    }
                })
                .addOnFailureListener(e -> holder.tvAddress.setText("Lỗi tải địa chỉ"));

        holder.itemView.setOnClickListener(v -> listener.onBookingClick(booking));

    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvHotelName, tvBookingDate, tvTotalPrice, tvAddress,tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHotelName = itemView.findViewById(R.id.tvHotelName);
            tvBookingDate = itemView.findViewById(R.id.tvBookingDate);
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice);
            tvAddress = itemView.findViewById(R.id.tvHotelAddress);
            tvStatus=itemView.findViewById(R.id.tvBookingStatus);
        }
    }
}
