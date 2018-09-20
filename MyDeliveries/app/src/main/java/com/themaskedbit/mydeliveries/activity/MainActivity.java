package com.themaskedbit.mydeliveries.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.themaskedbit.mydeliveries.R;
import com.themaskedbit.mydeliveries.adpater.DeliveryAdapter;
import com.themaskedbit.mydeliveries.model.Delivery;
import com.themaskedbit.mydeliveries.rest.DeliveryApiService;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    //base url of API
    public static final String BASE_URL = "http://986d59c0.ngrok.io";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static Retrofit retrofit = null;
    private RecyclerView recyclerView = null;
    private TextView deliveryMessage = null;
    private static int mutex = 0;
    private SwipeRefreshLayout swipeContainer;
    private LinearLayoutManager layoutManager;
    private ProgressBar progressBar;
    private RelativeLayout emptyVIew = null;
    private Context context = null;
    private DeliveryAdapter deliveryAdapter = null;
    private List<Delivery> totalDeliveries = null;
    private boolean isScrolling = false;
    private int currentItem = 0;
    private int totalItem = 0;
    private int scrollOutItem = 0;
    private int limit =20;
    private int refresh = 0;

    private final String dataKey = "objKey";
    private final String dateKey = "dateKey";
    private final String preferenceFile = "myPreference";
    private int cached = 0;

    SharedPreferences shref;
    SharedPreferences.Editor editor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayShowTitleEnabled(false);
        totalDeliveries = new ArrayList<Delivery>();
        shref = this.getSharedPreferences(preferenceFile, Context.MODE_PRIVATE);

        recyclerView = (RecyclerView)findViewById(R.id.recycler_view);
        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        deliveryMessage = (TextView) findViewById(R.id.delivery_message);

        emptyVIew = (RelativeLayout)findViewById(R.id.empty_view);
        progressBar = (ProgressBar)findViewById(R.id.progress);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        DeliveryAdapter.DeliveryViewClickListener mListener = new DeliveryAdapter.DeliveryViewClickListener() {
            @Override
            public void onClick(View view, int position) {
                Intent mapIntent = new Intent(context,MapActivity.class);
                mapIntent.putExtra("address",totalDeliveries.get(position).getLocation().getAddress());
                mapIntent.putExtra("latitude",totalDeliveries.get(position).getLocation().getLatitude());
                mapIntent.putExtra("longitude", totalDeliveries.get(position).getLocation().getLongitude());
                mapIntent.putExtra("image",totalDeliveries.get(position).getImageUrl());
                mapIntent.putExtra("description",totalDeliveries.get(position).getDescription());
                startActivity(mapIntent);
            }
        };

        deliveryAdapter = new DeliveryAdapter(recyclerView,totalDeliveries, getApplicationContext(),mListener);
        recyclerView.setAdapter(deliveryAdapter);
        getDeliveries(0, limit);

        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(mutex == 1 ){
                    //Handle - REVISIT - toast
                    return;
                }
                refresh = 1;
                cached = 0;
                totalDeliveries.clear();
                deliveryAdapter.notifyDataSetChanged();
                getDeliveries(0, limit);
            }

        });

        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if(newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL){
                    isScrolling = true;
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                currentItem = layoutManager.getChildCount();
                totalItem = layoutManager.getItemCount();
                scrollOutItem = layoutManager.findFirstVisibleItemPosition();
                if(isScrolling && (currentItem+scrollOutItem)==totalItem){
                    isScrolling = false;
                    if(mutex == 1 || cached == 1) {
                        return;
                    }
                    getDeliveries(totalItem, limit);
                }
            }
        });

    }

    public void getDeliveries(int offset, int limit) {
        mutex=1;
        if(refresh==0)
            progressBar.setVisibility(View.VISIBLE);
        if(retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        DeliveryApiService deliveryApiService = retrofit.create(DeliveryApiService.class);
        Map<String,Integer> parameters = new HashMap<>();
        parameters.put("offset",offset);
        parameters.put("limit",limit);
        context = this;
        Call<List<Delivery>> call = deliveryApiService.getAllDeliveries(parameters);
        call.enqueue(new Callback<List<Delivery>>() {
            @Override
            public void onResponse(Call<List<Delivery>> call, Response<List<Delivery>> response) {
                if(response.body() == null){
                    Log.e(TAG, "response is null");
                    progressBar.setVisibility(View.GONE);
                    Gson gson = new Gson();
                    int itemCount = layoutManager.getItemCount();
                    if(cached == 0 && itemCount == 0) {
                        String cachedData = shref.getString(dataKey, "");
                        List<Delivery> tempDelivery = gson.fromJson(cachedData,
                                new TypeToken<List<Delivery>>() {
                                }.getType());
                        if(tempDelivery != null) {
                            for (int i = 0; i < tempDelivery.size(); i++) {
                                totalDeliveries.add(tempDelivery.get(i));
                                deliveryAdapter.notifyItemInserted(totalDeliveries.size() - 1);
                            }
                            deliveryAdapter.notifyDataSetChanged();
                            cached = 1;
                        }
                    }
                    swipeContainer.setRefreshing(false);
                    refresh = 0;
                    if(itemCount > 0 && haveNetworkConnection() == true) {
                        Snackbar snackbar = Snackbar
                                .make(swipeContainer, R.string.error_online, Snackbar.LENGTH_LONG);
                        snackbar.show();
                        mutex=0;
                        return;
                    }
                    else if(itemCount > 0 && haveNetworkConnection() == false) {
                        Snackbar snackbar = Snackbar
                                .make(swipeContainer, R.string.error_offline, Snackbar.LENGTH_LONG);
                        snackbar.show();
                        mutex=0;
                        return;
                    }
                    if(layoutManager.getItemCount()==0) {
                        mutex=0;
                        emptyVIew.setVisibility(View.VISIBLE);
                        deliveryMessage.setText(R.string.error);
                        if(haveNetworkConnection() == false){
                            Snackbar snackbar = Snackbar
                                    .make(swipeContainer, R.string.error_offline, Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                        else {
                            Snackbar snackbar = Snackbar
                                    .make(swipeContainer, R.string.error_online, Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                        return;
                    }
                    mutex=0;
                    String cachedDate=shref.getString(dateKey , "");
                    if(!cachedDate.equals("") && layoutManager.getItemCount()!=0) {
                        if(haveNetworkConnection() == false){
                            Snackbar snackbar = Snackbar
                                    .make(swipeContainer, "Network issues. Last synced at " + cachedDate, Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                        else {
                            Snackbar snackbar = Snackbar
                                    .make(swipeContainer, "Last synced at " + cachedDate, Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    }

                    return;
                }
                final List<Delivery> deliveries = response.body();
                emptyVIew.setVisibility(View.INVISIBLE);
                if(cached == 1){
                    cached = 0;
                    totalDeliveries.clear();
                    deliveryAdapter.notifyDataSetChanged();
                }
                for(int i=0; i<deliveries.size();i++) {
                    totalDeliveries.add(deliveries.get(i));
                    deliveryAdapter.notifyItemInserted(totalDeliveries.size()-1);
                }
                mutex=0;
                swipeContainer.setRefreshing(false);
                deliveryAdapter.notifyDataSetChanged();
                Gson gson = new Gson();
                String json = gson.toJson(totalDeliveries);
                editor = shref.edit();
                editor.remove(dataKey).apply();
                editor.putString(dataKey, json);
                editor.apply();
                editor.remove(dateKey).apply();
                editor.putString(dateKey, DateFormat.getDateTimeInstance().format(new Date()));
                editor.apply();
                progressBar.setVisibility(View.GONE);
                if(layoutManager.getItemCount()==0) {
                    emptyVIew.setVisibility(View.VISIBLE);
                    deliveryMessage.setText(R.string.pending);
                    if(haveNetworkConnection() == false){
                        Snackbar snackbar = Snackbar
                                .make(swipeContainer, R.string.error_offline, Snackbar.LENGTH_LONG);
                        snackbar.show();
                    }
                    else {
                        Snackbar snackbar = Snackbar
                                .make(swipeContainer, R.string.error_online, Snackbar.LENGTH_LONG);
                        snackbar.show();
                    }
                }
                refresh = 0;
            }

            @Override
            public void onFailure(Call<List<Delivery>> call, Throwable t) {
                Log.e(TAG, t.toString());
                progressBar.setVisibility(View.GONE);
                mutex=0;
                Gson gson = new Gson();
                int itemCount = layoutManager.getItemCount();
                if(cached == 0 && itemCount == 0) {
                    String cachedData = shref.getString(dataKey, "");
                    List<Delivery> tempDelivery = gson.fromJson(cachedData,
                            new TypeToken<List<Delivery>>() {
                            }.getType());
                    if(tempDelivery != null) {
                        for (int i = 0; i < tempDelivery.size(); i++) {
                            totalDeliveries.add(tempDelivery.get(i));
                            deliveryAdapter.notifyItemInserted(totalDeliveries.size() - 1);
                        }
                        deliveryAdapter.notifyDataSetChanged();
                        cached = 1;
                    }
                }

                swipeContainer.setRefreshing(false);
                refresh = 0;
                if(itemCount > 0 && haveNetworkConnection() == true) {
                    Snackbar snackbar = Snackbar
                            .make(swipeContainer, R.string.error_online, Snackbar.LENGTH_LONG);
                    snackbar.show();
                    return;
                }
                else if(itemCount > 0 && haveNetworkConnection() == false) {
                    Snackbar snackbar = Snackbar
                            .make(swipeContainer, R.string.error_offline, Snackbar.LENGTH_LONG);
                    snackbar.show();
                    return;
                }
                if(layoutManager.getItemCount()==0) {
                    emptyVIew.setVisibility(View.VISIBLE);
                    deliveryMessage.setText(R.string.error);
                    if(haveNetworkConnection() == false){
                        Snackbar snackbar = Snackbar
                                .make(swipeContainer, R.string.error_offline, Snackbar.LENGTH_LONG);
                        snackbar.show();
                    }
                    else {
                        Snackbar snackbar = Snackbar
                                .make(swipeContainer, R.string.error_online, Snackbar.LENGTH_LONG);
                        snackbar.show();
                    }
                    return;
                }
                String cachedDate=shref.getString(dateKey , "");
                if(!cachedDate.equals("")) {
                    if(haveNetworkConnection() == false){
                        Snackbar snackbar = Snackbar
                                .make(swipeContainer, "Network issues. Last synced at " + cachedDate, Snackbar.LENGTH_LONG);
                        snackbar.show();
                    }
                    else {
                        Snackbar snackbar = Snackbar
                                .make(swipeContainer, "Last synced at " + cachedDate, Snackbar.LENGTH_LONG);
                        snackbar.show();
                    }
                }

            }
        });
    }
    private boolean haveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }
}
