package com.example.hotelbookingapp.dialogs;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.example.hotelbookingapp.R;

import java.util.List;

public class ImagePreviewDialog extends DialogFragment {

    private final List<String> imageUrls;
    private int currentIndex;

    private ImageView imgPreview;
    private ImageView btnPrev, btnNext, btnClose;

    public ImagePreviewDialog(List<String> imageUrls, int startPosition) {
        this.imageUrls = imageUrls;
        this.currentIndex = startPosition;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_image_preview, container, false);

        imgPreview = view.findViewById(R.id.imgPreview);
        btnPrev = view.findViewById(R.id.btnPrev);
        btnNext = view.findViewById(R.id.btnNext);
        btnClose = view.findViewById(R.id.btnClose);

        loadImage();

        btnPrev.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                loadImage();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentIndex < imageUrls.size() - 1) {
                currentIndex++;
                loadImage();
            }
        });

        btnClose.setOnClickListener(v -> dismiss());

        return view;
    }

    private void loadImage() {
        Glide.with(requireContext()).load(imageUrls.get(currentIndex)).into(imgPreview);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}
