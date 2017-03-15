package com.example.rr.radarulpadurii2;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.ResponseHandlerInterface;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;
import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static com.example.rr.radarulpadurii2.R.id.drawer;

/**
 * Created by rr on 14-Mar-17.
 */

public class MapModel implements Emitter.Listener {

    private MapScreen ctrl;
    private static final String SERVER_URL = "http://192.168.1.109:3000/";
    private static final String SERVER_URL_IO = SERVER_URL + "?__sails_io_sdk_version=0.11.0";

    public MapModel(MapScreen ctrl) {
        this.ctrl = ctrl;
    }

    public void connectTrucksSocekt() {
        try {
            Socket socket = IO.socket(SERVER_URL_IO);
            socket.connect();
            Log.wtf("wtf", "socket connected");
            JSONObject subscribe = new JSONObject();
            subscribe.put("url", "/api/truck");
            socket.emit("get", subscribe, new TruckSubbscribeAck(ctrl));
            socket.on("truck", this);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void trimiteSesizare(String nr, String obs, Location location) {
        AsyncHttpClient client = new AsyncHttpClient();

        RequestParams params = new RequestParams();

        params.put("lat", (double) location.getLatitude());
        params.put("lng", (double) location.getLongitude());
        params.put("numar_de_inmatriculare", nr);
        params.put("optional_message", obs);

        client.post(SERVER_URL + "/api/report", params, new TrimiteSesizareCb(ctrl));
    }

    @Override
    public void call(Object... args) {
        final JSONObject event = (JSONObject)args[0];
        Log.wtf("event", event.toString());
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable(){

            @Override
            public void run() {
                try {
                    String verb = event.getString("verb");

                    if(verb.equals("created")) {
                        Truck t = new Truck(event.getJSONObject("data"));
                        ctrl.createTruck(t);
                    }

                    if(verb.equals("updated")) {
                        ctrl.updateTruck(event.getJSONObject("previous").getString("numar_de_inmatriculare"),event.getJSONObject("data").getDouble("lat"),event.getJSONObject("data").getDouble("lng"));
                    }

                    if(verb.equals("destroyed")) {
                        Truck t = new Truck(event.getJSONObject("previous"));
                        ctrl.destroyTruck(t);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private class TruckSubbscribeAck implements Ack {
        MapScreen ctrl;
        public TruckSubbscribeAck(MapScreen ctrl) {
            this.ctrl = ctrl;
        }

        @Override
        public void call(Object... args) {
            JSONObject res = (JSONObject)args[0];
            try {
                final ArrayList<Truck> trucks = Truck.fromJSON(res.getJSONArray("body"));
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable(){
                    @Override
                    public void run() {
                        ctrl.createMarkers(trucks);
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class TrimiteSesizareCb extends JsonHttpResponseHandler {
        MapScreen ctrl;
        public TrimiteSesizareCb(MapScreen ctrl) {
            this.ctrl = ctrl;
        }

        @Override
        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
            ctrl.sesizareTrimisa();
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
            Log.wtf("onFailure", errorResponse.toString());
        }
    }
}
