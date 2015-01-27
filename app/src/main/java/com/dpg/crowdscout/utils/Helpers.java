package com.dpg.crowdscout.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import com.dpg.crowdscout.CrowdScoutApp;
import com.dpg.crowdscout.R;
import com.dpg.crowdscout.models.LocationModel;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * This class is intended for generic helper methods
 */
public enum Helpers {
    INSTANCE;

    private static final String LOG_TAG = Helpers.class.getSimpleName();

    /**
     * Enum used to assist with methods that require a direction, such as applying padding
     */
    public static enum Direction {
        LEFT, TOP, RIGHT, BOTTOM
    }

    /**
     * Fallback image urls intended for when application context is not available in unit tests
     */
    private final String[] FALLBACK_PLACEHOLDER_IMAGE_URLS = new String[]{
            "http://i.imgur.com/prDaaKR.jpg",
            "http://i.imgur.com/ly3H5Ce.jpg",
            "http://i.imgur.com/epSg1RO.jpg",
            "http://i.imgur.com/ExstdZ9.jpg"
    };

    /**
     * Contains the list of placeholder cover photo images
     */
    private List<String> m_placeholderImages;

    /**
     * Get a random image from the list of placeholder images
     *
     * @return {@link String} Url of a random image from the placeholder images list
     */
    public String getRandomPlaceholderImage() {
        List<String> placeholderImages = getPlaceholderImages();
        int randomPos = (int) (Math.random() * placeholderImages.size());
        return placeholderImages.get(randomPos);
    }

    /**
     * Convenience method for getting the Application context
     *
     * @return {@link Context} Application Context
     */
    public Context getAppContext() {
        return CrowdScoutApp.getAppContext();
    }

    /**
     * Returns a list of placeholder image urls that can be used for location cover photos. If the list
     * has not been initialized yet, the resource string array will be fetched using the ApplicationContext
     *
     * @return {@link List<String>} List of image URLs that can be used for placeholder location cover photos
     */
    @NonNull
    public List<String> getPlaceholderImages() {
        if (m_placeholderImages == null) {
            final Context context = getAppContext();
            String[] imageUrls = null;

            // Get string array containing placeholder image urls
            if (context != null) {
                imageUrls = context.getResources().getStringArray(R.array.placeholder_images);
            }

            // If image urls were not initialized, use the fallback array
            if (imageUrls == null || imageUrls.length == 0) {
                imageUrls = FALLBACK_PLACEHOLDER_IMAGE_URLS;
            }

            // Convert to ArrayList
            m_placeholderImages = new ArrayList<>(Arrays.asList(imageUrls));
        }
        return m_placeholderImages;
    }

    public int measureNumLines(@NonNull TextPaint textPaint, @NonNull String content, int widthLimit) {
        TextPaint paint = new TextPaint(textPaint);

        if (widthLimit <= 0) {
            Log.w(LOG_TAG, "measureNumLines(): Width (" + widthLimit + ") <= 0");
            return 0;
        }

        float lineSpacingExtra = 0.0f;
        float lineSpacingMultiplier = 1.0f;

        StaticLayout layout = new StaticLayout(content, paint, widthLimit,
                Layout.Alignment.ALIGN_NORMAL, lineSpacingMultiplier, lineSpacingExtra, false);

        return layout.getLineCount();
    }

    public int[] getPadding(@NonNull View v) {
        int paddingLeft = v.getPaddingLeft();
        int paddingTop = v.getPaddingTop();
        int paddingRight = v.getPaddingRight();
        int paddingBottom = v.getPaddingBottom();

        return new int[]{
                paddingLeft, paddingTop, paddingRight, paddingBottom
        };
    }

    public void setPadding(@NonNull View v, int px, Direction direction) {
        int[] padding = getPadding(v);
        padding[direction.ordinal()] = px;
        v.setPadding(padding[0], padding[1], padding[2], padding[3]);
    }

