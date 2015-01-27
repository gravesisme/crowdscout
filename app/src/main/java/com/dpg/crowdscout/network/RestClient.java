package com.dpg.crowdscout.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.util.Log;

import com.crowdscout.api.models.foursquare.FoursquareVenue;
import com.crowdscout.api.models.instagram.InstagramMedia;
import com.dpg.crowdscout.models.LocationModel;
import com.dpg.crowdscout.utils.JacksonConverter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public enum RestClient {
    INSTANCE;

    public interface PromiseCallback<T> {
        public void onSuccess(final @NonNull T t);

        public void onError(final @NonNull String message);
    }

    private static final String LOG_TAG = RestClient.class.getSimpleName();
    private static final String CROWD_SCOUT_API_URL = "http://fast-depths-4366.herokuapp.com/";

    private final CrowdScoutService m_service;

    private RestClient() {
        final RestAdapter m_adapter = new RestAdapter.Builder()
                .setEndpoint(CROWD_SCOUT_API_URL)
                .setLogLevel(RestAdapter.LogLevel.BASIC)
                .setConverter(createJacksonConverter())
                .build();

        m_service = m_adapter.create(CrowdScoutService.class);
    }

    public boolean isNetworkAvailable(@NonNull Context context) {
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean isNetworkAvailable = activeNetworkInfo != null && activeNetworkInfo.isConnected();
        Log.d(LOG_TAG, "isNetworkAvailable() = " + isNetworkAvailable);
        return isNetworkAvailable;
    }

    // *******************************************************
    // FOURSQUARE
    // *******************************************************

    /**
     * Get a List of FoursquareVenue models near an address for a specific filter
     *
     * @param address  {@link String} LocationModel.getEncodedName()
     * @param callback {@link PromiseCallback} Callback to execute after network request is resolved
     */
    public void explore(final @NonNull String address, final @NonNull PromiseCallback<List<FoursquareVenue>> callback) {
        explore(address, LocationModel.DEFAULT_VENUE_FILTER, false, callback);
    }

    public void explore(final @NonNull String address, final @NonNull LocationModel.VenueFilter filter, final @NonNull PromiseCallback<List<FoursquareVenue>> callback) {
        explore(address, filter, false, callback);
    }

    public void explore(final @NonNull String address, final @NonNull LocationModel.VenueFilter filter, final boolean forceRefresh, final @NonNull PromiseCallback<List<FoursquareVenue>> callback) {
        Log.d(LOG_TAG, String.format("exploreVenues(%s:%s)", address, filter));
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("section", filter.toString());
        queryParams.put("distance", "5000");

        if (forceRefresh) {
            queryParams.put("forceRefresh", "true");
        }

        m_service.exploreVenues(address, queryParams, wrapPromise(callback));
    }

    // *******************************************************
    // Instagram
    // *******************************************************

    /**
     * Get Recent InstagramMedia for a FoursquareVenue
     *
     * @param venueId  {@link String} FoursquareVenue ID
     * @param callback {@link PromiseCallback} Callback to execute after network request is resolved
     */
    public void getRecentFoursquareMedia(final @NonNull String venueId, final @NonNull PromiseCallback<List<InstagramMedia>> callback) {
        getRecentFoursquareMedia(venueId, false, callback);
    }

    public void getRecentFoursquareMedia(final @NonNull String venueId, final boolean forceRefresh, final @NonNull PromiseCallback<List<InstagramMedia>> callback) {
        getRecentFoursquareMedia(venueId, forceRefresh, Collections.EMPTY_MAP, callback);
    }

    public void getRecentFoursquareMedia(final @NonNull String venueId, final boolean forceRefresh, final @NonNull String maxId, final @NonNull PromiseCallback<List<InstagramMedia>> callback) {
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("max_id", maxId);
        getRecentFoursquareMedia(venueId, forceRefresh, queryParams, callback);
    }

    public void getRecentFoursquareMedia(final @NonNull String venueId, final boolean forceRefresh, final @NonNull Map<String, String> params, final @NonNull PromiseCallback<List<InstagramMedia>> callback) {
        Log.d(LOG_TAG, String.format("getRecentFoursquareMedia(%s, %s)", venueId, forceRefresh));
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.putAll(params);
        queryParams.put("forceRefresh", (forceRefresh ? "true" : "false"));
        m_service.getRecentFoursquareMedia(venueId, queryParams, wrapPromise(callback));
    }

    // *******************************************************
    // STATIC HELPERS
    // *******************************************************

    private static JacksonConverter createJacksonConverter() {
        final ObjectMapper mapper = new ObjectMapper();
        return new JacksonConverter(mapper);
    }

    private static <T> Callback<ApiResponse<T>> wrapPromise(final @NonNull PromiseCallback<T> callback) {
        return new Callback<ApiResponse<T>>() {
            @Override
            public void success(ApiResponse apiResponse, Response response) {
                if (apiResponse == null || !apiResponse.wasSuccessful()) {
                    callback.onError(apiResponse.status + ":" + response.getReason());
                    return;
                }

                callback.onSuccess((T) apiResponse.data);
            }

            @Override
            public void failure(RetrofitError error) {
                callback.onError(error.getMessage());
            }
        };
    }
}
