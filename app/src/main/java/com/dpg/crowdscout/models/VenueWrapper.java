package com.dpg.crowdscout.models;

import android.support.annotation.NonNull;

import com.crowdscout.api.models.foursquare.ConcreteImage;
import com.crowdscout.api.models.foursquare.FoursquarePhoto;
import com.crowdscout.api.models.foursquare.FoursquareTip;
import com.crowdscout.api.models.foursquare.FoursquareVenue;
import com.google.common.base.Strings;

public class VenueWrapper {
    private final FoursquareVenue m_venue;

    public VenueWrapper(@NonNull FoursquareVenue venue) {
        m_venue = venue;
    }

    public FoursquareVenue getVenue() {
        return m_venue;
    }

    public boolean hasTips() {
        if (m_venue.tips != null && !m_venue.tips.isEmpty()) {
            final FoursquareTip tip = m_venue.tips.get(0);
            final String tipText = tip == null ? null : tip.text;
            if (!Strings.isNullOrEmpty(tipText)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    public String getTip() {
        if (hasTips()) {
            return m_venue.tips.get(0).text;
        }
        return "";
    }

    public boolean hasPhoto() {
        if (m_venue.photos != null && !m_venue.photos.isEmpty()) {
            final FoursquarePhoto venuePhoto = m_venue.photos.get(0);
            final String venuePhotoUrl = venuePhoto == null
                    ? null : venuePhoto.getUrlFromWidth(ConcreteImage.ImageSize.SMALL.width);

            if (!Strings.isNullOrEmpty(venuePhotoUrl)) {
                return true;
            }
        }

        return false;
    }

    @NonNull
    public String getPhoto(@NonNull ConcreteImage.ImageSize size) {
        if (hasPhoto()) {
            return m_venue.photos.get(0).getUrlFromWidth(size.width);
        }
        return "";
    }

    public boolean hasCategory() {
        return (m_venue.category != null && !Strings.isNullOrEmpty(m_venue.category.name));
    }

    public boolean hasCategoryIcon() {
        return (hasCategory() && m_venue.category.icon != null && !Strings.isNullOrEmpty(m_venue.category.icon.getUrl()));
    }

    @NonNull
    public String getCategoryName() {
        if (hasCategory()) {
            return m_venue.category.name;
        }
        return "";
    }

    @NonNull
    public String getCategoryIconUrl() {
        if (hasCategoryIcon()) {
            return m_venue.category.icon.getUrl();
        }
        return "";
    }

    public boolean isSaved() {
        // TODO: Implement
        return false;
    }

    public void save() {
        // Make sure this operation is legal
        if (isSaved()) {
            return;
        }

        // TODO: Implement

    }

    public void unsave() {
        // Make sure this operation is legal
        if (!isSaved()) {
            return;
        }

        // TODO: Implement

    }
}
