package com.google.ar.core.examples.java.helloar.model;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by lukas on 14.01.18.
 */

public class GeneratePinboardObject {

    private final Pinboard pinboard;

    private GeneratePinboardObject() {
        String tileName = "News";
        String tileCategory = "Information";
        String tileContent = "Today is winter";
        final PinboardTiles firstPinboardTiel = new PinboardTiles(tileName, tileCategory, tileContent);

        tileName = "Laught";
        tileCategory = "Comedy";
        tileContent = "Today is winter but its April";
        final PinboardTiles secondfPinboardTiel = new PinboardTiles(tileName, tileCategory, tileContent);

        LinkedList<PinboardTiles> pinboardTiles = new LinkedList<>();
        pinboardTiles.add(firstPinboardTiel);
        pinboardTiles.add(secondfPinboardTiel);

        final String pinbaordName = "First Pinboard";
        final String pinboardQRCode = "20";
        final PinboardLocation pinboardLocation = new PinboardLocation(100.0, 100.0);

        pinboard = new Pinboard(pinbaordName, pinboardQRCode, pinboardLocation, pinboardTiles);
    }

    public Pinboard getGeneratedPinboard() {
        return pinboard;
    }
}
