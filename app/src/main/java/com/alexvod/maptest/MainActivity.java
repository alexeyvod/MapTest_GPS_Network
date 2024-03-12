package com.alexvod.maptest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;



import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
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
import java.util.Set;
import org.osmdroid.views.overlay.Overlay;

public class MainActivity extends AppCompatActivity {

    static public final String SEND_LOCATION_GPS = "SEND_LOCATION_GPS";
    static public final String SEND_LOCATION_NETWORK = "SEND_LOCATION_NETWORK";
    static public final String SEND_LOCATION_LAT = "SEND_LOCATION_LAT";
    static public final String SEND_LOCATION_LON = "SEND_LOCATION_LON";
    static public final String SEND_LOCATION_ACC = "SEND_LOCATION_ACC";
    static public final int MENU_DOWNLOAD_MAP = 352345435;

    BroadcastReceiver brGPS;
    BroadcastReceiver brNetwork;
    GPSTrackerGPS TrackerGPS;
    GPSTrackerNetwork TrackerNetwork;
    TextView txt1;
    TextView txtZoom;

    double GPSLat;
    double GPSLon;
    double GPSAcc = 99999d;
    double NetworkLat;
    double NetworkLon;
    double NetworkAcc = 99999d;
    long updateTime = 5000;

    GeoPoint myPosition;
    Marker MyMarkerGPS;
    Marker MyMarkerNetwork;
    IMapController mapController;
    Context ctx;



    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map = null;
    MyLocationNewOverlay mLocationOverlay;
    ArrayList<OverlayItem> items;

