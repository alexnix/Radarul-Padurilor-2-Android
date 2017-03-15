package com.example.rr.radarulpadurii2;

import android.*;
import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cz.msebera.android.httpclient.Header;
import hollowsoft.slidingdrawer.SlidingDrawer;
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static android.system.Os.remove;
import static com.example.rr.radarulpadurii2.R.id.drawer;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback, OnLocationUpdatedListener, Emitter.Listener, Ack, GoogleMap.OnMarkerClickListener, MapScreen {

    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 1;

    private GoogleMap mMap;
    private Marker me;
    private Map<String, Marker> trucks;
    private SlidingDrawer drawer;
    private EditText nr, obs;
    private Location currentLocation;

    private MapModel model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        nr = (EditText) findViewById(R.id.nrDeInmatricuare);
        obs = (EditText) findViewById(R.id.alteObservatii);
        trucks = new HashMap<>();

        drawer = (SlidingDrawer) findViewById(R.id.drawer);

        model = new MapModel(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        int off = 0;
        try {
            off = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        if(off==0){
            showEnableGPSDialog();
        } else {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionsForLocation();
            } else {
                SmartLocation.with(this).location().start(this);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        SmartLocation.with(this).location().stop();
    }

    private void requestPermissionsForLocation() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                MY_PERMISSIONS_REQUEST_FINE_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    SmartLocation.with(this).location().start(this);
                } else {
                    // permission was denied
                    finish();
                }
                return;
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.maps_style));
        mMap.setOnMarkerClickListener(this);
        model.connectTrucksSocekt();
    }

    @Override
    public void createMarkers(ArrayList<Truck> trs) {
        for(Truck t : trs) {

            Marker m = mMap.addMarker(
                    new MarkerOptions()
                            .position(new LatLng(t.lat, t.lng))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.truck_marker_small))
            );
            m.setTag(t);
            trucks.put(t.nr, m);
        }
    }

    @Override
    public void sesizareTrimisa() {
        drawer.close();
        Toast.makeText(this, "Sesizare a fost trimisă și va fi evaluatș în cel mai scurt timp.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void createTruck(Truck t) {
        Marker m = mMap.addMarker(
                new MarkerOptions()
                        .position(new LatLng(t.lat, t.lng))
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.truck_marker_small))
        );
        m.setTag(t);
        trucks.put(t.nr, m);
    }

    @Override
    public void destroyTruck(Truck t) {
        trucks.remove(t.nr).remove();
    }

    @Override
    public void updateTruck(String nr, double lat, double lng) {
        Marker m = trucks.get(nr);
        m.setPosition(new LatLng(lat, lng));
    }

    @Override
    public void onLocationUpdated(Location location) {
        currentLocation = location;
        if (me == null) {
            if(mMap != null) {
                me = mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude())));
                me.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.myloc));
                LatLng coordinate = new LatLng(location.getLatitude(), location.getLongitude());
                CameraUpdate update = CameraUpdateFactory.newLatLngZoom(coordinate, 13);
                mMap.animateCamera(update);
            }
        } else {
            me.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
        }
    }

    @Override
    public void call(Object... args) {

    }

    public void trimiteSesizare(View view) {
        model.trimiteSesizare(nr.getText().toString(), obs.getText().toString(), currentLocation);
    }

    private void showEnableGPSDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Activati GPS")
            .setMessage("Pentru a functiona aplicatia necesita pozitia GPS.")
            .setPositiveButton("Accpet", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Intent onGPS = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(onGPS);
                }
            })
            .setNegativeButton("Refuz", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            })
            .setIcon(R.drawable.ic_shortcut_gps_fixed)
            .show();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        TruckDetailsDialog dialog = new TruckDetailsDialog(this, (Truck) marker.getTag());
        dialog.show();
        return false;
    }
}
