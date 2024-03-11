package com.alexvod.maptest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.slider.RangeSlider;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.cachemanager.CacheManager;
import org.osmdroid.tileprovider.modules.ArchiveFileFactory;
import org.osmdroid.tileprovider.modules.IArchiveFile;
import org.osmdroid.tileprovider.modules.OfflineTileProvider;
import org.osmdroid.tileprovider.tilesource.FileBasedTileSource;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DownloadActivity extends AppCompatActivity {

    static public final String SEND_LOCATION_GPS = "SEND_LOCATION_GPS";
    static public final String SEND_LOCATION_NETWORK = "SEND_LOCATION_NETWORK";
    static public final String SEND_LOCATION_LAT = "SEND_LOCATION_LAT";
    static public final String SEND_LOCATION_LON = "SEND_LOCATION_LON";
    static public final String SEND_LOCATION_ACC = "SEND_LOCATION_ACC";

    BroadcastReceiver brGPS;
    BroadcastReceiver brNetwork;
    GPSTrackerGPS TrackerGPS;
    GPSTrackerNetwork TrackerNetwork;

    double GPSLat;
    double GPSLon;
    double GPSAcc = 99999d;
    double NetworkLat;
    double NetworkLon;
    double NetworkAcc = 99999d;
    GeoPoint myPosition;
    Marker MyMarkerGPS;
    Marker MyMarkerNetwork;
    IMapController mapController;
    Context ctx;
    private MapView map = null;
    MyLocationNewOverlay mLocationOverlay;


    RangeSlider sliderZoom;
    List<Float> sliderValues;
    TextView sliderCaption;
    TextView mainCaption;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        ctx = this;
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        sliderCaption = findViewById(R.id.sliderCaption);
        mainCaption = findViewById(R.id.txtDownloadCaption);


        String[] basesURLs = new String[] { "http://tile.openstreetmap.org/" };
        map = (MapView) findViewById(R.id.map);
        final ITileSource tileSource = new XYTileSource(
                "Mapnik", 2, 20, 256, ".png", basesURLs);
        map.setTileSource(tileSource);

        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        mapController = map.getController();
        mapController.setZoom(12);
        GeoPoint startPoint = new GeoPoint(55.859896, 37.594028);
        mapController.setCenter(startPoint);

        myPosition = new GeoPoint(55.859896, 37.594028);

        MyMarkerGPS = new Marker(map);
        MyMarkerGPS.setPosition(startPoint);
        MyMarkerGPS.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        MyMarkerGPS.setIcon(this.getResources().getDrawable(android.R.drawable.ic_menu_compass));
        map.getOverlays().add(MyMarkerGPS);

        MyMarkerNetwork = new Marker(map);
        MyMarkerNetwork.setPosition(startPoint);
        MyMarkerNetwork.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        MyMarkerNetwork.setIcon(this.getResources().getDrawable(android.R.drawable.ic_menu_call));
        map.getOverlays().add(MyMarkerNetwork);
        map.addMapListener(new MapListener(){
            @Override
            public boolean onScroll(ScrollEvent event) {
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                mainCaption.setText(String.format("Zoom: %2.1f", map.getZoomLevelDouble()));
                return false;
            }
        });




        sliderZoom = findViewById(R.id.sliderZoom);

        sliderValues = new ArrayList<Float>();
        sliderValues.add(3.0f);
        sliderValues.add(12.0f);

        sliderZoom.setValues(sliderValues);
        sliderCaption.setText("Масштаб для скачивания: " + sliderValues.get(0) + " - " + sliderValues.get(1));
        sliderZoom.addOnSliderTouchListener(new RangeSlider.OnSliderTouchListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onStartTrackingTouch(@NonNull RangeSlider slider) {

            }

            @SuppressLint("RestrictedApi")
            @Override
            public void onStopTrackingTouch(@NonNull RangeSlider slider) {
                sliderValues = slider.getValues();
                sliderCaption.setText("Масштаб для скачивания: " + sliderValues.get(0) + " - " + sliderValues.get(1));
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        RegisterReceiviers();
        map.onResume();
        TrackerGPS = new GPSTrackerGPS(this);
        TrackerGPS.setTimeUpadates(5000);
        TrackerGPS.getLocation();
        TrackerNetwork = new GPSTrackerNetwork(this);
        TrackerNetwork.setTimeUpadates(5000);
        TrackerNetwork.getLocation();

    }


    public void download(View view){
        final BoundingBox boxMap = map.getBoundingBox();
        final CacheManager cacheManager = new CacheManager(map);
        int min = (int) Math.floor(sliderValues.get(0));
        int max = (int) Math.ceil(sliderValues.get(1));
        int tilesCount = cacheManager.possibleTilesInArea(boxMap, min, max);
        //Toast.makeText(this, "tilesCount: " + tilesCount, Toast.LENGTH_LONG).show();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Скачивание");
        builder.setMessage("Скачать " + tilesCount + " тайлов?");
        builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing but close the dialog
                cacheManager.downloadAreaAsync(ctx, boxMap, 6, 13, new CacheManager.CacheManagerCallback() {

                    @Override
                    public void onTaskComplete() {
                        Toast.makeText(ctx, "Скачано ", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void updateProgress(int progress, int currentZoomLevel, int zoomMin, int zoomMax) {
                    }

                    @Override
                    public void downloadStarted() {

                    }

                    @Override
                    public void setPossibleTilesInArea(int total) {
                    }

                    @Override
                    public void onTaskFailed(int errors) {

                    }
                });
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }



    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
        TrackerGPS.stopUsingGPS();
        TrackerNetwork.stopUsingGPS();
    }

    void updateCoord(){
        MyMarkerGPS.setPosition(new GeoPoint(GPSLat, GPSLon));
        MyMarkerNetwork.setPosition(new GeoPoint(NetworkLat, NetworkLon));
        map.invalidate();
    }

    public void downloadCenterGPS(View view) {
        mapController.setCenter(new GeoPoint(GPSLat, GPSLon));
        map.invalidate();
    }

    public void downloadCenterNetwork(View view) {
        mapController.setCenter(new GeoPoint(NetworkLat, NetworkLon));
        map.invalidate();
    }


    void RegisterReceiviers(){
        brGPS = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                GPSLat = intent.getDoubleExtra(SEND_LOCATION_LAT, 0);
                GPSLon = intent.getDoubleExtra(SEND_LOCATION_LON, 0);
                GPSAcc = intent.getFloatExtra(SEND_LOCATION_ACC, 99999);
                Log.d("alexvod", "brGPS " + GPSLat + ", " + GPSLon);
                updateCoord();
            }
        };
        IntentFilter intFiltGPS = new IntentFilter(SEND_LOCATION_GPS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(brGPS, intFiltGPS, Context.RECEIVER_NOT_EXPORTED);
        }

        brNetwork = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                NetworkLat = intent.getDoubleExtra(SEND_LOCATION_LAT, 0);
                NetworkLon = intent.getDoubleExtra(SEND_LOCATION_LON, 0);
                NetworkAcc = intent.getFloatExtra(SEND_LOCATION_ACC, 99999);
                Log.d("alexvod", "brNETWORK " + NetworkLat + ", " + NetworkLon);
                updateCoord();
            }
        };
        IntentFilter intFiltNetwork = new IntentFilter(SEND_LOCATION_NETWORK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(brNetwork, intFiltNetwork, Context.RECEIVER_NOT_EXPORTED);
        }
    }


    public void Cancel(View view) {
        Intent intent = new Intent();
        intent.setPackage(getApplicationContext().getApplicationInfo().packageName);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("RESULT", "Cancel");
        setResult(RESULT_CANCELED, intent);
        finish();
    }


    @Override
    public void onBackPressed(){
        Intent intent = new Intent();
        intent.setPackage(getApplicationContext().getApplicationInfo().packageName);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("RESULT", "Cancel");
        setResult(RESULT_CANCELED, intent);
        finish();
    }



}
