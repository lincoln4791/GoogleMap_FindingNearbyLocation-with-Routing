package com.example.srarchingnearbyplaceswitrouting;

import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CallBackMap  {

    @GET("maps/api/place/nearbysearch/json?radius=3000&keyword=cruise")
    Call<JsonObject> callNearbyPlaces(@Query("type") String type,
                                      @Query("location") String location,
                                      @Query("key") String YOUR_API_KEY);

}
