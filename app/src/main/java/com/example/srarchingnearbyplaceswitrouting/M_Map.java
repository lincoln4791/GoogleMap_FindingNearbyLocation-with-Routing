package com.example.srarchingnearbyplaceswitrouting;
import com.google.gson.annotations.SerializedName;

public class M_Map {
    @SerializedName("lat")
    String lat;

    @SerializedName("lng")
    String lng;

    @SerializedName("name")
    String placeName;

    public M_Map(String lat, String lng, String placeName) {
        this.lat = lat;
        this.lng = lng;
        this.placeName = placeName;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLng() {
        return lng;
    }

    public void setLng(String lng) {
        this.lng = lng;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }
}
