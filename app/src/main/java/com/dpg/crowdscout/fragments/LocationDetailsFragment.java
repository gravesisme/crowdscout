package com.dpg.crowdscout.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.Toast;

import com.crowdscout.api.models.foursquare.FoursquareVenue;
import com.dpg.crowdscout.BuildConfig;
import com.dpg.crowdscout.R;
import com.dpg.crowdscout.adapters.LocationDetailsAdapter;
import com.dpg.crowdscout.models.LocationModel;
import com.dpg.crowdscout.models.VenueWrapper;
import com.dpg.crowdscout.network.RestClient;
import com.dpg.crowdscout.utils.ImageDownloader;
import com.dpg.crowdscout.views.LocationDetailsView;
import com.dpg.crowdscout.widgets.ProgressDialogFragment;
import com.google.common.collect.Iterables;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by dgraves on 1/27/15.
 */
public class LocationDetailsFragment extends Fragment implements AdapterView.OnItemClickListener {
    private static final String LOG_TAG = LocationDetailsFragment.class.getSimpleName();
    private static boolean DEBUG = BuildConfig.DEBUG && false;

    /**
     * Keys used in bundle arguments
     */
    private static final String KEY_PREFIX = LocationDetailsFragment.class.getName();
    private static final String KEY_LOCATION = KEY_PREFIX + ".LOCATION";
    private static final String KEY_VENUES = KEY_PREFIX + ".VENUES";

    /**
     * Interface responsible for displaying venue details when user selects a venue
     */
    public interface OnShowVenueDetailsListener {
        public void showVenueDetails(@NonNull FoursquareVenue venue);
    }

    /**
     * Adapter for displaying venue details
     */
    private OnShowVenueDetailsListener m_onShowVenueDetailsListener;

    /**
     * Location Details Adapter. Contains a header showing general info about the location and
     * a list of Venues that correspond to the location's active category
     */
    private LocationDetailsAdapter m_adapter;

    /**
     * View
     */
    protected LocationDetailsView m_view;

    /**
     * Contains the active location model
     */
    private LocationModel m_location;

    /**
     * Contains the set of venues for the active location
     */
    private final Set<FoursquareVenue> m_items = new LinkedHashSet<>(25);

    /**
     * Image cache used by adapter
     */
    private ImageDownloader.ImageCache m_imageCache;

    /**
     * Creates a new Fragment instance using the provided list of Location Models
     *
     * @param locationModel {@link LocationModel} LocationModel to associate with the new instance
     * @return {@link LocationDetailsFragment} New fragment instance associated w/ provided location model
     */
    public static LocationDetailsFragment newInstance(@NonNull LocationModel locationModel) {
        final LocationDetailsFragment fragment = new LocationDetailsFragment();
        final Bundle args = new Bundle();
        args.putParcelable(KEY_LOCATION, locationModel);
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
            m_location = args.getParcelable(KEY_LOCATION);
            if (m_location == null) {
                m_location = LocationModel.newDefaultInstance();
            }
        }

        // Finish initializing fragment
        m_adapter = new LocationDetailsAdapter(getActivity(), m_items);
        m_imageCache = ImageDownloader.BitmapLruCache.newInstance(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        m_view = LocationDetailsView.newInstance(getActivity(), m_location);
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
        m_view.setListViewOnItemClickListener(null);
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
        m_view.setListViewOnItemClickListener(this);
        m_view.setAdapter(m_adapter);

        // Refresh venues
        refreshVenues();
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
        m_view.setAdapter(null);

        // Clear list of locations
        m_items.clear();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (DEBUG) {
            Log.d(LOG_TAG, String.format("onItemClick(%d)", position));
        }

        if (m_onShowVenueDetailsListener == null) {
            return;
        }

        final FoursquareVenue model = m_view.getListViewItem(position);

        if (model != null) {
            m_onShowVenueDetailsListener.showVenueDetails(model);
        } else {
            Log.e(LOG_TAG, "onItemClick(): NULL FoursquareVenue detected at position: " + position);
        }
    }

    public void setOnShowVenueDetailsListener(OnShowVenueDetailsListener adapter) {
        m_onShowVenueDetailsListener = adapter;
    }