    private Overlay accuracyOverlayNetwork;
    private Overlay accuracyOverlayGPS;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = this;
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_main);
        txt1 = findViewById(R.id.text1);
        txtZoom = findViewById(R.id.txtZoom1);


        requestPermissionsIfNecessary(new String[] {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE
        });

        map = (MapView) findViewById(R.id.map);
        //map.setTileSource(TileSourceFactory.MAPNIK);



        //final ITileSource tileSource = new XYTileSource("Mapnik", 1, 25, 256, ".png", basesURLs);
        //map.setTileSource(tileSource);

        Configuration.getInstance().setUserAgentValue("MapTest");

        //addOverlays2();

        String[] basesURLs = new String[] { "http://tile.openstreetmap.org/" };
        map = (MapView) findViewById(R.id.map);
        final ITileSource tileSource = new XYTileSource(
                "Mapnik", 2, 22, 256, ".png", basesURLs);
        map.setTileSource(tileSource);

        //map.setTileSource(TileSourceFactory.MAPNIK);




        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        mapController = map.getController();
        mapController.setZoom(10);
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

        /*
        items = new ArrayList<OverlayItem>();
        Drawable myCurrentLocationMarker = this.getResources().getDrawable(R.drawable.person);
        OverlayItem myLocationOverlayItem = new OverlayItem("Here", "Current Position", myPosition);
        myLocationOverlayItem.setMarker(myCurrentLocationMarker);
        items.add(myLocationOverlayItem);
        //items.add(new OverlayItem("Title", "Description", new GeoPoint(55.859896d,37.594028d))); // Lat/Lon decimal degrees
        ItemizedOverlayWithFocus<OverlayItem> mOverlay = new ItemizedOverlayWithFocus<OverlayItem>(items,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                        //do something
                        return true;
                    }
                    @Override
                    public boolean onItemLongPress(final int index, final OverlayItem item) {
                        return false;
                    }
                }, this);
        mOverlay.setFocusItemsOnTap(true);
        map.getOverlays().add(mOverlay);
        */

        final MapEventsReceiver mReceive = new MapEventsReceiver(){
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {

                Toast.makeText(
                        getBaseContext(),
                        p.getLatitude() + "\n"+ p.getLongitude(),
                        Toast.LENGTH_LONG).show();
                Toast.makeText(
                        getBaseContext(),
                        String.format("Zoom: %2.1f", map.getZoomLevelDouble()),
                        Toast.LENGTH_SHORT).show();
                //Log.d("happy", "MaxZoomLevel=" + map.getMaxZoomLevel());
                return false;
            }
            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };
        map.getOverlays().add(new MapEventsOverlay(mReceive));


        map.setMaxZoomLevel(25.0);

    }



    public void addOverlays2() {
        //not even needed since we are using the offline tile provider only
        map.setUseDataConnection(false); // false = work from directory osmdroid

        //first we'll look at the default location for tiles that we support
        //File f = new File(Configuration.getInstance().getOsmdroidTileCache().toString());
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/osmdroid/");
        //Log.d("happy", "Dir:  " + f);
        if (f.exists()) {

            File[] list = f.listFiles();
            if (list != null) {
                for (int i = 0; i < list.length; i++) {
                    if (list[i].isDirectory()) {
                        continue;
                    }
                    String name = list[i].getName().toLowerCase();
                    if (!name.contains(".")) {
                        continue; //skip files without an extension
                    }
                    name = name.substring(name.lastIndexOf(".") + 1);
                    if (name.length() == 0) {
                        continue;
                    }
                    if (ArchiveFileFactory.isFileExtensionRegistered(name)) {
                        try {

                            //ok found a file we support and have a driver for the format, for this demo, we'll just use the first one

                            //create the offline tile provider, it will only do offline file archives
                            //again using the first file
                            OfflineTileProvider tileProvider = new OfflineTileProvider(new SimpleRegisterReceiver(this), new File[]{list[i]});

                            //tell osmdroid to use that provider instead of the default rig which is (asserts, cache, files/archives, online
                            //map.setTileProvider(tileProvider);

                            //this bit enables us to find out what tiles sources are available. note, that this action may take some time to run
                            //and should be ran asynchronously. we've put it inline for simplicity

                            String source = "";
                            IArchiveFile[] archives = tileProvider.getArchives();
                            if (archives.length > 0) {
                                //cheating a bit here, get the first archive file and ask for the tile sources names it contains
                                Set<String> tileSources = archives[0].getTileSources();
                                //presumably, this would be a great place to tell your users which tiles sources are available
                                if (!tileSources.isEmpty()) {
                                    //ok good, we found at least one tile source, create a basic file based tile source using that name
                                    //and set it. If we don't set it, osmdroid will attempt to use the default source, which is "MAPNIK",
                                    //which probably won't match your offline tile source, unless it's MAPNIK
                                    source = tileSources.iterator().next();
                                    tileProvider.setTileSource(FileBasedTileSource.getSource(source));
                                } else {
                                    tileProvider.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
                                }

                            } else {
                                tileProvider.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
                            }
                            TilesOverlay tilesOverlay = new TilesOverlay(tileProvider, this);
                            tilesOverlay.setLoadingBackgroundColor(Color.TRANSPARENT);
                            map.getOverlays().add(tilesOverlay);
                            map.invalidate();
                            //Log.d("happy", "Using " + list[i].getAbsolutePath() + " " + source);
                            Toast.makeText(this, "Using " + list[i].getAbsolutePath() + " " + source, Toast.LENGTH_LONG).show();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
            //Toast.makeText(this, f.getAbsolutePath() + " did not have any files I can open! Try using MOBAC", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, f.getAbsolutePath() + " dir not found!", Toast.LENGTH_LONG).show();
        }

    }


    public void HowManyTiles(){
        final BoundingBox boxMap = map.getBoundingBox();
        final CacheManager cacheManager = new CacheManager(map);
        int tilesCount = cacheManager.possibleTilesInArea(boxMap, 6, 13);
        Toast.makeText(this, "tilesCount: " + tilesCount, Toast.LENGTH_LONG).show();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Скачивание ");
        builder.setMessage("Скачать " + tilesCount + " тайлов?");
        builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing but close the dialog
                cacheManager.downloadAreaAsync(ctx, boxMap, 6, 13, new CacheManager.CacheManagerCallback() {
                    @Override
                    public void onTaskComplete() {
                        Toast.makeText(ctx, "tilesCount: " + "Скачано", Toast.LENGTH_LONG).show();
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



    public void addOverlays() {
        //not even needed since we are using the offline tile provider only
        map.setUseDataConnection(true);

        //first we'll look at the default location for tiles that we support
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/osmdroid/");
        if (f.exists()) {

            File[] list = f.listFiles();
            if (list != null) {
                for (int i = 0; i < list.length; i++) {
                    if (list[i].isDirectory()) {
                        continue;
                    }
                    String name = list[i].getName().toLowerCase();
                    if (!name.contains(".")) {
                        continue; //skip files without an extension
                    }
                    name = name.substring(name.lastIndexOf(".") + 1);
                    if (name.length() == 0) {
                        continue;
                    }
                    if (ArchiveFileFactory.isFileExtensionRegistered(name)) {
                        try {

                            //ok found a file we support and have a driver for the format, for this demo, we'll just use the first one

                            //create the offline tile provider, it will only do offline file archives
                            //again using the first file
                            OfflineTileProvider tileProvider = new OfflineTileProvider(new SimpleRegisterReceiver(this), new File[]{list[i]});

                            //tell osmdroid to use that provider instead of the default rig which is (asserts, cache, files/archives, online
                            map.setTileProvider(tileProvider);

                            //this bit enables us to find out what tiles sources are available. note, that this action may take some time to run
                            //and should be ran asynchronously. we've put it inline for simplicity

                            String source = "";
                            IArchiveFile[] archives = tileProvider.getArchives();
                            if (archives.length > 0) {
                                //cheating a bit here, get the first archive file and ask for the tile sources names it contains
                                Set<String> tileSources = archives[0].getTileSources();
                                //presumably, this would be a great place to tell your users which tiles sources are available
                                if (!tileSources.isEmpty()) {
                                    //ok good, we found at least one tile source, create a basic file based tile source using that name
                                    //and set it. If we don't set it, osmdroid will attempt to use the default source, which is "MAPNIK",
                                    //which probably won't match your offline tile source, unless it's MAPNIK
                                    source = tileSources.iterator().next();
                                    map.setTileSource(FileBasedTileSource.getSource(source));
                                } else {
                                    map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
                                }

                            } else {
                                map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
                            }


                            Toast.makeText(this, "Using " + list[i].getAbsolutePath() + " " + source, Toast.LENGTH_LONG).show();
                            map.invalidate();
                            return;
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
            Toast.makeText(this, f.getAbsolutePath() + " did not have any files I can open! Try using MOBAC", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, f.getAbsolutePath() + " dir not found!", Toast.LENGTH_LONG).show();
        }

    }


    @Override
    protected void onResume() {
        super.onResume();
        GPSAcc = 99999d;
        NetworkAcc = 99999d;
        NetworkLat = 0;
        NetworkLon = 0;
        GPSLat = 0;
        GPSLon = 0;
        RegisterReceiviers();
        map.onResume();
        TrackerGPS = new GPSTrackerGPS(this);
        TrackerGPS.setTimeUpadates(updateTime);
        TrackerGPS.getLocation();

        TrackerNetwork = new GPSTrackerNetwork(this);
        TrackerNetwork.setTimeUpadates(updateTime);
        TrackerNetwork.getLocation();

        txt1.setText("Wait");
        updateCoord();


        //startService(new Intent(this, GPSTrackerGPS.class));
    }


    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
        unregisterReceiver(brNetwork);
        unregisterReceiver(brGPS);
        TrackerGPS.stopUsingGPS();
        TrackerNetwork.stopUsingGPS();
        /*
        if(updateTime < 5000){
            TrackerGPS.stopUsingGPS();
            TrackerNetwork.stopUsingGPS();
            //stopService(new Intent(this, GPSTrackerGPS.class));
        }
         */

    }

    void updateCoord(){
        MyMarkerGPS.setPosition(new GeoPoint(GPSLat, GPSLon));
        MyMarkerNetwork.setPosition(new GeoPoint(NetworkLat, NetworkLon));
        String GG = ((int)GPSAcc) == 99999 ? " waiting " :  ((int)GPSAcc) + " ";
        String NN = ((int)NetworkAcc) == 99999 ? " waiting " : ((int)NetworkAcc) + " ";
        txt1.setText("GPS acc = "+GG + "  Network acc = " + NN );
        MyMarkerGPS.setTitle("GPS acc="+(int)GPSAcc + "\n" + GPSLat + "\n" + GPSLon);
        MyMarkerNetwork.setTitle("Network acc="+(int)NetworkAcc + "\n" + NetworkLat + "\n" + NetworkLon);

        // Network Accuracy Circle
        if (accuracyOverlayNetwork != null) {
            map.getOverlays().remove(accuracyOverlayNetwork);
            map.invalidate();
        }
        if(NetworkAcc <= 10000){
            accuracyOverlayNetwork = new AccuracyOverlay(new GeoPoint(NetworkLat, NetworkLon), (float)NetworkAcc, Color.parseColor("#006633"));
            map.getOverlays().add(accuracyOverlayNetwork);
        }

        // GPS Accuracy Circle
        if (accuracyOverlayGPS != null) {
            map.getOverlays().remove(accuracyOverlayGPS);
            map.invalidate();
        }
        if(GPSAcc <= 10000){
            accuracyOverlayGPS = new AccuracyOverlay(new GeoPoint(GPSLat, GPSLon), (float)GPSAcc, Color.parseColor("#0033FF"));
            map.getOverlays().add(accuracyOverlayGPS);
        }


        map.invalidate();
    }

    public void CenterGPS(View view) {
        mapController.setCenter(new GeoPoint(GPSLat, GPSLon));
        map.invalidate();
    }

    public void CenterNetwork(View view) {
        mapController.setCenter(new GeoPoint(NetworkLat, NetworkLon));
        map.invalidate();
    }

    void RestartService(){
        TrackerGPS.stopUsingGPS();
        TrackerNetwork.stopUsingGPS();
        TrackerGPS.setTimeUpadates(updateTime);
        TrackerGPS.getLocation();
        TrackerNetwork.setTimeUpadates(updateTime);
        TrackerNetwork.getLocation();
    }

    public void m3(View view) {
        updateTime = 1000 * 60 * 3; // 5 minutes
        RestartService();
    }

    public void s5(View view) {
        updateTime = 5000; // 5 sec
        RestartService();
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


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d("happy", "onRequestPermissionsResult");
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            permissionsToRequest.add(permissions[i]);
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    public void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0) {
            Intent tmpIntent = new Intent(this, Permiss.class);
            tmpIntent.setPackage(getApplicationContext().getApplicationInfo().packageName);
            // tmpIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(tmpIntent);
            /*
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
            */
        }
    }


    public void z(View view) {
        //Intent i12 = new Intent(this, DownloadActivity.class);
        //startActivity(i12);
        // //HowManyTiles();
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_DOWNLOAD_MAP, Menu.NONE, "Загрузка карты").setVisible(true).setIcon(android.R.drawable.ic_menu_preferences);
        //menu.add(0, MENU_SETTINGS, Menu.NONE, "Настройки").setVisible(true).setIcon(android.R.drawable.ic_menu_preferences);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId())
        {
            case MENU_DOWNLOAD_MAP:
                Intent intent = new Intent(this, DownloadActivity.class);
                intent.setPackage(getApplicationContext().getApplicationInfo().packageName);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivityForResult(intent, 1);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }









}
