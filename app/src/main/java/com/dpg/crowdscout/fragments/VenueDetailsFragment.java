package com.dpg.crowdscout.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.crowdscout.api.models.foursquare.FoursquareVenue;
import com.crowdscout.api.models.instagram.InstagramMedia;
import com.dpg.crowdscout.BuildConfig;
import com.dpg.crowdscout.R;
import com.dpg.crowdscout.adapters.VenueDetailsAdapter;
import com.dpg.crowdscout.network.RestClient;
import com.dpg.crowdscout.utils.ImageDownloader;
import com.dpg.crowdscout.widgets.ProgressDialogFragment;
import com.google.common.collect.Iterables;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class VenueDetailsFragment extends Fragment implements AdapterView.OnItemClickListener {
    private static final String LOG_TAG = VenueDetailsFragment.class.getSimpleName();
    private static boolean DEBUG = BuildConfig.DEBUG && false;

    /**
     * Keys used in bundle arguments
     */
    private static final String KEY_PREFIX = LocationDetailsFragment.class.getName();
    private static final String KEY_VENUE = KEY_PREFIX + ".VENUE";
    private static final String KEY_MEDIA_LIST = KEY_PREFIX + ".MEDIA_LIST";

    /**
     * Interface responsible for displaying venue details when user selects a venue
     */
    public interface OnShowMediaDetailsListener {
        public void showMediaDetails(@NonNull InstagramMedia media);
    }

    /**
     * Adapter for displaying media details
     */
    private OnShowMediaDetailsListener m_onShowMediaDetailsListener;

    /**
     * Adapter used to render all the media associated with the active venue
     */
    private VenueDetailsAdapter m_adapter;

    /**
     * GridView
     */
    protected GridView m_gridView;

    /**
     * Contains the active venue model
     */
    private FoursquareVenue m_venue;

    /**
     * Contains the SortedSet of media for the active location
     */
    private final Set<InstagramMedia> m_items = new TreeSet<>();

    /**
     * Image cache used by adapter
     */
    private ImageDownloader.ImageCache m_imageCache;

    /**
     * Creates a new Fragment instance using the provided list of Location Models
     *
     * @param venue {@link FoursquareVenue} FoursquareVenue to associate with the new instance
     * @return {@link VenueDetailsFragment} New fragment instance associated w/ provided venue model
     */
    public static VenueDetailsFragment newInstance(@NonNull FoursquareVenue venue) {
        final VenueDetailsFragment fragment = new VenueDetailsFragment();
        final Bundle args = new Bundle();
        args.putParcelable(KEY_VENUE, venue);
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
            m_venue = args.getParcelable(KEY_VENUE);
        }

        // Finish initializing fragment
        m_adapter = new VenueDetailsAdapter(getActivity(), m_items);
        m_imageCache = ImageDownloader.BitmapLruCache.newInstance(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.venue_details_view, null);

        // Set UI references
        m_gridView = (GridView) view.findViewById(R.id.venue_details_grid_view);

        // Set header label using active venue
        final TextView headerLabel = (TextView) view.findViewById(R.id.venue_details_header_label);
        headerLabel.setText(m_venue.name);

        return view;
    }

    @Override
    public void onPause() {
        if (DEBUG) {
            Log.d(LOG_TAG, "onPause()");
        }

        super.onPause();

        // Remove all references to browse view
        m_adapter.setImageCache(null);
        m_gridView.setOnItemClickListener(null);
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
        m_gridView.setOnItemClickListener(this);
        m_gridView.setAdapter(m_adapter);

        // Refresh media
        refreshMedia();
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
        super.onDestroy();

        // Make sure image cache has been destroyed (onDestory can be called without onStop when orientation change occurs)
        if (m_imageCache != null) {
            m_adapter.setImageCache(null); // just in case onDestroy is called without onPause (ie: orientation change)
            m_imageCache.clear();
            m_imageCache = null;
        }

        // Remove adapter reference
        m_gridView.setAdapter(null);

        // Clear list of locations
        m_items.clear();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (DEBUG) {
            Log.d(LOG_TAG, String.format("onItemClick(%d)", position));
        }

        if (m_onShowMediaDetailsListener == null) {
            return;
        }

        final InstagramMedia model = Iterables.get(m_items, position);

        if (model != null) {
            m_onShowMediaDetailsListener.showMediaDetails(model);
        } else {
            Log.e(LOG_TAG, "onItemClick(): NULL InstagramMedia detected at position: " + position);
        }
    }

    public void setOnShowMediaDetailsListener(OnShowMediaDetailsListener adapter) {
        m_onShowMediaDetailsListener = adapter;
    }

    void refreshMedia() {
        // Make sure there is an active venue
        if (m_venue == null) {
            Log.w(LOG_TAG, "refreshMedia() called before venue initialized");
            return;
        }

        // Show Progress Dialog while fetch is in progress
        ProgressDialogFragment.show(getFragmentManager(), false);

        // Fetch Recent Media for the active venue
        RestClient.INSTANCE.getRecentFoursquareMedia(m_venue.id, m_items.isEmpty(), new RestClient.PromiseCallback<List<InstagramMedia>>() {
            @Override
            public void onSuccess(@NonNull List<InstagramMedia> media) {
                Log.d(LOG_TAG, "refreshMedia(): Finished fetching media.. # Fetched: " + media.size());

                // Add all media to Set of items associated with list adapter
                m_items.addAll(media);

                // Refresh list
                m_adapter.notifyDataSetChanged();

                Log.d(LOG_TAG, "onSuccess(): m_adapter.getCount() = " + m_adapter.getCount());

                // TODO: Show empty view if there is no recent media. For now, a toast is sufficient
                if (m_items.isEmpty()) {
                    Toast.makeText(VenueDetailsFragment.this.getActivity(), "No Recent Media :( Try again later!", Toast.LENGTH_SHORT).show();
                }

                // Hide Progress Dialog on success
                ProgressDialogFragment.hide();
            }

            @Override
            public void onError(@NonNull String message) {
                Log.e(LOG_TAG, String.format("refreshMedia(%s): Error! Unable to fetch venues: %s", m_venue.name, message));

                // Hide Progress Dialog on error
                ProgressDialogFragment.hide();

                // Show error message
                Toast.makeText(VenueDetailsFragment.this.getActivity(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