    /**
     * This method will eventually be responsible for the animation of removing a venue from
     * the list of saved venues; once the animation completes, the actual business logic
     * will be executed
     *
     * @param view {@link View} SaveVenueTextButton view that contains parent view and item position
     */
    @SuppressWarnings("unused")
    void removeVenueFromList(View view) {
        final View parentView = (View) view.getTag(R.id.location_details_list_item_key_parent);
        final Integer position = (Integer) view.getTag(R.id.location_details_list_item_key_position);
        final LocationDetailsAdapter.ViewHolder viewHolder = parentView == null ? null : (LocationDetailsAdapter.ViewHolder) parentView.getTag();

        if (parentView != null && position != null && viewHolder != null) {
            final FoursquareVenue venue = Iterables.get(m_items, position);
            final VenueWrapper wrappedVenue = new VenueWrapper(venue);

            // Define listener that will update adapter when the remove animation finishes
            Animation.AnimationListener animationListener = new Animation.AnimationListener() {
                @Override
                public void onAnimationEnd(Animation arg0) {
                    // Remove venue from list of saved venues
                    wrappedVenue.unsave();

                    // Remove place from list
                    m_items.remove(position.intValue());

                    // Mark the row that was deleted as needing to be re-inflated
                    viewHolder.needsInflate = true;

                    // Notify the adapter to update the list
                    m_adapter.notifyDataSetChanged();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationStart(Animation animation) {
                }
            };

            // Update numItems label
            m_view.updateNumItemsLabel(m_items.size() - 1);

            // Start delete place animation
            collapseRow(parentView, animationListener);
        }
    }

    void collapseRow(@NonNull final View v, @NonNull final Animation.AnimationListener al) {
        // Store the initial height for transformation calculations
        final int initialHeight = v.getMeasuredHeight();

        // Animation that will gradually shrink the provided view
        final Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.hide_list_item);

        // Update view height using interpolator to ensure any items below the item being collapsed are moved up
        anim.setInterpolator(new DecelerateInterpolator() {
            @Override
            public float getInterpolation(float input) {
                float interpolatedTime = super.getInterpolation(input);
                if (input < 1) {
                    v.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                } else {
                    v.getLayoutParams().height = 0;
                    v.setVisibility(View.GONE);
                }
                v.requestLayout();
                return interpolatedTime;
            }
        });

        // Start collapse animation
        anim.setAnimationListener(al);
        v.startAnimation(anim);
    }

    void refreshVenues() {
        // Make sure there is an active location
        if (m_location == null) {
            Log.w(LOG_TAG, "refreshVenues() called before location initialized");
            return;
        }

        // Show Progress Dialog while fetch is in progress
        ProgressDialogFragment.show(getFragmentManager(), false);

        // Fetch Venues for the active location/category
        RestClient.INSTANCE.explore(m_location.getEncodedName(), m_location.getCategory(), m_items.isEmpty(), new RestClient.PromiseCallback<List<FoursquareVenue>>() {
            @Override
            public void onSuccess(@NonNull List<FoursquareVenue> venues) {
                Log.d(LOG_TAG, "refreshVenues(): Finished fetching venues.. # Fetched: " + venues.size());

                // Add all venues to Set of items associated with list adapter
                m_items.addAll(venues);

                // Refresh list
                m_adapter.notifyDataSetChanged();

                Log.d(LOG_TAG, "onSuccess(): m_adapter.getCount() = " + m_adapter.getCount());

                // TODO: Show empty view if there are no results. For now, a toast is sufficient
                if (m_items.isEmpty()) {
                    Toast.makeText(LocationDetailsFragment.this.getActivity(), "No Venues Found :( Try again later!", Toast.LENGTH_SHORT).show();
                }

                // Hide Progress Dialog on success
                ProgressDialogFragment.hide();
            }

            @Override
            public void onError(@NonNull String message) {
                Log.e(LOG_TAG, String.format("refreshVenues(%s:%s): Error! Unable to fetch venues: %s", m_location.getName(), m_location.getCategory(), message));

                // Hide Progress Dialog on error
                ProgressDialogFragment.hide();

                // Show error message
                Toast.makeText(LocationDetailsFragment.this.getActivity(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
