package com.khazanah.ig_frames;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

//    Service runs in the background to get data from Instagram on app startup
public class GetDataService extends Service {

//    Strings for API (fetching images)

//    replace the access token to get the media count for another user
    public static String user_info = "https://api.instagram.com/v1/users/self/?" +
            "access_token=" + ApplicationData.KAI_ACCESS_TOKEN;
//    replace the access token to get images from another account
    public static String self_recent = "https://api.instagram.com/v1/users/self/media/recent" +
            "/?access_token=" + ApplicationData.KAI_ACCESS_TOKEN;
//    replace the access token to get liked images from another account
    public static String liked = "https://api.instagram.com/v1/users/self/media/" +
            "liked?access_token=" + ApplicationData.KAI_ACCESS_TOKEN;
//    replace goldengate below with whatever tag you want
    public static String tags = "https://api.instagram.com/v1/tags/goldengate/media/" +
            "recent?access_token=" + ApplicationData.KAI_ACCESS_TOKEN;
//    replace 784361207 with the location id of the desired place - the location id was recovered
//    from one of the photos posted on the KAI instagram account
    public static String location = "https://api.instagram.com/v1/locations/784361207/" +
            "media/recent?access_token=" + ApplicationData.KAI_ACCESS_TOKEN;

//  Strings for broadcast receiver and intent
    public static final String BROADCAST_ACTION = "com.khazanah.ig_frames.getdata";
    public static final String RETRIEVE_PHOTO_OBJECTS_LIST = "retrieve_Photo_Objects_List";

    public String next = "";
//    public String hashtags = "";
    public boolean initial = true;
    public int totalUserMediaCount = 0;
    public List<PhotoObject> photoObjects = new ArrayList<>();

    private Intent intent;
    private Gson gson = new Gson();
//    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        intent = new Intent(BROADCAST_ACTION);
//        TODO: For getting tags from the user (Work in Progress)
//        prefs = PreferenceManager.getDefaultSharedPreferences(this);
//        hashtags = prefs.getString("hashtag", "goldengate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String new_access_token = intent.getStringExtra("test");
        if (new_access_token!= null) {
//            modify all API strings here and provide the new access token
            user_info = "https://api.instagram.com/v1/users/self/?" +
                    "access_token=" + new_access_token;
            self_recent = "https://api.instagram.com/v1/users/self/media/recent" +
                    "/?access_token=" + new_access_token;
            liked = "https://api.instagram.com/v1/users/self/media/" +
                    "liked?access_token=" + new_access_token;
            tags = "https://api.instagram.com/v1/tags/goldengate/media/" +
                    "recent?access_token=" + new_access_token;
            location = "https://api.instagram.com/v1/locations/784361207/" +
                    "media/recent?access_token=" + new_access_token;
        }
        callOtherMethods();
        return START_NOT_STICKY;
    }

//    This method calls other methods sequentially to collect image data, which is then sent to the
//    Main Activity to be displayed. getData is called repeatedly with a different string passed in
//    each time. It get images recently uploaded by the user, images liked by the user, images from
//    a set tag, and also from a particular location (based on the location id)
//    This method uses a separate thread since a network call is being made - such calls are
//    resource intensive and can cause the app to get unresponsive if run on the main thread
    public void callOtherMethods() {
        Thread t = new Thread() {
            @Override
            public void run() {
                super.run();
                photoObjects.clear();
                next = "";
                getTotalMediaCount(user_info);
                getData(self_recent, initial);
                next = "";
                getData(liked, initial);
                next = "";
                getData(tags, initial);
                next = "";
                getData(location, initial);
//                TODO: For getting tags from the user (Work In Progress)
//                if (!hashtags.equals("")) {
//                    String[] single_hashtag = hashtags.split("\\s*,\\s*");
//                    for (String x : single_hashtag) {
//                        tags = "https://api.instagram.com/v1/tags/" + x + "/media/" +
//                                "recent?access_token=" + ApplicationData.KAI_ACCESS_TOKEN;
//                        getData(tags, initial);
//                        next = "";
//                    }
//                } else {
//                    getData(tags, initial);
//                }
                sendToMain();
            }
        };
        t.start();
    }

    public void getTotalMediaCount(final String s) {
        try {
            URL url = new URL(s);
            URLConnection tc = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(tc.getInputStream()));
            String line;
            try {
                while ((line = in.readLine()) != null) {
                    JSONObject ob = new JSONObject(line);
                    JSONObject object = ob.getJSONObject("data");
                    JSONObject counts = object.getJSONObject("counts");
                    totalUserMediaCount = counts.getInt("media");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//    This method gets the data using the URL passed in, and puts it into a list
    private void getData(final String s, final boolean init) {
        try {
            URL url = new URL(s);
            URLConnection tc = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(tc.getInputStream()));
            String line;
            String captionText;
            String latitude;
            String longitude;
            String locationName;
            try {
//                Get relevant data from IG and put it into a list of PhotoObject instances
//                IG returns JSON - need to get meaningful data out of that
                while ((line = in.readLine()) != null) {
                    JSONObject ob = new JSONObject(line);
                    JSONArray object = ob.getJSONArray("data");
                    JSONObject pag = ob.getJSONObject("pagination");
                    if (!pag.isNull("next_url")) {
                        next = pag.getString("next_url"); //here
                    }

                    for (int i = 0; i < object.length(); i++) {
                        JSONObject jo = (JSONObject) object.get(i);

                        JSONObject imageJsonObj = jo.getJSONObject("images");
                        JSONObject stdRes = imageJsonObj.getJSONObject("standard_resolution");

                        if (jo.isNull("caption")) {
                            captionText = "";
                        } else {
                            JSONObject captions = jo.getJSONObject("caption");
                            captionText = captions.getString("text");
                        }

                        long timeStamp = jo.getLong("created_time");
                        Date date = new Date(timeStamp * 1000L);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
                        sdf.setTimeZone(TimeZone.getTimeZone("GMT-8"));

                        if (jo.isNull("location")) {
                            latitude = getResources().getString(R.string.undefined);
                            longitude = getResources().getString(R.string.undefined);
                            locationName = getResources().getString(R.string.undefined);
                        } else {
                            JSONObject loc = jo.getJSONObject("location");
                            latitude = loc.getString("latitude");
                            longitude = loc.getString("longitude");
                            locationName = loc.getString("name");
                        }

                        PhotoObject po = new PhotoObject(stdRes.getString("url"),
                                captionText, jo.getString("tags"), sdf.format(date),
                                latitude, longitude, locationName);
                        if (!photoObjects.contains(po)) {
                            photoObjects.add(po);
                        }
                    }
                }
                if (!next.equals("")) {
                    if (!init) {
                        return;
                    }
                    getData(next, false);   //can use photoObjects.size here somehow
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//    The list of PhotoObjects is converted into a Json string in order to send it to the main
//    activity using the intent
    private void sendToMain() {
        String photoObjectsListJson = gson.toJson(photoObjects);
        intent.putExtra(RETRIEVE_PHOTO_OBJECTS_LIST, photoObjectsListJson);
        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
