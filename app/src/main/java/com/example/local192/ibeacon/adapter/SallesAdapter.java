package com.example.local192.ibeacon.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ActionMenuView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.example.local192.ibeacon.R;
import com.example.local192.ibeacon.model.Salle;

import java.util.List;

/**
 * Created by local192 on 12/12/2017.
 */

public class SallesAdapter extends ArrayAdapter {
    Activity activity;
    List<Salle> salles;
    public SallesAdapter(Activity activity, List<Salle> salles) {
        super(activity, R.layout.salles_adapter, salles);
        this.activity = activity;
        this.salles = salles;
    }

    TextView textSalle;
    ImageView imageVisited;
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.salles_adapter, parent, false);
        textSalle = (TextView) view.findViewById(R.id.textSalle);
        imageVisited = (ImageView) view.findViewById(R.id.imageNfc);
        textSalle.setText(salles.get(position).getName());

        return view;
    }
}
