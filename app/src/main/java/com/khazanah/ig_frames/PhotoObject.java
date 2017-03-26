package com.khazanah.ig_frames;

/**
 * Created by Gaurang on 12/2/15.
 **/

// Container class for holding all relevant info for photos
public class PhotoObject {

    public String url;
    public String caption;
    public String tags;
    public String date;
    public String latitude;
    public String longitude;
    public String locationName;

    public PhotoObject(String url, String caption, String tags, String date, String latitude,
                       String longitude, String locationName) {
        this.url = url;
        this.caption = caption;
        this.tags = tags;
        this.date = date;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationName = locationName;
    }
}
