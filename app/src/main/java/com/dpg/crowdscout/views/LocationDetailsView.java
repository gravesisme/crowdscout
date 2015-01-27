package com.dpg.crowdscout.views;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.crowdscout.api.models.foursquare.FoursquareVenue;
import com.dpg.crowdscout.R;
import com.dpg.crowdscout.adapters.LocationDetailsAdapter;
import com.dpg.crowdscout.models.LocationModel;

/**
 * Created by dgraves on 1/28/15.
 */
public class LocationDetailsView extends FrameLayout {
    private static final String LOG_TAG = LocationDetailsView.class.getSimpleName();

    private LocationDetailsHeaderView m_headerView;
    private ListView m_listView;

    private final Animation m_animFadeIn;
    private boolean m_animateFadeIn = true;

    private final Runnable m_showListView = new Runnable() {
        @Override
        public void run() {
            if (m_listView != null) {
                if (m_animateFadeIn) {
                    m_listView.startAnimation(m_animFadeIn);
                }
                m_listView.setVisibility(VISIBLE);
            }
        }
    };

    // *******************************************************
    // Factory Method
    // *******************************************************

    public static LocationDetailsView newInstance(@NonNull Context context, @NonNull LocationModel locationModel) {
        // Build - and decorate - the view using the provided location model
        final LocationDetailsView view = new LocationDetailsView(context);
        view.decorate(locationModel);
        return view;
    }

    // *******************************************************
    // CONSTRUCTORS
    // *******************************************************

    public LocationDetailsView(Context context) {
        this(context, null);
    }

    public LocationDetailsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LocationDetailsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // Load animation resource
        m_animFadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in);

        // Inflate view
        final LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.location_details_view, this);

        // Set view references
        m_listView = (ListView) findViewById(R.id.location_venues_list);

        // Setup the ListView
        if (m_listView != null) {
            m_listView.setCacheColorHint(Color.TRANSPARENT);
            m_listView.setScrollingCacheEnabled(false);
            m_listView.setSelector(R.drawable.selector_collected_places_list);
            m_listView.setDrawSelectorOnTop(true);

            // Initialize the header
            m_headerView = new LocationDetailsHeaderView(getContext());
            m_headerView.setClickable(false);
            m_listView.setSelectionAfterHeaderView();

            // Add list header view
            m_listView.addHeaderView(m_headerView);
        }
    }

    /**
     * Sets the adapter that should be used to populate the ListView
     *
     * @param adapter {@link com.dpg.crowdscout.adapters.LocationDetailsAdapter}
     */
    public void setAdapter(LocationDetailsAdapter adapter) {
        if (m_listView != null) {
            m_listView.setAdapter(adapter);
        }
    }

    /**
     * Sets an click listener that should be executed whenever an item is clicked
     *
     * @param listener {@link android.view.View.OnClickListener} Listener to execute onItemClick
     */
    public void setListViewOnItemClickListener(AdapterView.OnItemClickListener listener) {
        if (m_listView != null) {
            m_listView.setOnItemClickListener(listener);
        }
    }

    /**
     * Triggers a smooth scroll to the top of the list
     */
    public void scrollToTop() {
        if (m_listView != null) {
            m_listView.smoothScrollToPosition(0);
        }
    }

    public void decorate(@NonNull LocationModel locationModel) {
        if (m_headerView != null) {
            m_headerView.decorate(locationModel);
        }
    }

    public void updateNumItemsLabel(int numItems) {
        if (m_headerView != null) {
            m_headerView.updateNumItemsLabel(numItems);
        }
    }

    /**
     * Helper for determining if the list view is currently visible
     */
    public boolean isListViewVisible() {
        return m_listView == null ? false : (m_listView.getVisibility() == VISIBLE);
    }

    /**
     * Helper for hiding the list view
     */
    public void hideListView() {
        if (m_listView != null) {
            m_listView.setVisibility(GONE);
        }
    }

    /**
     * Helper for showing the list view
     */
    public void showListView() {
        showListView(false);
    }

    public void showListView(boolean animated) {
        if (m_listView != null) {
            if (animated) {
                removeCallbacks(m_showListView);
                post(m_showListView);
            } else {
                m_listView.setVisibility(VISIBLE);
            }
        }
    }

    public FoursquareVenue getListViewItem(int position) {
        if (position >= 0) {
            final Object item = m_listView.getItemAtPosition(position);
            if (item != null && (item instanceof FoursquareVenue)) {
                return ((FoursquareVenue) item);
            }
        }
        return null;
    }
}
