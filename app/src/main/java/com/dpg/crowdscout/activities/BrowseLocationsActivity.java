package com.dpg.crowdscout.activities;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.dpg.crowdscout.BuildConfig;
import com.dpg.crowdscout.fragments.BrowseLocationsFragment;
import com.dpg.crowdscout.models.LocationModel;
import com.dpg.crowdscout.utils.Helpers;
import com.dpg.crowdscout.utils.SingleFragmentActivity;

import java.util.List;

public class BrowseLocationsActivity extends SingleFragmentActivity implements BrowseLocationsFragment.OnShowLocationListener {
    private static final String LOG_TAG = BrowseLocationsActivity.class.getSimpleName();
    private static boolean DEBUG = BuildConfig.DEBUG && false;

    /**
     * Fragment used by this Activity
     */
    private BrowseLocationsFragment m_fragment;

    @Override
    protected Fragment createFragment() {
        final List<LocationModel> locations = Helpers.INSTANCE.loadLocationModels();
        m_fragment = BrowseLocationsFragment.newInstance(locations);
        return m_fragment;
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (m_fragment != null) {
            m_fragment.setOnShowLocationListener(null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (m_fragment != null) {
            m_fragment.setOnShowLocationListener(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        m_fragment = null;
    }

    @Override
    public void showModelDetails(@NonNull LocationModel locationModel) {
        if (DEBUG) {
            Log.d(LOG_TAG, String.format("showModelDetails(): %s", locationModel.getName()));
        }

        // Launch LocationDetailsActivity for the provided location
        final Intent i = new Intent(this, LocationDetailsActivity.class);
        i.putExtra(LocationDetailsActivity.KEY_LOCATION, locationModel);
        startActivity(i);
    }
}
