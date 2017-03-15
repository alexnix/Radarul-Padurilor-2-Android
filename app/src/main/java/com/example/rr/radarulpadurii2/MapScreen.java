package com.example.rr.radarulpadurii2;

import java.util.ArrayList;

/**
 * Created by rr on 14-Mar-17.
 */

public interface MapScreen {

    public void createMarkers(ArrayList<Truck> trs);
    public void sesizareTrimisa();
    public void createTruck(Truck t);
    public void destroyTruck(Truck t);
    public void updateTruck(String nr, double lat, double lng);
}
