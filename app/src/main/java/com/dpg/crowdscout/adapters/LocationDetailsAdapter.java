package com.dpg.crowdscout.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.crowdscout.api.models.foursquare.ConcreteImage;
import com.crowdscout.api.models.foursquare.FoursquareVenue;
import com.dpg.crowdscout.BuildConfig;
import com.dpg.crowdscout.R;
import com.dpg.crowdscout.models.VenueWrapper;
import com.dpg.crowdscout.utils.Helpers;
import com.dpg.crowdscout.utils.ImageDownloader;
import com.dpg.crowdscout.widgets.TextIconDrawable;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

import java.util.Set;

/**
 * Adapter used to render FoursquareVenue models in the LocationDetailsView's ListView
 */
public class LocationDetailsAdapter extends BaseAdapter {
    private static final String LOG_TAG = LocationDetailsAdapter.class.getSimpleName();
    private static boolean DEBUG = BuildConfig.DEBUG && false;

    private final Context m_context;
    private final Resources m_resources;
    private final LayoutInflater m_layoutInflater;
    private final ImageDownloader m_imageDownloader;

    private final ColorStateList m_addNoteColorStateList;
    private final Bitmap m_noteBitmap;
    private final Bitmap m_noPhotosBitmap;
    private final Bitmap m_loadingPlaceholder;
    private final Bitmap m_categoryIconPlaceholder;
    private final Bitmap m_errorPlaceholderImage;
    private final Bitmap m_oomPlaceholderImage;
    private final Drawable m_noPhotosPlaceholder;

    private final String m_addNoteString;

    private final int m_noteBgColor;
    private final int m_selectedIconColor;
    private final int m_emptyDescriptionTextColor;
    private final int m_thumbnailImageMaxHeight;
    private final int m_paddingSmallHorizontal;
    private final int m_descriptionLabelTextColor;
    private final int m_descriptionLabelDisabledTextColor;

    private final ImageDownloader.BitmapDimension m_targetDimension;
    private final ImageDownloader.BitmapDimension m_categoryIconDimension;

    /**
     * Stores all of the items in the adapter
     */
    private final Set<FoursquareVenue> m_items;