    /**
     * Compares content of two strings. If a null string is provided, an empty string
     * will be used for the comparison. (isTextEqual("",null) => true)
     * All strings will be trimmed as well.
     *
     * @param s1 {@link String}
     * @param s2 {@link String}
     * @return true if trimmed strings contain the same CASE-SENSITIVE text
     */
    public boolean isTextEqual(String s1, String s2) {
        // Replace null strings with empty strings
        s1 = s1 == null ? "" : s1.trim();
        s2 = s2 == null ? "" : s2.trim();


        // Compare values
        return s1.equals(s2);
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static void setBackground(@NonNull View view, Drawable background) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackgroundDrawable(background);
        } else {
            view.setBackground(background);
        }
    }

    /**
     * Used to normalize a string meant to be passed as a query parameter
     * <p/>
     * Converts something like 'Las Vegas, NV' into 'las+vegas,nv'
     *
     * @param s {@link String} The string that should be normalized
     * @return {@link String} Lowercase string with all whitespace replaced with '+'
     */
    @NonNull
    public static String normalizeQueryParam(final @NonNull String s) {
        return s.trim().toLowerCase().replaceAll(",[\\s]", ",").replaceAll("[\\s]", "+");
    }

    /**
     * Returns a color value specified by the given color ID
     * If the specified color could not be found, returns Color.CYAN
     * since it can be read in both black and white backgrounds
     *
     * @param colorId A color ID attribute such as R.color.white
     */
    public int getColor(int colorId) {
        int color = Color.CYAN;
        try {
            getAppContext().getResources().getColor(colorId);
        } catch (Resources.NotFoundException e) {
            Log.w(LOG_TAG, String.format("getColor(%d): Color Not Found!", colorId));
        }
        return color;
    }

    /**
     * Helper used to get strings by supplying a corresponding resource ID
     *
     * @param stringId {@link int} A string resource ID, such as R.string.app_name
     * @return {@link String} The string associated with the provided resource ID
     */
    public String getString(int stringId) {
        return getAppContext().getResources().getString(stringId);
    }

    /**
     * Returns a color value specified by the given theme attribute.
     * If the method couldn't properly resolve the color, returns Color.CYAN
     * since it can be read in both black and white backgrounds
     *
     * @param colorAttribute A theme attribute such as R.attr.colorBackground.
     */
    public int getThemeColor(int colorAttribute) {
        final TypedValue typedValue = new TypedValue();
        getAppContext().getTheme().resolveAttribute(colorAttribute, typedValue, true);

        // As long as the retrieved type is a color, return the color associated with the attribute
        final int type = typedValue.type;
        if (type >= TypedValue.TYPE_FIRST_COLOR_INT || type == TypedValue.TYPE_LAST_COLOR_INT) {
            return typedValue.data;
        }

        // Color Attribute not found. Return default color CYAN
        return Color.CYAN;
    }

    /**
     * Parses 'assets/locations.json' which contains a JSON Array of LocationModel attributes
     *
     * @return {@link List<LocationModel>} List of LocationModels contained in 'assets/locations.json'
     */
    public List<LocationModel> loadLocationModels() {
        final List<LocationModel> locations = new ArrayList<>();

        try {
            final ObjectMapper mapper = new ObjectMapper();
            final JsonFactory jsonFactory = new JsonFactory();

            // InputStream is closed automatically by JsonParser
            final InputStream is = getAppContext().getAssets().open("locations.json");
            final JsonParser jsonParser = jsonFactory.createParser(is);
            final JsonNode locationNodes = mapper.readTree(jsonParser);

            if (locationNodes != null && locationNodes.isArray()) {
                final List<LocationModel> items = mapper.readValue(locationNodes.traverse(jsonParser.getCodec()), new TypeReference<ArrayList<LocationModel>>() {
                });
                if (items != null && !items.isEmpty()) {
                    locations.addAll(items);
                }
            }

            // Use some default locations if there was a problem parsing the file
            else {
                locations.addAll(Arrays.asList(
                        LocationModel.newDefaultInstance(),
                        LocationModel.newInstance("New York, NY", LocationModel.VenueFilter.drinks),
                        LocationModel.newInstance("Las Vegas, NV", LocationModel.VenueFilter.casino),
                        LocationModel.newInstance("San Francisco, CA", LocationModel.VenueFilter.food)
                ));
            }

        } catch (IOException e) {
            Log.e(LOG_TAG, "loadLocationModels(): IOException Encountered: " + e.getMessage(), e);
        }

        return locations;
    }

    // Recursively calculates the size of fileOrDirectory including all contained files and directories.
    public long calculateSizeRecursive(File fileOrDirectory) {
        long size = 0;

        if (fileOrDirectory == null) {
            return 0;
        }

        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                size += calculateSizeRecursive(child);
            }
        } else {
            size = fileOrDirectory.length();
        }

        return size;
    }

    public boolean isLessThanXHours(Date d, int numHours) {
        return isBeforeSpecifiedTime(d, Calendar.MINUTE, -(numHours * 60));
    }

    public boolean isLessThanXMinutes(Date d, int numMinutes) {
        return isBeforeSpecifiedTime(d, Calendar.SECOND, -(60 * numMinutes));
    }

    private boolean isBeforeSpecifiedTime(Date d, int timeField, int value) {
        Calendar currentTimeCal = Calendar.getInstance();
        currentTimeCal.add(timeField, value);

        Calendar comparisonCal = Calendar.getInstance();
        comparisonCal.setTime(d);

        return currentTimeCal.before(comparisonCal);
    }
}
