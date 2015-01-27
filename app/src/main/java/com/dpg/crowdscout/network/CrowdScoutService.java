package com.dpg.crowdscout.network;

import com.crowdscout.api.models.foursquare.FoursquareVenue;
import com.crowdscout.api.models.instagram.InstagramMedia;

import java.util.List;
import java.util.Map;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.QueryMap;

public interface CrowdScoutService {
    // *******************************************************
    // Foursquare
    // *******************************************************

    @GET("/foursquare/explore/near/{address}")
    public void exploreVenues(@Path("address") String address, @QueryMap Map<String, String> options, Callback<ApiResponse<List<FoursquareVenue>>> callback);

    // *******************************************************
    // Instagram
    // *******************************************************

    @GET("/foursquare/venues/{venueId}/instagram/media")
    public void getRecentFoursquareMedia(@Path("venueId") String venueId, @QueryMap Map<String, String> options, Callback<ApiResponse<List<InstagramMedia>>> callback);
}
