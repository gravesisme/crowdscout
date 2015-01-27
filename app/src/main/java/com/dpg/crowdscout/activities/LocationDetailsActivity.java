package com.dpg.crowdscout.activities;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.crowdscout.api.models.foursquare.FoursquareVenue;
import com.dpg.crowdscout.BuildConfig;
import com.dpg.crowdscout.fragments.LocationDetailsFragment;
import com.dpg.crowdscout.models.LocationModel;
import com.dpg.crowdscout.utils.SingleFragmentActivity;

public class LocationDetailsActivity extends SingleFragmentActivity implements LocationDetailsFragment.OnShowVenueDetailsListener {
    private static final String LOG_TAG = LocationDetailsActivity.class.getSimpleName();
    private static boolean DEBUG = BuildConfig.DEBUG && false;

    /**
     * Keys used for intent extras
     */
    private static final String KEY_PREFIX = LocationDetailsActivity.class.getName();
    public static final String KEY_LOCATION = KEY_PREFIX + ".LOCATION";

    /**
     * Fragment used by this Activity
     */
    private LocationDetailsFragment m_fragment;

    @Override
    protected Fragment createFragment() {
        final LocationModel locationModel = getIntent().getParcelableExtra(KEY_LOCATION);
        m_fragment = LocationDetailsFragment.newInstance(locationModel != null ? locationModel : LocationModel.newDefaultInstance());
        return m_fragment;
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (m_fragment != null) {
            m_fragment.setOnShowVenueDetailsListener(null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (m_fragment != null) {
            m_fragment.setOnShowVenueDetailsListener(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        m_fragment = null;
    }

    @Override
    public void showVenueDetails(@NonNull FoursquareVenue venue) {
        if (DEBUG) {
            Log.d(LOG_TAG, String.format("showVenueDetails(): %s", venue.name));
        }

        // Launch VenueDetailsActivity for the provided venue
        final Intent i = new Intent(this, VenueDetailsActivity.class);
        i.putExtra(VenueDetailsActivity.KEY_VENUE, venue);
        startActivity(i);
    }
}
