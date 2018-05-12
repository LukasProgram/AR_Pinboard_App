package com.google.ar.core.examples.java.helloar.model;


import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.ar.core.examples.java.helloar.rendering.ObjectRenderer;

import java.util.List;

public class Pinboard {

    @NonNull
    private String id;
    @NonNull
    private final String name;
    @NonNull
    private final String qrCode;
    @Nullable
    private  PinboardLocation pinboardLocation;
    @Nullable
    private final List<PinboardTiles> pinboardTilesiList;

    private float distance;
    private boolean inRange;
    private float[] zeroMatrix;
    private ObjectRenderer virtualObject;


    public Pinboard(@NonNull final String name,
                    @NonNull final String qrCode,
                    @Nullable final PinboardLocation pinboardLocation,
                    @Nullable final List<PinboardTiles> pinboardTilesiList) {
        this.name = name;
        this.qrCode = qrCode;
        this.pinboardLocation = pinboardLocation;
        this.pinboardTilesiList = pinboardTilesiList;
    }
    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getQrCode() {
        return qrCode;
    }

    @Nullable
    public Location getPinboardLocation() {
       final Location location = new Location(name);
       Double longitude = null;
       Double latitude = null;
       if (pinboardLocation != null) {
            longitude = pinboardLocation.getLongitude();
            latitude = pinboardLocation.getLatitude();
           if (longitude != null && latitude != null) {
                location.setLongitude(longitude);
                location.setLatitude(latitude);
           }
       }
       return location;
    }

    @Nullable
    public List<PinboardTiles> getPinboardTilesiList() {
        return pinboardTilesiList;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public boolean isInRange() {
        return inRange;
    }

    public void setInRange(boolean inRange) {
        this.inRange = inRange;
    }

    public float[] getZeroMatrix() {
        return zeroMatrix;
    }

    public void setZeroMatrix(float[] zeroMatrix) {
        this.zeroMatrix = zeroMatrix;
    }

    public ObjectRenderer getVirtualObject() {
        return virtualObject;
    }

    public void setVirtualObject(ObjectRenderer virtualObject) {
        this.virtualObject = virtualObject;
    }
}
