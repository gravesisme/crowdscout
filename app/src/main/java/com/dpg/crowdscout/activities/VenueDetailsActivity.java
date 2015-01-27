package com.dpg.crowdscout.activities;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.crowdscout.api.models.foursquare.FoursquareVenue;
import com.crowdscout.api.models.instagram.InstagramMedia;
import com.dpg.crowdscout.BuildConfig;
import com.dpg.crowdscout.fragments.VenueDetailsFragment;
import com.dpg.crowdscout.utils.SingleFragmentActivity;

public class VenueDetailsActivity extends SingleFragmentActivity implements VenueDetailsFragment.OnShowMediaDetailsListener {
    private static final String LOG_TAG = VenueDetailsActivity.class.getSimpleName();
    private static boolean DEBUG = BuildConfig.DEBUG && false;

    /**
     * Keys used for intent extras
     */
    private static final String KEY_PREFIX = LocationDetailsActivity.class.getName();
    public static final String KEY_VENUE = KEY_PREFIX + ".VENUE";

    /**
     * Fragment used by this Activity
     */
    private VenueDetailsFragment m_fragment;

    @Override
    protected Fragment createFragment() {
        final FoursquareVenue venueModel = getIntent().getParcelableExtra(KEY_VENUE);
        m_fragment = VenueDetailsFragment.newInstance(venueModel != null ? venueModel : new FoursquareVenue());
        return m_fragment;
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (m_fragment != null) {
            m_fragment.setOnShowMediaDetailsListener(null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (m_fragment != null) {
            m_fragment.setOnShowMediaDetailsListener(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        m_fragment = null;
    }

    @Override
    public void showMediaDetails(@NonNull InstagramMedia media) {
        // TODO: Implement and launch Intent for MediaDetailsActivity
        Toast.makeText(this, "Not Implemented Yet!", Toast.LENGTH_SHORT).show();
    }
}
