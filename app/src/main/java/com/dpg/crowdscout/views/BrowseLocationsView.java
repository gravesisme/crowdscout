package com.dpg.crowdscout.views;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dpg.crowdscout.R;
import com.dpg.crowdscout.adapters.BrowseLocationsAdapter;
import com.dpg.crowdscout.utils.SimpleCallback;

/**
 * View used by the BrowseLocationsFragment
 */
public class BrowseLocationsView extends FrameLayout {
    private final int m_gridViewMarginTopUnderFilterContainer;
    private final int m_gridViewMarginTopUnderLabel;

    private GridView m_gridView;
    private TextView m_label;
    private TextView m_createNewLocationButton;
    private TextView m_createNewLocationButtonOnEmptyScreen;
    private TextView m_sortOrderButton;
    private LinearLayout m_filterContainer;
    private View m_emptyView;
    private ViewTreeObserver.OnGlobalLayoutListener m_gridViewLayoutListener;
    private SimpleCallback m_onLayoutChangedCallback;

    // *******************************************************
    // Factory Method
    // *******************************************************

    public static BrowseLocationsView newInstance(@NonNull Context context) {
        final BrowseLocationsView view = new BrowseLocationsView(context);
        return view;
    }

    // *******************************************************
    // CONSTRUCTORS
    // *******************************************************

    public BrowseLocationsView(Context context) {
        this(context, null, 0);
    }

