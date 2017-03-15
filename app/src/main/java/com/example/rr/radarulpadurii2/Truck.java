package com.example.rr.radarulpadurii2;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by rr on 15-Mar-17.
 */

public class Truck {
    public String nr, continut, createdAt;
    public double lat, lng;

    public Truck(JSONObject object){
        try {
            this.nr = object.getString("numar_de_inmatriculare");
            this.continut = object.getString("continutul_transportului");
            this.createdAt = object.getString("createdAt");
            this.lat = object.getDouble("lat");
            this.lng = object.getDouble("lng");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<Truck> fromJSON(JSONArray array){
        ArrayList<Truck> traks = new ArrayList<>();

        for(int i = 0; i < array.length(); i++)
            try {
                traks.add(new Truck(array.getJSONObject(i)));
            } catch (JSONException e) {
                e.printStackTrace();
            }

        return traks;
    }
}
