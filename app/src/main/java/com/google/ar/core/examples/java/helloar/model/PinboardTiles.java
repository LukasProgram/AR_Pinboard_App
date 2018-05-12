package com.google.ar.core.examples.java.helloar.model;


import android.support.annotation.NonNull;

import com.google.ar.core.examples.java.helloar.rendering.ObjectRenderer;

public class PinboardTiles {

    @NonNull
    private String name;
    @NonNull
    private String category;
    @NonNull
    private String content;
    @NonNull
    private ObjectRenderer tileVirtualObject;

    public PinboardTiles(@NonNull String name, @NonNull String category, @NonNull String content) {
        this.name = name;
        this.category = category;
        this.content = content;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public String getCategory() {
        return category;
    }

    public void setCategory(@NonNull String category) {
        this.category = category;
    }

    @NonNull
    public String getContent() {
        return content;
    }

    public void setContent(@NonNull String content) {
        this.content = content;
    }

    @NonNull
    public ObjectRenderer getTileVirtualObject() {
        return tileVirtualObject;
    }

    public void setTileVirtualObject(@NonNull ObjectRenderer tileVirtualObject) {
        this.tileVirtualObject = tileVirtualObject;
    }


}
