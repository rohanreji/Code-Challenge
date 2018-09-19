package com.themaskedbit.mydeliveries.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.picasso.Picasso;
import com.themaskedbit.mydeliveries.R;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private TextView deliveryAddressMap = null;
    private ImageView deliveryImageMap = null;
    private String address = null;
    private double lat = 0.0;
    private double lng = 0.0;
    private String imageUrl = null;
    private String deliveryDescription = null;
    private MapFragment mapFragment = null;
    private GoogleMap mMap;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_map);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.map_toolbar);
        setSupportActionBar(myToolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        deliveryAddressMap = (TextView)findViewById(R.id.delivery_description_map);
        deliveryImageMap = (ImageView)findViewById(R.id.delivery_image_map);
        address = getIntent().getExtras().getString("address");
        lat = getIntent().getExtras().getDouble("latitude");
        lng = getIntent().getExtras().getDouble("longitude");
        imageUrl = getIntent().getExtras().getString("image");
        deliveryDescription = getIntent().getExtras().getString("description");
        ab.setTitle(address);
        mapFragment = ((MapFragment) getFragmentManager()
                .findFragmentById(R.id.map));
        mapFragment.getMapAsync(this);
        Picasso.with(this)
                .load(imageUrl)
                .placeholder(android.R.drawable.sym_def_app_icon)
                .error(android.R.drawable.sym_def_app_icon)
                .into(deliveryImageMap);
        deliveryAddressMap.setText(deliveryDescription);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        final LatLng latlngPoint = new LatLng(lat , lng);
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(latlngPoint).title(address));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlngPoint,12.0f));
    }
}
