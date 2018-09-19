package com.themaskedbit.mydeliveries.rest;

import com.themaskedbit.mydeliveries.model.Delivery;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.QueryMap;

public interface DeliveryApiService {
    @GET("deliveries")
    Call<List<Delivery>> getAllDeliveries(@QueryMap Map<String, Integer> parameters);
}
