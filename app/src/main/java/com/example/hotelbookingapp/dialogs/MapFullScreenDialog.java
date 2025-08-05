package com.example.hotelbookingapp.dialogs;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.DialogFragment;

import com.example.hotelbookingapp.R;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MapFullScreenDialog extends DialogFragment {

    private double latitude, longitude;
    private String hotelName;
    private MapView mapView;

    public MapFullScreenDialog(double latitude, double longitude, String hotelName) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.hotelName = hotelName;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_map_fullscreen, container, false);
        mapView = view.findViewById(R.id.fullscreenMapView);

        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        GeoPoint point = new GeoPoint(latitude, longitude);
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(point);

        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(hotelName);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(marker);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }
}