    /**
     * Touch listener used to apply a different color to provided image view when touched
     */
    private final View.OnTouchListener m_highlightOnTouch = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (v instanceof ImageView) {
                final ImageView imageView = (ImageView) v;

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    imageView.setColorFilter(new PorterDuffColorFilter(m_selectedIconColor, PorterDuff.Mode.MULTIPLY));
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    imageView.setColorFilter(new PorterDuffColorFilter(m_emptyDescriptionTextColor, PorterDuff.Mode.MULTIPLY));
                }
            }

            return false;
        }
    };

    /**
     * Flag used to configure whether edit note functionality is enabled
     */
    private boolean m_isEditNoteEnabled = false;

    /**
     * Click listener for personalizing a venue's description
     * <p/>
     * Currently not implemented
     */
    protected View.OnClickListener m_onEditPlaceNoteClickListener;

    /**
     * Flag used to configure whether save/unsave venue functionality is enabled
     */
    private boolean m_isSavingEnabled = false;

    /**
     * Click listener for save/unsave venue operations
     * <p/>
     * Currently not implemented
     */
    private View.OnClickListener m_onSaveVenueClickListener;

    // *******************************************************
    // CONSTRUCTOR
    // *******************************************************

    public LocationDetailsAdapter(final Context context, final Set<FoursquareVenue> items) {
        super();
        m_context = context;
        m_items = items;
        m_layoutInflater = LayoutInflater.from(context);
        m_resources = context.getResources();

        // Calculate the thumbnail image's max dimension so that ImageCache can be optimized
        m_thumbnailImageMaxHeight = measureMaxImageHeight();

        // Create target dimension based on max dimension possible for thumbnail image
        final int thumbnailImageWidth = m_resources.getDimensionPixelOffset(R.dimen.venue_thumbnail_image_width);
        m_targetDimension = new ImageDownloader.BitmapDimension(thumbnailImageWidth, m_thumbnailImageMaxHeight);

        // Create category icon target dimension
        final int categoryIconSize = m_resources.getDimensionPixelSize(R.dimen.venue_category_icon_size);
        m_categoryIconDimension = new ImageDownloader.BitmapDimension(categoryIconSize, categoryIconSize);

        // Initialize all of the required view resources
        m_selectedIconColor = m_resources.getColor(R.color.blue);
        m_emptyDescriptionTextColor = m_resources.getColor(R.color.location_button_accent);
        m_descriptionLabelTextColor = m_descriptionLabelDisabledTextColor = m_resources.getColor(R.color.grey3);
        m_paddingSmallHorizontal = m_resources.getDimensionPixelSize(R.dimen.contentPaddingSmallHorizontal);
        m_addNoteString = m_resources.getString(R.string.add_a_note);
        m_addNoteColorStateList = m_resources.getColorStateList(R.color.bt_light_blue_text_states);

        // Get the placeholder bitmap that will be used while remote images load
        final Bitmap placeholderImage = BitmapFactory.decodeResource(m_resources,
                R.drawable.location_image_placeholder);

        // Get the placeholder bitmap that will be used while remote category icon images load
        m_categoryIconPlaceholder = BitmapFactory.decodeResource(m_resources,
                R.drawable.ic_marker);

        // A second placeholder used for when a location has no image to load
        m_noPhotosPlaceholder = m_resources.getDrawable(R.drawable.no_photo_placeholder);
        m_noPhotosBitmap = TextIconDrawable.createBitmap(context, m_noPhotosPlaceholder,
                m_resources.getColor(R.color.grey3),
                m_resources.getString(R.string.no_photos), thumbnailImageWidth, thumbnailImageWidth);

        // Initialize image downloader
        m_imageDownloader = new ImageDownloader(context, placeholderImage);

        // Use a sample size of 2 to save memory
        m_imageDownloader.setBitmapSampleSize(2);

        // Enable fade to add visual cue for when image is not ready right away. This is necessary because the image
        // url is only available after fetching place details from Places API, which almost doubles time to load
        m_imageDownloader.enableFadeIn();

        // Execute downloads using global thread pool
        m_imageDownloader.setExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        // Get bitmap used to decorate container for items that have a description
        m_noteBitmap = BitmapFactory.decodeResource(m_resources,
                R.drawable.quotation_marks);

        // Get color to tint note bitmap with
        m_noteBgColor = m_resources.getColor(R.color.grey1);

        // Get the placeholder bitmap that will be used while remote images load
        int loadingBgColor = m_resources.getColor(R.color.grey4);
        m_loadingPlaceholder = TextIconDrawable.createBitmap(context,
                m_resources.getDrawable(R.drawable.empty_placeholder),
                loadingBgColor, "", thumbnailImageWidth, thumbnailImageWidth);

        // Create bitmaps that will be shown when errors or out-of-memory exceptions are encountered while downloading images
        if (DEBUG) {
            // Developers should see error codes (404 = server-error, 500 = out-of-memory)
            m_errorPlaceholderImage = TextIconDrawable.createBitmap(context, "404",
                    thumbnailImageWidth, thumbnailImageWidth);
            m_oomPlaceholderImage = TextIconDrawable.createBitmap(context, "500",
                    thumbnailImageWidth, thumbnailImageWidth);
        } else {
            // User should only see gray images for both error messages
            int errorBgColor = m_resources.getColor(R.color.grey2);
            m_errorPlaceholderImage = m_oomPlaceholderImage =
                    TextIconDrawable.createBitmap(context, m_noPhotosPlaceholder, errorBgColor,
                            "", thumbnailImageWidth, thumbnailImageWidth);
        }
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
    public FoursquareVenue getItem(int position) {
        return Iterables.get(m_items, position);
    }

    /**
     * Get item Id
     */
    @Override
    public long getItemId(int position) {
        final FoursquareVenue item = getItem(position);
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

    /**
     * Set click listener responsible for handling saving (and unsaving) venues
     *
     * @param listener {@link android.view.View.OnClickListener}
     */
    public void setOnSaveVenueClickListener(View.OnClickListener listener) {
        m_onSaveVenueClickListener = listener;
    }

    public void setImageCache(ImageDownloader.ImageCache imageCache) {
        m_imageDownloader.setImageCache(imageCache);
    }

    public void setFadeInEnabled(boolean enabled) {
        if (enabled) {
            final boolean doNotifyDataSetChanged = m_imageDownloader.isFadeInEnabled();
            m_imageDownloader.enableFadeIn();
            if (doNotifyDataSetChanged) {
                notifyDataSetChanged();
            }
        } else {
            m_imageDownloader.disableFadeIn();
        }
    }

    public void enableDownloading() {
        final boolean doNotifyDataSetChanged = !m_imageDownloader.isDownloadingEnabled();
        m_imageDownloader.enableDownloading();
        // If downloading was previously disabled, notify the adapter that data needs to be re-rendered
        if (doNotifyDataSetChanged) {
            notifyDataSetChanged();
        }
    }

    public void disableDownloading() {
        m_imageDownloader.disableDownloading();
    }

    public boolean isFadeInEnabled() {
        return m_imageDownloader.isFadeInEnabled();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view;
        final ViewHolder viewHolder;
        final FoursquareVenue venue = getItem(position);
        final VenueWrapper wrappedVenue = new VenueWrapper(venue);
        final String mostRecentTip = wrappedVenue.getTip();

        // Store whether the item has a description or not
        final boolean hasDescription = !Strings.isNullOrEmpty(mostRecentTip);

        // Determine if the layout needs to be inflated
        boolean needsInflate = convertView == null;

        // If the view has been previously created, ensure that the previous height matches up with the current and that the holder is not dirty
        if (!needsInflate) {
            ViewHolder holder = ((ViewHolder) convertView.getTag());
            needsInflate = (holder == null || holder.needsInflate);
        }

        // Inflate the layout and create a new viw holder if specified
        if (needsInflate) {
            view = m_layoutInflater.inflate(R.layout.location_details_list_item, null);
            viewHolder = new ViewHolder(view);

            // When enabled (not yet), allows user to personalize venue descriptions for themself
            if (m_isEditNoteEnabled) {
                viewHolder.descriptionLabel.setOnClickListener(m_onEditPlaceNoteClickListener);
            }

            /**
             * When enabled (not yet), allows user to save/unsave venues that appear in list for
             * easy retrieval in the future via a fragment that will populate this adapter with
             * only saved venues
             */
            if (m_isSavingEnabled) {
                // Add photo click handler should only be enabled in NORMAL mode
                viewHolder.saveTextButton.setOnClickListener(m_onSaveVenueClickListener);
            }

            // Store viewHolder in view
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }

        // Store the placesID of the place being rendered
        viewHolder.venueId = venue.id;

        // Add position to description label if edit note is enabled
        if (m_isEditNoteEnabled) {
            viewHolder.descriptionLabel.setTag(R.id.location_details_list_item_key_position, position);
        }

        // If saving is enabled, update save button text, visibility, and add current position
        if (m_isSavingEnabled) {
            viewHolder.saveTextButton.setTag(R.id.location_details_list_item_key_position, position);
            viewHolder.saveTextButton.setVisibility(View.VISIBLE);
            // Update button text based on whether active venue is saved
            viewHolder.saveTextButton.setText(Helpers.INSTANCE.getString(wrappedVenue.isSaved() ?
                    R.string.unsave : R.string.save));
        } else {
            // Otherwise, just hide the button
            viewHolder.saveTextButton.setVisibility(View.GONE);
        }

        // Set name
        viewHolder.nameLabel.setText(venue.name);

        // Toggle container bg icon based on whether the current item has a description
        if (hasDescription) {
            setListItemContainerBg(viewHolder.container);
        } else {
            Helpers.INSTANCE.setBackground(viewHolder.container, null);
        }

        // Figure out if the place note should be shown
        boolean showDescription = hasDescription || m_isEditNoteEnabled;
        viewHolder.descriptionLabel.setVisibility(showDescription ? View.VISIBLE : View.GONE);

        // Add styles to description when it is visible
        if (showDescription) {
            boolean showBlueOutline = hasDescription && m_isEditNoteEnabled;
            boolean showAddNoteHint = !hasDescription && m_isEditNoteEnabled;

            // Adjust left padding based on whether blue outline is visible
            Helpers.INSTANCE.setPadding(viewHolder.descriptionLabel, showBlueOutline ? m_paddingSmallHorizontal : 0, Helpers.Direction.LEFT);

            // Add - or remove - blue outline bg if edit mode is active and item has a description
            viewHolder.descriptionLabel.setBackgroundResource(showBlueOutline ? R.drawable.blue_outline_bg : 0);

            // Show 'Add a note' hint if edit mode is active and editNote is enabled
            if (showAddNoteHint) {
                viewHolder.descriptionLabel.setText(m_addNoteString);
                viewHolder.descriptionLabel.setTextColor(m_addNoteColorStateList);
            } else {
                // Otherwise, just show the item's actual description
                viewHolder.descriptionLabel.setText(mostRecentTip);

                // Use the disabled color if edit mode is active and editNote is disabled
                viewHolder.descriptionLabel.setTextColor(!m_isEditNoteEnabled
                        ? m_descriptionLabelDisabledTextColor : m_descriptionLabelTextColor);
            }
        }

        // Set category name
        if (wrappedVenue.hasCategory()) {
            viewHolder.categoryLabel.setVisibility(View.VISIBLE);
            viewHolder.categoryLabel.setText(wrappedVenue.getCategoryName());
        } else {
            // Otherwise, just hide the category icon and label
            viewHolder.categoryLabel.setVisibility(View.GONE);
        }

        // Load category icon
        if (wrappedVenue.hasCategoryIcon()) {
            viewHolder.categoryIcon.setVisibility(View.VISIBLE);
            m_imageDownloader.loadImageInto(wrappedVenue.getCategoryIconUrl(), viewHolder.categoryIcon, m_categoryIconPlaceholder, m_categoryIconDimension);
        } else {
            viewHolder.categoryIcon.setVisibility(View.GONE);

            // If there is no category icon, cancel any potential downloads from a previous getView call
            // NOTE: loadImageInto() automatically performs this operation
            m_imageDownloader.cancelDownload(viewHolder.categoryIcon);

            // Make sure any previously set category icon is removed
            viewHolder.categoryIcon.setImageDrawable(null);
        }

        // Load venue image
        if (wrappedVenue.hasPhoto()) {
            final String imageUrl = wrappedVenue.getPhoto(ConcreteImage.ImageSize.MEDIUM);
            m_imageDownloader.loadImageInto(imageUrl, viewHolder.thumbnailImage, m_loadingPlaceholder,
                    m_targetDimension, m_oomPlaceholderImage, m_errorPlaceholderImage);
        } else {
            // If the venue does not have a photo, make sure there are no other active
            // downloads taking place and set proper placeholder to replace previous value
            m_imageDownloader.cancelDownload(viewHolder.thumbnailImage, m_noPhotosBitmap);
        }

        return view;
    }

    /**
     * Adds the hasDescription drawable to the provided container's background
     *
     * @param container {@link RelativeLayout} The container to add the drawable to
     */
    private void setListItemContainerBg(@NonNull RelativeLayout container) {
        // Create drawable used whenever an item contains a description
        final BitmapDrawable hasDescriptionBgDrawable = new BitmapDrawable(m_resources, m_noteBitmap);
        hasDescriptionBgDrawable.setBounds(0, 0, m_noteBitmap.getWidth(), m_noteBitmap.getHeight());
        hasDescriptionBgDrawable.setColorFilter(new PorterDuffColorFilter(m_noteBgColor, PorterDuff.Mode.MULTIPLY));
        hasDescriptionBgDrawable.setGravity(Gravity.BOTTOM | Gravity.LEFT);
        Helpers.INSTANCE.setBackground(container, hasDescriptionBgDrawable);
    }

    /**
     * Helper used for determining max dimensions that should be cached by ImageDownloader for each
     * image after it has been downloaded
     *
     * @return {@link int} Max height of thumbnail image in pixels
     */
    private int measureMaxImageHeight() {
        View view = m_layoutInflater.inflate(R.layout.location_details_list_item, null);

        ViewHolder viewHolder = new ViewHolder(view);
        viewHolder.nameLabel.setText("Line 1\nLine 2");
        viewHolder.descriptionLabel.setText("Line 1\nLine 2\nLine 3\nLine 4");
        viewHolder.descriptionLabel.setVisibility(View.VISIBLE);

        int maxWidthPixels = m_resources.getDisplayMetrics().widthPixels;

        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(maxWidthPixels, View.MeasureSpec.EXACTLY);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(ViewGroup.LayoutParams.WRAP_CONTENT, View.MeasureSpec.UNSPECIFIED);

        view.measure(widthMeasureSpec, heightMeasureSpec);

        view.requestLayout();

        int thumbnailImageHeight = viewHolder.thumbnailImage.getMeasuredHeight();

        return thumbnailImageHeight;
    }

    /**
     * ViewHolder class used to ensure view components will only have to be initialized once per
     * available cell
     */
    public static class ViewHolder {
        public RelativeLayout container;

        public ImageView thumbnailImage;
        public ImageView categoryIcon;

        public TextView nameLabel;
        public TextView categoryLabel;
        public TextView descriptionLabel;
        public TextView saveTextButton;

        // Used to store the venue's ID so when fetch resolves, the fetched place can be checked against view holder
        public String venueId;

        // Boolean used whenever removing a place from a list that will trigger a re-inflate next time that row is used
        public boolean needsInflate = false;

        public ViewHolder(View v) {
            // Initialize view components
            container = (RelativeLayout) v.findViewById(R.id.location_details_container);
            thumbnailImage = (ImageView) v.findViewById(R.id.place_thumbnail_image);
            categoryIcon = (ImageView) v.findViewById(R.id.place_category_icon);
            nameLabel = (TextView) v.findViewById(R.id.place_name);
            categoryLabel = (TextView) v.findViewById(R.id.place_category);
            descriptionLabel = (TextView) v.findViewById(R.id.place_description);
            saveTextButton = (TextView) v.findViewById(R.id.save_venue_text_button);
        }
    }
}