    public BrowseLocationsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BrowseLocationsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // Inflate view
        final LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.browse_locations_view, this);

        // Set view references
        m_gridView = (GridView) findViewById(R.id.browse_locations_grid);
        m_label = (TextView) findViewById(R.id.locations_label);
        m_createNewLocationButton = (TextView) findViewById(R.id.create_new_location_button);
        m_createNewLocationButtonOnEmptyScreen = (TextView) findViewById(R.id.create_new_location_text_button);
        m_sortOrderButton = (TextView) findViewById(R.id.order_by_button);
        m_filterContainer = (LinearLayout) findViewById(R.id.filter_container);
        m_emptyView = findViewById(R.id.browse_locations_grid_empty);

        // Get resources that will be referenced throughout class
        final Resources resources = getResources();
        m_gridViewMarginTopUnderFilterContainer = resources.getDimensionPixelSize(R.dimen.browse_locations_grid_margin_top_under_filter_container);
        m_gridViewMarginTopUnderLabel = resources.getDimensionPixelSize(R.dimen.browse_locations_grid_margin_top_under_label);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        /**
         * Trigger OnLayoutChanged SimpleCallback if layout has changed and
         * a callback has been assigned
         */
        if (changed && m_onLayoutChangedCallback != null) {
            m_onLayoutChangedCallback.onComplete();
        }
    }

    /**
     * Show empty view
     */
    public void showEmptyView() {
        if (m_emptyView != null) {

            // Show empty view
            m_emptyView.setVisibility(VISIBLE);
        }
    }

    /**
     * Hide empty view
     */
    public void hideEmptyView() {
        if (m_emptyView != null) {
            m_emptyView.setVisibility(GONE);
        }
    }

    /**
     * Sets the adapter that should be used to populate the GridView
     *
     * @param adapter {@link com.dpg.crowdscout.adapters.BrowseLocationsAdapter}
     */
    public void setAdapter(BrowseLocationsAdapter adapter) {
        if (m_gridView != null) {
            m_gridView.setAdapter(adapter);
        }
    }

    /**
     * Sets listener that should be triggered when user clicks "create new" button
     *
     * @param clickListener {@link android.view.View.OnClickListener}
     */
    public void setOnCreateLocationClickListener(OnClickListener clickListener) {
        if (m_createNewLocationButton != null) {
            m_createNewLocationButton.setOnClickListener(clickListener);
        }
        if (m_createNewLocationButtonOnEmptyScreen != null) {
            m_createNewLocationButtonOnEmptyScreen.setOnClickListener(clickListener);
        }
    }

    /**
     * Sets listener that should be triggered when user clicks sort button
     *
     * @param clickListener {@link android.view.View.OnClickListener}
     */
    public void setSortOrderClickListener(OnClickListener clickListener) {
        if (m_sortOrderButton != null) {
            m_sortOrderButton.setOnClickListener(clickListener);
        }
    }

    /**
     * Decorates the sort button based on whether it is in ASCENDING or DESCENDING mode, which
     * should be triggered by the parent class
     *
     * @param asc {@link boolean} Whether the button should appear as ASCENDING or DESCENDING
     */
    public void toggleSortButtonState(boolean asc) {
        if (asc) {
            m_sortOrderButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.bt_arrow_down_image_states, 0);
            m_sortOrderButton.setText(getResources().getString(R.string.a_to_z));
        } else {
            m_sortOrderButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.bt_arrow_up_image_states, 0);
            m_sortOrderButton.setText(getResources().getString(R.string.z_to_a));
        }
    }

    /**
     * Triggers a smooth scroll to the top of the list
     */
    public void scrollToTop() {
        if (m_gridView != null) {
            m_gridView.smoothScrollToPosition(0);
        }
    }

    /**
     * Helper for hiding this view
     */
    public void hide() {
        this.setVisibility(GONE);
    }

    /**
     * Helper for showing this view
     */
    public void show() {
        this.setVisibility(VISIBLE);
    }

    /**
     * Helper for hiding the filter container available to this view
     */
    public void hideFilterContainer() {
        if (m_filterContainer != null) {
            m_filterContainer.setVisibility(GONE);
            moveGridViewBelowLabel();
        }
    }

    /**
     * Helper for showing the filter container available to this view
     */
    public void showFilterContainer() {
        if (m_filterContainer != null) {
            m_filterContainer.setVisibility(VISIBLE);
            moveGridViewBelowFilterContainer();
        }
    }

    /**
     * Helper for determining if the filter container is currently visible
     */
    public boolean isFilterContainerVisible() {
        return m_filterContainer == null ? false : (m_filterContainer.getVisibility() == VISIBLE);
    }

    /**
     * Helper for hiding the sort button available to this view
     */
    public void hideSortOrderButton() {
        if (m_sortOrderButton != null) {
            m_sortOrderButton.setVisibility(GONE);
        }
    }

    /**
     * Helper for showing the sort button available to this view
     */
    public void showSortOrderButton() {
        if (m_sortOrderButton != null) {
            m_sortOrderButton.setVisibility(VISIBLE);
        }
    }

    /**
     * Helper for hiding the create button available to this view
     */
    public void hideCreateButton() {
        if (m_createNewLocationButton != null) {
            m_createNewLocationButton.setVisibility(GONE);
        }

        if (m_createNewLocationButtonOnEmptyScreen != null) {
            m_createNewLocationButtonOnEmptyScreen.setVisibility(GONE);
        }
    }

    /**
     * Helper for showing the create button available to this view
     */
    public void showCreateButton() {
        if (m_createNewLocationButton != null) {
            m_createNewLocationButton.setVisibility(VISIBLE);
        }
        if (m_createNewLocationButtonOnEmptyScreen != null)
            m_createNewLocationButtonOnEmptyScreen.setVisibility(VISIBLE);
    }

    /**
     * Helper that can change the primary label of this view
     *
     * @param text {@link String} Desired text for header label
     */
    public void setLabelText(String text) {
        if (m_label != null) {
            m_label.setText(text);
        }
    }

    /**
     * Helper that can change the gravity of the primary label of this view
     *
     * @param gravity {@link int} Desired gravity for header label
     */
    public void setLabelGravity(int gravity) {
        if (m_label != null) {
            m_label.setGravity(gravity);
        }
    }

    /**
     * Helper that can change the left margin of the primary label of this view
     *
     * @param leftMargin {@link int} Desired left margin for header label
     */
    public void setLabelLeftMargin(int leftMargin) {
        if (m_label != null) {
            ((RelativeLayout.LayoutParams) m_label.getLayoutParams()).leftMargin = leftMargin;
        }
    }

    /**
     * Helper that can change the top margin of the primary label of this view
     *
     * @param topMargin {@link int} Desired top margin for header label
     */
    public void setGridViewMarginTop(int topMargin) {
        ((RelativeLayout.LayoutParams) m_gridView.getLayoutParams()).topMargin = topMargin;
    }

    /**
     * Helper that can move the grid view below the primary label. By default, the grid view
     * appears below the filter container, so this would be useful if the implementer does
     * not wish to show the filter container.
     */
    public void moveGridViewBelowLabel() {
        moveGridViewBelow(R.id.locations_label, m_gridViewMarginTopUnderLabel);
    }

    /**
     * Helper that can move the grid view below the filter container, which is the default position
     */
    public void moveGridViewBelowFilterContainer() {
        moveGridViewBelow(R.id.filter_container, m_gridViewMarginTopUnderFilterContainer);
    }

    /**
     * Internal helper that can move the grid view BELOW a specific anchor point with a specific
     * margin
     *
     * @param anchor    {@link int} View the grid view should be moved below
     * @param marginTop {@link int} Desired top margin for grid view
     */
    private void moveGridViewBelow(int anchor, int marginTop) {
        moveView(m_gridView, RelativeLayout.BELOW, anchor, marginTop);
    }

    /**
     * Internal helper that can move a designated view using a verb, an anchor point, and a specific
     * margin
     *
     * @param view      {@link View} Desired view that should be moved
     * @param verb      {@link int} RelativeLayout verb used for positioning a view (e.g. BELOW, ABOVE)
     * @param anchor    {@link int} View the grid view should be moved below
     * @param marginTop {@link int} Desired top margin for view
     */
    private void moveView(View view, int verb, int anchor, int marginTop) {
        final RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) view.getLayoutParams();
        params.addRule(verb, anchor);
        params.topMargin = marginTop;
        view.setLayoutParams(params);
    }

    /**
     * Helper for determining if the grid view is visible
     *
     * @return {@link boolean} TRUE if grid view has visibility VISIBLE
     */
    public boolean isGridViewVisible() {
        return m_gridView == null ? false : (m_gridView.getVisibility() == View.VISIBLE);
    }

    /**
     * Helper that will return the grid view width
     *
     * @return {@link int} GridView width in pixels
     */
    public int getGridViewWidth() {
        return m_gridView.getWidth();
    }

    /**
     * Helper that will return the grid view height
     *
     * @return {@link int} GridView height in pixels
     */
    public int getGridViewHeight() {
        return m_gridView.getHeight();
    }

    /**
     * Sets an click listener that should be executed whenever an item is clicked
     *
     * @param listener {@link android.view.View.OnClickListener} Listener to execute onItemClick
     */
    public void setGridViewOnItemClickListener(AdapterView.OnItemClickListener listener) {
        if (m_gridView != null) {
            m_gridView.setOnItemClickListener(listener);
        }
    }

    /**
     * Sets a simple listener that should be executed whenever the layout changes
     *
     * @param callback {@link com.dpg.crowdscout.utils.SimpleCallback} Callback to execute when layout changes
     */
    public void setOnLayoutChangedCallback(@Nullable SimpleCallback callback) {
        m_onLayoutChangedCallback = callback;
    }
}
