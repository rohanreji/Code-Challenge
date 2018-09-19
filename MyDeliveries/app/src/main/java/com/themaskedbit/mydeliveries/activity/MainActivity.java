package com.themaskedbit.mydeliveries.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.themaskedbit.mydeliveries.R;
import com.themaskedbit.mydeliveries.adpater.DeliveryAdapter;
import com.themaskedbit.mydeliveries.model.Delivery;
import com.themaskedbit.mydeliveries.model.DeliveryLocation;
import com.themaskedbit.mydeliveries.rest.DeliveryApiService;

import junit.framework.TestCase;

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

    private static final String TAG = MainActivity.class.getSimpleName();
    public static final String BASE_URL = "http://8c204764.ngrok.io";
    private static Retrofit retrofit = null;
    private RecyclerView recyclerView = null;
    private TextView deliveryMessage = null;
    private static int mutex = 0;
    private SwipeRefreshLayout swipeContainer;
    private LinearLayoutManager layoutManager;
    private ProgressBar progressBar;
    private Button refreshButton = null;
    private RelativeLayout emptyVIew = null;
    private Context context = null;
    private DeliveryAdapter deliveryAdapter = null;
    private List<Delivery> totalDeliveries = null;
    private boolean isScrolling = false;
    private int currentItem = 0;
    private int totalItem = 0;
    private int scrollOutItem = 0;

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
        editor = shref.edit();

        recyclerView = (RecyclerView)findViewById(R.id.recycler_view);
        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        deliveryMessage = (TextView) findViewById(R.id.delivery_message);
        refreshButton = (Button)findViewById(R.id.refresh_button);
        emptyVIew = (RelativeLayout)findViewById(R.id.empty_view);
        progressBar = (ProgressBar)findViewById(R.id.progress);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        DeliveryAdapter.DeliveryViewClickListener mListener = new DeliveryAdapter.DeliveryViewClickListener() {
            @Override
            public void onClick(View view, int position) {
//                        Snackbar snackbar = Snackbar
//                                .make(swipeContainer, "clicked"+deliveries.get(position).getDescription(), Snackbar.LENGTH_LONG);
//                        snackbar.show();
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
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(mutex == 1){
                    //Handle - REVISIT - toast
                    return;
                }
                getDeliveries(0, 20);
            }

        });
        getDeliveries(0, 20);

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
                    getDeliveries(totalItem, 20);
                }
            }
        });

    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        getDeliveries(0, 20);
//    }

    public void getDeliveries(int offset, int limit) {
        mutex=1;
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
                    //handle - REVISIT - caching
                    Log.e(TAG, "response is null");
                    progressBar.setVisibility(View.GONE);

                    if(layoutManager.getItemCount()==0) {
                        emptyVIew.setVisibility(View.VISIBLE);
                        deliveryMessage.setText("No pending items!");
                    }
                    mutex=0;
                    swipeContainer.setRefreshing(false);
                    return;
                }
                final List<Delivery> deliveries = response.body();
                System.out.println(deliveries.size());
                System.out.println(deliveries.size());
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
                editor.remove(dataKey).commit();
                editor.putString(dataKey, json);
                editor.commit();
                editor.remove(dateKey).commit();
                editor.putString(dateKey, DateFormat.getDateTimeInstance().format(new Date()));
                editor.commit();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(Call<List<Delivery>> call, Throwable t) {
                Log.e(TAG, t.toString());
                progressBar.setVisibility(View.GONE);
                mutex=0;
                Gson gson = new Gson();
                if(cached == 0) {
                    String cachedData = shref.getString(dataKey, "");
                    List<Delivery> tempDelivery = gson.fromJson(cachedData,
                            new TypeToken<List<Delivery>>() {
                            }.getType());
                    for (int i = 0; i < tempDelivery.size(); i++) {
                        totalDeliveries.add(tempDelivery.get(i));
                        deliveryAdapter.notifyItemInserted(totalDeliveries.size() - 1);
                    }
                    deliveryAdapter.notifyDataSetChanged();
                    cached = 1;
                }
                if(layoutManager.getItemCount()==0) {
                    emptyVIew.setVisibility(View.VISIBLE);
                    deliveryMessage.setText("Couldn't fetch the items!");
                }
                String cachedDate=shref.getString(dateKey , "");
                if(!cachedDate.equals("")) {
                    Snackbar snackbar = Snackbar
                            .make(swipeContainer, "Last Sync on " +cachedDate, Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
                swipeContainer.setRefreshing(false);
            }
        });
    }

    public void refreshDeliveries(View v) {
        if(mutex == 1)
            return;
        getDeliveries(0,20);
    }

}
