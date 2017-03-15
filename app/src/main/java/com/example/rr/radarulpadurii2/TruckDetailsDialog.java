package com.example.rr.radarulpadurii2;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by rr on 15-Mar-17.
 */

public class TruckDetailsDialog extends Dialog {

    private Truck truck;
    public TruckDetailsDialog(Context context, Truck truck) {
        super(context);
        this.truck = truck;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_truck_details);
        getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        TextView nr, continut, createdAt;

        nr = (TextView) findViewById(R.id.nr);
        continut = (TextView) findViewById(R.id.continut);
        createdAt = (TextView) findViewById(R.id.createdAt);

        nr.setText(truck.nr);
        continut.setText("Continutul transportului: " + truck.continut);
        createdAt.setText("Adaugat la data de: " + truck.createdAt);
    }

}
