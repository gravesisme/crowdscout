package com.dpg.crowdscout.fragments;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.dpg.crowdscout.BuildConfig;
import com.dpg.crowdscout.R;
import com.dpg.crowdscout.adapters.BrowseLocationsAdapter;
import com.dpg.crowdscout.models.LocationModel;
import com.dpg.crowdscout.utils.ImageDownloader;
import com.dpg.crowdscout.utils.SimpleCallback;
import com.dpg.crowdscout.views.BrowseLocationsView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BrowseLocationsFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener {
    private static final String LOG_TAG = BrowseLocationsFragment.class.getSimpleName();
    private static boolean DEBUG = BuildConfig.DEBUG && false;
    private static boolean FILTER_CONTAINER_ENABLED = false;
    private static final int DEFAULT_NUM_COLUMNS = 2;

    /**
     * Keys used in bundle arguments
     */
    private static final String KEY_PREFIX = BrowseLocationsFragment.class.getName();
    private static final String KEY_LOCATIONS = KEY_PREFIX + ".LOCATIONS";

    /**
     * Interface responsible for displaying location details when user selects a location
     */
    public interface OnShowLocationListener {
        public void showModelDetails(@NonNull LocationModel locationModel);
    }

    /**
     * Adapter for displaying model details
     */
    private OnShowLocationListener m_onShowLocationListener;

    /**
     * Locations Adapter
     */
    protected BrowseLocationsAdapter m_adapter;

    /**
     * View
     */
    protected BrowseLocationsView m_view;

    /**
     * Contains the list of locations
     */
    private List<LocationModel> m_locations;

    /**
     * Amount of spacing between grid view items
     */
    private int m_gridViewSpacing;

    /**
     * Used to determine sort order
     */
    private boolean m_asc = true;

    /**
     * Image cache used by adapter
     */
    private ImageDownloader.ImageCache m_imageCache;

    /**
     * Creates a new Fragment instance using the provided list of Location Models
     *
     * @param locations {@link LocationModel} List of locations to associate with the new instance
     * @return {@link BrowseLocationsFragment} New fragment instance containing provided locations
     */
    public static BrowseLocationsFragment newInstance(@NonNull List<LocationModel> locations) {
        final BrowseLocationsFragment fragment = new BrowseLocationsFragment();
        final Bundle args = new Bundle();
        args.putParcelableArrayList(KEY_LOCATIONS, new ArrayList<Parcelable>(locations));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (DEBUG) {
            Log.d(LOG_TAG, "onCreate()");
        }

        super.onCreate(savedInstanceState);

        // Get the list of locations passed to this fragment
        final Bundle args = getArguments();
        if (args != null) {
            m_locations = args.getParcelableArrayList(KEY_LOCATIONS);
            if (m_locations == null) {
                m_locations = new ArrayList<>();
            }
        }

        // Finish initializing fragment
        m_gridViewSpacing = getResources().getDimensionPixelSize(R.dimen.coll_grid_view_spacing);
        m_adapter = new BrowseLocationsAdapter(getActivity(), m_locations);
        m_imageCache = ImageDownloader.BitmapLruCache.newInstance(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        m_view = BrowseLocationsView.newInstance(getActivity());

        // Decorate view based on whether FilterContainer should be enabled
        if (FILTER_CONTAINER_ENABLED) {
            m_view.showFilterContainer();
        } else {
            m_view.hideFilterContainer();

            // Increase grid view margin top when hiding filter container
            m_view.setGridViewMarginTop(getResources().getDimensionPixelSize(R.dimen.browse_grid_margin_top_under_label));
        }

        // Show the empty view when the user has no locations of their own
        if (m_locations.isEmpty()) {
            m_view.showEmptyView();
        } else {
            // Otherwise, make sure it is hidden
            m_view.hideEmptyView();
        }

        /**
         * When view layout first changes, measure the view and inform GridView of target
         * dimensions via setupAdapter() call. Remove callback once complete first time.
         */
        m_view.setOnLayoutChangedCallback(new SimpleCallback() {
            @Override
            public void onComplete() {
                if (m_view.getWidth() > 0) {
                    setupAdapter();
                    m_view.setOnLayoutChangedCallback(null);
                }
            }
        });

        return m_view;
    }

    @Override
    public void onPause() {
        if (DEBUG) {
            Log.d(LOG_TAG, "onPause()");
        }

        super.onPause();

        // Remove all references to browse view
        m_adapter.setImageCache(null);
        m_view.setOnCreateLocationClickListener(null);
        m_view.setSortOrderClickListener(null);
        m_view.setGridViewOnItemClickListener(null);
        m_view.setOnLayoutChangedCallback(null);
    }

    @Override
    public void onResume() {
        if (DEBUG) {
            Log.d(LOG_TAG, "onResume()");
        }

        super.onResume();

        // Initialize image cache
        if (m_imageCache == null) {
            m_imageCache = ImageDownloader.BitmapLruCache.newInstance(getActivity());
        }

        // Set references
        m_adapter.setImageCache(m_imageCache);
        m_view.setLongClickable(false);
        m_view.setOnCreateLocationClickListener(this);
        m_view.setSortOrderClickListener(this);
        m_view.setGridViewOnItemClickListener(this);
        m_view.setAdapter(m_adapter);
    }

    @Override
    public void onStop() {
        if (DEBUG) {
            Log.d(LOG_TAG, "onStop()");
        }

        super.onStop();

        // Destroy image cache in onStop instead of onPause in case we want to save something
        // during onSaveInstanceState which is called after onPause. Potential future improvement.
        if (m_imageCache != null) {
            m_imageCache.clear();
            m_imageCache = null;
        }
    }

    @Override
    public void onDestroy() {
        if (DEBUG) {
            Log.d(LOG_TAG, "onDestroy()");
        }

        super.onDestroy();

        // Make sure image cache has been destroyed (onDestory can be called without onStop when orientation change occurs)
        if (m_imageCache != null) {
            m_adapter.setImageCache(null); // just in case onDestroy is called without onPause (ie: orientation change)
            m_imageCache.clear();
            m_imageCache = null;
        }

        // Remove adapter reference
        m_view.setAdapter(null);

        // Clear list of locations
        m_locations.clear();

        // Clear adapter
        m_adapter.clear();
    }

    /**
     * Informs grid view adapter how tall each grid item should be
     */
    private void setupAdapter() {
        // Adapter required
        if (m_adapter == null) {
            return;
        }

        final int numColumns = DEFAULT_NUM_COLUMNS;
        final int columnSize = (m_view.getWidth() / numColumns) - m_gridViewSpacing;

        // Setup adapter based on measured column width
        m_adapter.setNumColumns(numColumns);
        m_adapter.setItemHeight(columnSize);
    }

    /**
     * Reverses current sort order and smooth scrolls to the top of the list
     */
    private void toggleSortOrder() {
        m_asc = !m_asc;
        m_view.toggleSortButtonState(m_asc);
        sortLocations(m_locations);
        m_adapter.notifyDataSetChanged();
        m_view.postDelayed(new Runnable() {
            @Override
            public void run() {
                m_view.scrollToTop();
            }
        }, 100);
    }

    /**
     * Used to sort the list of models
     * <p/>
     * Not called on UI thread
     */
    public void sortLocations(final List<LocationModel> locations) {
        if (m_asc) {
            Collections.sort(locations);
        } else {
            Collections.sort(locations, Collections.reverseOrder());
        }
    }

    /**
     * Click handler for sortButton
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.order_by_button) {
            toggleSortOrder();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (m_onShowLocationListener == null) {
            return;
        }

        final LocationModel model = m_locations.get(position);

        if (model != null) {
            m_onShowLocationListener.showModelDetails(model);
        } else {
            Log.e(LOG_TAG, "onItemClick(): NULL LocationModel detected at position: " + position);
        }
    }

    public void setOnShowLocationListener(OnShowLocationListener adapter) {
        m_onShowLocationListener = adapter;
    }
}
