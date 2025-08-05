package com.example.hotelbookingapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;



import com.bumptech.glide.Glide;
import com.example.hotelbookingapp.R;
import java.util.List;
import android.widget.BaseAdapter;


public class ImageAdapter extends BaseAdapter {

    private final Context context;
    private final List<String> imageUrls;
    private final OnImageClickListener listener;

    public interface OnImageClickListener {
        void onImageClick(int position);
    }

    public ImageAdapter(Context context, List<String> imageUrls, OnImageClickListener listener) {
        this.context = context;
        this.imageUrls = imageUrls;
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return imageUrls.size();
    }

    @Override
    public Object getItem(int position) {
        return imageUrls.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imgPhoto;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
            imgPhoto = convertView.findViewById(R.id.imgPhoto);
            convertView.setTag(imgPhoto);
        } else {
            imgPhoto = (ImageView) convertView.getTag();
        }

        Glide.with(context).load(imageUrls.get(position)).into(imgPhoto);

        imgPhoto.setOnClickListener(v -> {
            if (listener != null) {
                listener.onImageClick(position);
            }
        });

        return convertView;
    }
}


