package io.intrepid.russell.checkin;

import android.content.res.Resources;
import android.support.annotation.StringRes;

import com.google.android.gms.maps.model.LatLng;

public enum LocationData {

    THIRD_ST(42.367023, -71.080052, R.string.label_third, "Third_St"),
    ROGERS_ST(42.366403, -71.077766, R.string.label_rogers, "Rogers_St"),;

    public static final int GEOFENCE_RADIUS = 50; // m

    final double lng; // deg
    final double lat; // deg
    @StringRes
    final int labelRes; // User visible label
    final String id;    // String id used by Geofencing API
    final LatLng latLng;

    LocationData(double lat, double lng, @StringRes int labelRes, String id) {
        this.lat = lat;
        this.lng = lng;
        this.labelRes = labelRes;
        this.id = id;
        this.latLng = new LatLng(lat, lng);
    }

    public String getLabel(Resources res) {
        return res.getString(labelRes);
    }
}
