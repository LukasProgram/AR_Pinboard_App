package com.google.ar.core.examples.java.helloar.model;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

public class Pinbaord {

    @NonNull
    private String id;
    @NonNull
    private final String name;
    @NonNull
    private final String qrCode;
    @Nullable
    private final PinboardLocation pinboardLocation;

    private float distance;
    private boolean inRange;
    private float[] zeroMatrix

    public Pinbaord(@NonNull String name, @NonNull String qrCode, PinboardLocation pinboardLocation, List<PinboardTiles> pinboardTilesiList) {
        this.name = name;
        this.qrCode = qrCode;
        this.pinboardLocation = pinboardLocation;
        this.pinboardTilesiList = pinboardTilesiList;
    }

    @Nullable
    private final List<PinboardTiles> pinboardTilesiList;


}
