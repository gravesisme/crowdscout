package com.dpg.crowdscout.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.dpg.crowdscout.R;
import com.dpg.crowdscout.models.LocationModel;
import com.dpg.crowdscout.utils.ImageDownloader;
import com.google.common.base.Strings;

import java.util.List;

/**
 * Adapter used by the BrowseLocationsView to render LocationModel items
 */
public class BrowseLocationsAdapter extends ArrayAdapter<LocationModel> {
    private final LayoutInflater m_layoutInflater;
    private final List<LocationModel> m_items;
    private final ImageDownloader m_imageDownloader;
    private final Drawable m_noPhotosPlaceholder;

    private boolean m_showCategoryLabel = true;

    private int m_itemHeight = 0;
    private int m_numColumns = 0;

    private GridView.LayoutParams m_layoutParams;

    public BrowseLocationsAdapter(final Context context, final List<LocationModel> locations) {
        super(context, 0, locations);
        m_layoutInflater = LayoutInflater.from(context);
        m_items = locations;
        m_layoutParams = new GridView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        // Get the placeholder bitmap that will be used while remote images load
        final Bitmap placeholderImage = BitmapFactory.decodeResource(context.getResources(), R.drawable.location_image_placeholder);

        // A second placeholder used for when a location has no image to load
        m_noPhotosPlaceholder = context.getResources().getDrawable(R.drawable.no_photo_placeholder);

        // Initialize image downloader
        m_imageDownloader = new ImageDownloader(context, placeholderImage);

        // Enable fade to add visual cue for when image is not ready right away. This is necessary because the image
        // url is only available after fetching place details from Places API, which almost doubles time to load
        m_imageDownloader.enableFadeIn();

        // Execute downloads using global thread pool
        m_imageDownloader.setExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void setImageCache(ImageDownloader.ImageCache imageCache) {
        m_imageDownloader.setImageCache(imageCache);
    }

    /**
     * Use item position as ID
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Get # of items in the grid
     */
    @Override
    public int getCount() {
        return (m_items.size());
    }

    /**
     * Get LocationModel at position X
     */
    @Override
    public LocationModel getItem(int position) {
        return (m_items.get(position));
    }

    /**
     * Gets view
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view;
        final ViewHolder viewHolder;
        final LocationModel item = m_items.get(position);

        if (convertView == null) {
            view = m_layoutInflater.inflate(R.layout.browse_locations_list_item, null);
            view.setLayoutParams(m_layoutParams);
            viewHolder = new ViewHolder(view);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }

        // Check the height matches our calculated column width
        if (view.getLayoutParams().height != m_itemHeight) {
            view.setLayoutParams(m_layoutParams);
        }

        // Set name
        viewHolder.nameLabel.setText(item.getName());

        // Set category
        viewHolder.categoryLabel.setText(item.getCategory().toString().toUpperCase());

        // Set category icon
        switch (item.getCategory()) {
            case food:
                viewHolder.categoryLabel.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_food, 0, 0, 0);
                break;
            case drinks:
                viewHolder.categoryLabel.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_drinks, 0, 0, 0);
                break;
            case casino:
                viewHolder.categoryLabel.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_casino, 0, 0, 0);
                break;
            case outdoors:
                viewHolder.categoryLabel.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_beach, 0, 0, 0);
                break;
            default:
                // TODO: Add icons for the remaining categories
                viewHolder.categoryLabel.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }

        // Hide category label if specified
        if (!m_showCategoryLabel) {
            viewHolder.categoryLabel.setVisibility(View.INVISIBLE);
        }

        // Get the current location's cover photo
        String imageUrl = item.getImageUrl();

        // If the current location has a cover photo, load the image
        if (!Strings.isNullOrEmpty(imageUrl)) {
            m_imageDownloader.loadImageInto(imageUrl, viewHolder.imageView);
        } else {
            // Otherwise, show the no photo placeholder
            m_imageDownloader.cancelDownloadAndShowDrawable(viewHolder.imageView, m_noPhotosPlaceholder);
        }

        return view;
    }

    public int getNumColumns() {
        return m_numColumns;
    }

    /**
     * Sets the item height. Useful for when we know the column width so the height can be set
     * to match.
     * <p/>
     * Note: Possible improvement would be to have ImageDownloader crop images
     * to this size after displaying them. This would ensure images do not have to be scaled
     * when they are placed into a grid view
     *
     * @param height
     */
    public void setItemHeight(int height) {
        if (height == m_itemHeight) {
            return;
        }

        m_itemHeight = height;
        m_layoutParams = new GridView.LayoutParams(LayoutParams.MATCH_PARENT, m_itemHeight);

        // Notify image downloader what the target dimensions for images should be
        if (m_imageDownloader != null) {
            // Since all images displayed in this list are squares, item height can be used for both dimensions
            m_imageDownloader.setTargetDimension(new ImageDownloader.BitmapDimension(m_itemHeight, m_itemHeight));
        }

        // If items were already added, notify the adapter of the size change
        if (!m_items.isEmpty()) {
            notifyDataSetChanged();
        }
    }

    public void setNumColumns(int numColumns) {
        m_numColumns = numColumns;
    }

    public void hideCategoryLabel() {
        m_showCategoryLabel = false;
    }

    public void showCategoryLabel() {
        m_showCategoryLabel = true;
    }

    /**
     * ViewHolder class used to ensure view components will only have to be initialized once per
     * available cell
     */
    private static class ViewHolder {
        public TextView nameLabel;
        public TextView categoryLabel;
        public ImageView imageView;

        public ViewHolder(View v) {
            // Initialize view components
            nameLabel = (TextView) v.findViewById(R.id.location_name);
            categoryLabel = (TextView) v.findViewById(R.id.category_label);
            imageView = (ImageView) v.findViewById(R.id.location_image);
        }
    }
}
