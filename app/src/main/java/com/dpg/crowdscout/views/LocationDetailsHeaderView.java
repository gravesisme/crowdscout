package com.dpg.crowdscout.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.dpg.crowdscout.BuildConfig;
import com.dpg.crowdscout.R;
import com.dpg.crowdscout.models.LocationModel;
import com.dpg.crowdscout.utils.ImageCache;
import com.dpg.crowdscout.widgets.TitleView;
import com.google.common.base.Strings;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Header used by ListView inside LocationDetailsView
 */
public class LocationDetailsHeaderView extends FrameLayout {
    private static final String LOG_TAG = LocationDetailsHeaderView.class.getSimpleName();
    private static final boolean DEBUG = BuildConfig.DEBUG && false;

    /**
     * Flag that should only be touched by unit tests to disable image downloading
     */
    static boolean DOWNLOADING_ENABLED = true;

    /**
     * Placeholder image applied to cover photo before image is downloaded
     */
    private final Bitmap m_placeholderImage;

    /**
     * View UI Components
     */
    private final View m_coverPhotoGradient;
    private final ImageView m_coverPhotoImageView;
    private final TextView m_nameLabel;
    private final TextView m_categoryLabel;
    private final TextView m_descriptionLabel;
    private final TitleView m_numItemsLabel;
    private final TextView m_emptyView;

    // *******************************************************
    // Factory Method
    // *******************************************************

    public static LocationDetailsHeaderView newInstance(@NonNull Context context, @NonNull LocationModel locationModel) {
        final LocationDetailsHeaderView headerView = new LocationDetailsHeaderView(context);
        headerView.decorate(locationModel);
        return headerView;
    }

    // *******************************************************
    // Constructors
    // *******************************************************

    public LocationDetailsHeaderView(Context context) {
        this(context, null, 0);
    }

    public LocationDetailsHeaderView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LocationDetailsHeaderView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // Inflate view
        final LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.location_details_list_header, this);

        // Set view references
        m_coverPhotoGradient = findViewById(R.id.cover_photo_gradient);
        m_coverPhotoImageView = (ImageView) findViewById(R.id.cover_photo);
        m_nameLabel = (TextView) findViewById(R.id.location_name_label);
        m_categoryLabel = (TextView) findViewById(R.id.location_category_label);
        m_descriptionLabel = (TextView) findViewById(R.id.location_description_label);
        m_numItemsLabel = (TitleView) findViewById(R.id.num_items_label);
        m_emptyView = (TextView) findViewById(R.id.empty_view_hint_label);

        // Get the placeholder bitmap that will be used while remote images load
        m_placeholderImage = BitmapFactory.decodeResource(getResources(), R.drawable.location_image_placeholder);
    }

    // *******************************************************
    // SETTERS
    // *******************************************************

    public void decorate(@NonNull LocationModel locationModel) {
        setNameLabelText(locationModel.getName());
        setDescriptionLabelText(locationModel.getDescription());
        updateCategoryLabel(locationModel.getCategory());
        updateCoverPhoto(locationModel.getImageUrl());
    }

    void setNameLabelText(@NonNull String name) {
        if (m_nameLabel != null) {
            m_nameLabel.setText(name);
        }
    }

    void setDescriptionLabelText(@NonNull String description) {
        if (m_descriptionLabel != null) {
            m_descriptionLabel.setText(description);
        }
    }

    void setCategoryLabelText(@NonNull String category) {
        if (m_categoryLabel != null) {
            m_categoryLabel.setText(category);
        }
    }

    void updateCategoryLabel(@NonNull LocationModel.VenueFilter category) {
        if (m_categoryLabel != null) {
            // Set label text
            setCategoryLabelText(category.toString());

            // Set category icon
            switch (category) {
                case food:
                    m_categoryLabel.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_food, 0, 0, 0);
                    break;
                case drinks:
                    m_categoryLabel.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_drinks, 0, 0, 0);
                    break;
                case casino:
                    m_categoryLabel.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_casino, 0, 0, 0);
                    break;
                case outdoors:
                    m_categoryLabel.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_beach, 0, 0, 0);
                    break;
                default:
                    // TODO: Add icons for the remaining categories
                    m_categoryLabel.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
        }
    }

    void updateCoverPhoto(final String imageUrl) {
        // Make sure downloading is enabled and that both model and image view exist
        if (!DOWNLOADING_ENABLED || m_coverPhotoImageView == null) {
            return;
        }

        // If the current collection has no cover photo, show the no photos placeholder
        if (Strings.isNullOrEmpty(imageUrl)) {
            m_coverPhotoImageView.setImageResource(R.drawable.no_photo_placeholder);
            return;
        }

        // Check to see if a cover photo has already been downloaded
        String prevImageUrl = (String) m_coverPhotoImageView.getTag();

        // If the previously downloaded cover photo is the same as the active collection, don't download it again
        if (prevImageUrl != null && prevImageUrl.equals(imageUrl)) {
            if (DEBUG) {
                Log.d(LOG_TAG, "updateCoverPhoto(): Previous image same as current image. Skipping download.");
            }
            return;
        }

        // Set placeholder
        m_coverPhotoImageView.setImageBitmap(m_placeholderImage);

        // Make sure there is something to download
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // Download image and update image view when finished
            try {
                new ImageCache.DownloadImageTask() {
                    @Override
                    protected void onPostExecute(BitmapDrawable bitmapDrawable) {
                        // Store the downloaded image url in the image view tag
                        m_coverPhotoImageView.setTag(imageUrl);

                        // Update the image
                        m_coverPhotoImageView.setImageDrawable(bitmapDrawable);
                    }
                }.execute(new URL(imageUrl));
            } catch (MalformedURLException e) {
                if (DEBUG) {
                    Log.e(LOG_TAG, "updateCoverPhoto(): Exception in downloading image " + e.toString(), e);
                }
            }
        }
    }

    public void updateNumItemsLabel(int numItems) {
        if (m_numItemsLabel != null) {
            String s;

            if (numItems == 0) {
                s = "";

                // Show empty view
                showEmptyView();
            } else {
                // Hide empty view
                hideEmptyView();

                // TODO: Localize this string
                s = (numItems == 1 ? "1 place" : String.format("%d places", numItems));
            }

            // Update label text
            m_numItemsLabel.setText(s);

            // Toggle numItems label visibility
            m_numItemsLabel.setVisibility(numItems > 0 ? VISIBLE : GONE);
        }
    }

    void showEmptyView() {
        if (m_emptyView != null) {
            m_emptyView.setVisibility(VISIBLE);
        }
    }

    void hideEmptyView() {
        if (m_emptyView != null) {
            m_emptyView.setVisibility(GONE);
        }
    }
}
