package com.google.ar.core.examples.java.helloar.model;


import android.support.annotation.Nullable;

public class PinboardLocation {

    @Nullable
    private final Double latitude;

    @Nullable
    private final Double longitude;


    public PinboardLocation(@Nullable final Double latitude, @Nullable final Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Nullable
    public Double getLatitude() {
        return latitude;
    }

    @Nullable
    public Double getLongitude() {
        return longitude;
    }

}
