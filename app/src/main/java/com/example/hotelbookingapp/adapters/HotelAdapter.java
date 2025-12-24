package com.example.hotelbookingapp.adapters;

import android.content.Context;
import android.content.Intent;
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
import com.example.hotelbookingapp.activities.HotelDetailActivity;
import com.example.hotelbookingapp.models.Hotel;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class HotelAdapter extends RecyclerView.Adapter<HotelAdapter.HotelViewHolder> {
    private String dateFrom;
    private String dateTo;

    private final List<Hotel> hotelList;
    private final Context context;
    private int numberOfNights = 1; // mặc định 1 đêm

    public HotelAdapter(Context context, List<Hotel> hotelList, int numberOfNights,String dateFrom, String dateTo) {
        this.context = context;
        this.hotelList = hotelList;
        this.numberOfNights = numberOfNights;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
    }

    @NonNull
    @Override
    public HotelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_hotel, parent, false);
        return new HotelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HotelViewHolder holder, int position) {
        Hotel hotel = hotelList.get(position);
        holder.tvName.setText(hotel.name);
        holder.tvAddress.setText(hotel.address);

        holder.tvRating.setText(String.format(Locale.getDefault(), "%.1f", hotel.rating));
        holder.tvStars.setText(hotel.stars + "★");

        long total = hotel.priceNew * numberOfNights;
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        holder.tvPrice.setText(format.format(total) + " / " + numberOfNights + " đêm");
        String priceFormatted = format.format(hotel.priceOld*numberOfNights);
        holder.tvOldPrice.setText(priceFormatted);

        if(hotel.availableRooms<=0){
            holder.tvNumber.setText("Hết phòng");
        }else{
            holder.tvNumber.setText("Chỉ còn "+hotel.availableRooms+" phòng");
        }
        holder.tvOldPrice.setPaintFlags(holder.tvOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        if (hotel.imageUrls != null && !hotel.imageUrls.isEmpty()) {
            Glide.with(context)
                    .load(hotel.imageUrls.get(0))
                    .placeholder(R.drawable.ic_placeholder)
                    .into(holder.imgHotel);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, HotelDetailActivity.class);
            intent.putExtra("selected_hotel", hotel);
            intent.putExtra("date_from", dateFrom);
            intent.putExtra("date_to", dateTo);
            context.startActivity(intent);
        });


    }

    @Override
    public int getItemCount() {
        return hotelList.size();
    }

    public void setNumberOfNights(int nights) {
        this.numberOfNights = nights;
        notifyDataSetChanged();
    }

    public static class HotelViewHolder extends RecyclerView.ViewHolder {
        ImageView imgHotel;
        TextView tvName, tvAddress, tvRating, tvStars, tvPrice,tvOldPrice,tvNumber;

        public HotelViewHolder(@NonNull View itemView) {
            super(itemView);
            imgHotel = itemView.findViewById(R.id.imgHotel);
            tvName = itemView.findViewById(R.id.tvHotelName);
            tvAddress = itemView.findViewById(R.id.tvHotelAddress);
            tvRating = itemView.findViewById(R.id.tvHotelRating);
            tvStars = itemView.findViewById(R.id.tvHotelStars);
            tvPrice = itemView.findViewById(R.id.tvHotelPrice);
            tvOldPrice=itemView.findViewById(R.id.tv_OldPrice);
            tvNumber=itemView.findViewById(R.id.tv_numberRoom);
        }
    }
}
