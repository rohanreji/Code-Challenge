package com.themaskedbit.mydeliveries.model;

import com.google.gson.annotations.SerializedName;

public class Delivery {
    @SerializedName("id")
    private int id;
    @SerializedName("description")
    private String description;
    @SerializedName("imageUrl")
    private String imageUrl;
    public DeliveryLocation location;

    public Delivery(int id, String description, String imageUrl, DeliveryLocation location) {
        this.id = id;
        this.description = description;
        this.imageUrl = imageUrl;
        this.location = location;
    }

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public DeliveryLocation getLocation() {
        return location;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setLocation(DeliveryLocation location) {
        this.location = location;
    }
}
