package com.example.hotelbookingapp.adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hotelbookingapp.R;
import com.example.hotelbookingapp.models.Hotel;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class SavedHotelsAdapter extends RecyclerView.Adapter<SavedHotelsAdapter.ViewHolder> {
    private final List<Hotel> hotels;
    private final OnHotelClickListener listener;

    public interface OnHotelClickListener {
        void onHotelClick(Hotel hotel);
    }

    public SavedHotelsAdapter(List<Hotel> hotels, OnHotelClickListener listener) {
        this.hotels = hotels;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_hotel, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Hotel hotel = hotels.get(position);
        holder.tvName.setText(hotel.name);
        holder.tvAddress.setText(hotel.address);
        holder.tvOldPrice.setText(NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(hotel.priceOld)+" /đêm");
        holder.tvOldPrice.setPaintFlags(holder.tvOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        holder.tvPrice.setText(NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(hotel.priceNew)+" /đêm");
        Glide.with(holder.itemView.getContext()).load(hotel.imageUrls.get(0)).into(holder.imgHotel);
        holder.itemView.setOnClickListener(v -> listener.onHotelClick(hotel));
    }

    @Override
    public int getItemCount() {
        return hotels.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress, tvPrice,tvOldPrice;
        ImageView imgHotel;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvHotelName);
            tvAddress = itemView.findViewById(R.id.tvHotelAddress);
            tvPrice = itemView.findViewById(R.id.tvHotelPrice);
            imgHotel = itemView.findViewById(R.id.imgHotel);
            tvOldPrice=itemView.findViewById(R.id.tv_Old_Price);
        }
    }
}

