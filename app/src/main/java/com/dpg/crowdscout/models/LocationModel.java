package com.dpg.crowdscout.models;

import android.content.Context;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;

import com.dpg.crowdscout.R;
import com.dpg.crowdscout.utils.Helpers;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LocationModel implements Comparable, Parcelable {
    private static final String LOG_TAG = LocationModel.class.getSimpleName();
    public static final VenueFilter DEFAULT_VENUE_FILTER = VenueFilter.food;

    public static enum VenueFilter {
        trending, drinks, food, outdoors, sights, coffee, shops, arts, casino;
    }

    @JsonProperty("name")
    private String m_name;

    @JsonProperty("category")
    private VenueFilter m_category;

    @JsonProperty("description")
    private String m_description;

    @JsonProperty("imageUrl")
    private String m_imageUrl;

    // *******************************************************
    // FACTORY METHODS
    // *******************************************************

    // TODO: Replace all these factory methods with a Builder pattern
    public static LocationModel newInstance(@NonNull String name) {
        return newInstance(name, DEFAULT_VENUE_FILTER);
    }

    public static LocationModel newInstance(@NonNull String name, @NonNull VenueFilter category) {
        return newInstance(name, category, "");
    }

    public static LocationModel newInstance(@NonNull String name, @NonNull VenueFilter category, @NonNull String description) {
        return newInstance(name, category, description, Helpers.INSTANCE.getRandomPlaceholderImage());
    }

    public static LocationModel newInstance(@NonNull String name, @NonNull VenueFilter category, @NonNull String description, @NonNull String imageUrl) {
        return new LocationModel(name, category, description, imageUrl);
    }

    public static LocationModel newDefaultInstance() {
        final Context context = Helpers.INSTANCE.getAppContext();

        if (context == null) {
            return newInstance("Boston, MA");
        }

        final Resources res = context.getResources();
        final String name = res.getString(R.string.default_location_name);
        final String description = res.getString(R.string.default_location_description);
        final String imageUrl = res.getString(R.string.default_location_image_url);
        return newInstance(name, VenueFilter.food, description, imageUrl);
    }

    // *******************************************************
    // CONSTRUCTOR
    // *******************************************************

    LocationModel(@NonNull String name, @NonNull VenueFilter category, @NonNull String description, @NonNull String imageUrl) {
        m_name = name;
        m_category = category;
        m_description = description;
        m_imageUrl = imageUrl;
    }

    // *******************************************************
    // GETTERS
    // *******************************************************

    @JsonProperty("name")
    public String getName() {
        return m_name;
    }

    @JsonProperty("description")
    public String getDescription() {
        return m_description;
    }

    @JsonProperty("category")
    public VenueFilter getCategory() {
        return m_category;
    }

    @JsonProperty("imageUrl")
    public String getImageUrl() {
        return m_imageUrl;
    }

    /**
     * Used by RestClient when fetching venues for the active category for this location
     *
     * @return {@link String} Encoded query-param (e.g. "Las Vegas,NV" => "las+vegas,nv"
     */
    @JsonIgnore
    public String getEncodedName() {
        return Helpers.INSTANCE.normalizeQueryParam(m_name);
    }

    @JsonIgnore
    public String asJsonString() {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            Log.e(LOG_TAG, "asJsonString(): Exception Encountered: " + e.getMessage(), e);
        }

        // Use an empty JsonObject if something goes wrong
        return "{}";
    }

    // *******************************************************
    // SETTERS
    // *******************************************************

    @JsonProperty("name")
    public void setName(@NonNull String name) {
        // Verify param is not null in case JsonPath ignores annotation
        m_name = name == null ? "" : name.trim();
    }

    @JsonProperty("category")
    public void setCategory(@NonNull VenueFilter category) {
        // Verify param is not null in case JsonPath ignores annotation
        m_category = category == null ? DEFAULT_VENUE_FILTER : category;
    }

    @JsonProperty("description")
    public void setDescription(@NonNull String description) {
        // Verify param is not null in case JsonPath ignores annotation
        m_description = description == null ? "" : description.trim();
    }

    @JsonProperty("imageUrl")
    public void setImageUrl(@NonNull String imageUrl) {
        // Verify param is not null in case JsonPath ignores annotation
        m_imageUrl = imageUrl == null ? "" : imageUrl.trim();
    }

    // *******************************************************
    // COMPARATOR
    // *******************************************************

    @Override
    public int compareTo(Object another) {
        // Make sure there is something to compare to
        if (another == null || !(another instanceof LocationModel)) {
            return 1;
        }

        final LocationModel otherModel = (LocationModel) another;

        // Sort by name:category
        final String currLocation = String.format("%s:%s", getName(), getCategory());
        final String otherLocation = String.format("%s:%s", otherModel.getName(), otherModel.getCategory());

        // Perform comparison on name:category attributes
        final int comparison = currLocation.compareTo(otherLocation);

        // If the items have the same name and category, compare descriptions as a fallback
        if (comparison == 0) {
            return getDescription().compareTo(otherModel.getDescription());
        }

        return comparison;
    }

    @Override
    public boolean equals(Object another) {
        // Make sure there is something to compare to
        if (another == null || !(another instanceof LocationModel)) {
            return false;
        }

        // Hash codes should be the same if the two objects are the same
        final LocationModel otherModel = (LocationModel) another;
        return (this.hashCode() == otherModel.hashCode());
    }

    @Override
    public int hashCode() {
        // Use model.asJsonString() to compute hash code, which will combine all attrs into one string
        return asJsonString().hashCode();
    }

    // *******************************************************
    // PARCEL RELATED
    // *******************************************************

    public LocationModel() {
        // Required default constructor to enable reflection operations performed by Jackson JSON
    }

    protected LocationModel(Parcel in) {
        m_name = in.readString();
        m_category = (VenueFilter) in.readValue(VenueFilter.class.getClassLoader());
        m_description = in.readString();
        m_imageUrl = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(m_name);
        dest.writeValue(m_category);
        dest.writeString(m_description);
        dest.writeString(m_imageUrl);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<LocationModel> CREATOR = new Parcelable.Creator<LocationModel>() {
        @Override
        public LocationModel createFromParcel(Parcel in) {
            return new LocationModel(in);
        }

        @Override
        public LocationModel[] newArray(int size) {
            return new LocationModel[size];
        }
    };
}
