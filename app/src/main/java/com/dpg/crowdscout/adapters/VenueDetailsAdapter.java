package com.dpg.crowdscout.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.crowdscout.api.models.instagram.InstagramMedia;
import com.dpg.crowdscout.BuildConfig;
import com.dpg.crowdscout.R;
import com.dpg.crowdscout.utils.Helpers;
import com.dpg.crowdscout.utils.ImageDownloader;
import com.google.common.collect.Iterables;

import java.util.Set;

public class VenueDetailsAdapter extends BaseAdapter {
    private static final String LOG_TAG = VenueDetailsAdapter.class.getSimpleName();
    private static boolean DEBUG = BuildConfig.DEBUG && false;

    private final Resources m_resources;
    private final LayoutInflater m_layoutInflater;
    private final ImageDownloader m_imageDownloader;
    private final ImageDownloader.BitmapDimension m_targetDimension;
    private final String m_nowText;
    private final String m_newText;

    /**
     * Stores all of the items in the adapter
     */
    private final Set<InstagramMedia> m_items;

    // *******************************************************
    // CONSTRUCTOR
    // *******************************************************

    public VenueDetailsAdapter(final Context context, final Set<InstagramMedia> items) {
        super();
        m_items = items;
        m_layoutInflater = LayoutInflater.from(context);
        m_resources = context.getResources();

        // Create target dimension
        final int imageSize = m_resources.getDimensionPixelSize(R.dimen.image_thumbnail_size);
        m_targetDimension = new ImageDownloader.BitmapDimension(imageSize, imageSize);

        // Get the placeholder bitmap that will be used while remote images load
        final Bitmap placeholderImage = BitmapFactory.decodeResource(m_resources,
                R.drawable.location_image_placeholder);

        // Initialize image downloader
        m_imageDownloader = new ImageDownloader(context, placeholderImage);

        // Assign target dimension
        m_imageDownloader.setTargetDimension(m_targetDimension);

        // Use a sample size of 2 to save memory
        //m_imageDownloader.setBitmapSampleSize(2);

        // Enable fade to add visual cue for when image is not ready right away. This is necessary because the image
        // url is only available after fetching place details from Places API, which almost doubles time to load
        m_imageDownloader.enableFadeIn();

        // Execute downloads using global thread pool
        m_imageDownloader.setExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        // Get localized strings that will be used often
        m_nowText = m_resources.getString(R.string.recent_now);
        m_newText = m_resources.getString(R.string.recent_new);
    }

    /**
     * Get # of items in the list
     */
    @Override
    public int getCount() {
        return (m_items.size());
    }

    /**
     * Get Item at position X
     */
    @Override
    public InstagramMedia getItem(int position) {
        return Iterables.get(m_items, position);
    }

    /**
     * Get item Id
     */
    @Override
    public long getItemId(int position) {
        final InstagramMedia item = getItem(position);
        if (item != null) {
            long hashCode = Long.valueOf(item.hashCode());
            // Make sure the hash code was computed correctly
            if (hashCode > 0) {
                return hashCode;
            }
        }

        // Fallback is to use item position
        return position;
    }

    public void setImageCache(ImageDownloader.ImageCache imageCache) {
        m_imageDownloader.setImageCache(imageCache);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view;
        final ViewHolder viewHolder;
        final InstagramMedia item = getItem(position);

        if (convertView == null) {
            view = m_layoutInflater.inflate(R.layout.venue_details_grid_item, parent, false);
            viewHolder = new ViewHolder(view);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }

        // Show video controls if current item is a video
        viewHolder.videoControls.setVisibility(item.isVideo() ? View.VISIBLE : View.GONE);

        // Show recent label if media is less than 10 hours old
        boolean isRecent = Helpers.INSTANCE.isLessThanXHours(item.created_time, 10);
        viewHolder.recentLabel.setVisibility(isRecent ? View.VISIBLE : View.GONE);

        if (isRecent) {
            // Show NOW label if less than '2' mins, otherwise show NEW
            boolean isNow = Helpers.INSTANCE.isLessThanXMinutes(item.created_time, 2);
            viewHolder.recentLabel.setText(isNow ? m_nowText : m_newText);
            viewHolder.recentLabel.setTextColor(m_resources.getColor(isNow ? android.R.color.holo_red_light : android.R.color.white));
        }

        // Load preview image
        m_imageDownloader.loadImageInto(item.images.low_resolution.url, viewHolder.imageView);

        return view;
    }

    /**
     * ViewHolder class used to ensure view components will only have to be initialized once per
     * available cell
     */
    public static class ViewHolder {
        public ImageView imageView;
        public ImageView videoControls;
        public TextView recentLabel;

        public ViewHolder(View v) {
            imageView = (ImageView) v.findViewById(R.id.image_view);
            videoControls = (ImageView) v.findViewById(R.id.video_controls);
            recentLabel = (TextView) v.findViewById(R.id.recent_label);
        }
    }
}
