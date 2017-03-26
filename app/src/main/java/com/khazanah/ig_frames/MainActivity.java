package com.khazanah.ig_frames;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterViewFlipper;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {
    public static String lineSep = System.getProperty("line.separator");
    public static String myLink = "";
//    public static String initial_hastags = "";
    public AdapterViewFlipper avf;
    private Intent intent;
    private Gson gson = new Gson();
    private SharedPreferences sharedPreferences;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private int flipIntervalInt = 3500;
    private double inputInterval = 0;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUI(intent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Unlock screen automatically when in debug mode - do not need to unlock manually
        if (BuildConfig.DEBUG) {
            DebugUtils.riseAndShine(this);
        }
        setContentView(R.layout.activity_main);
        sharedPreferences = getSharedPreferences("NAME", 0);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
//        initial_hastags = prefs.getString("hashtag", "goldengate");
//        System.out.println("initial hashtags onCreate: " + initial_hastags);
        editor = sharedPreferences.edit();
        intent = new Intent(getApplicationContext(), GetDataService.class);
        registerReceiver(broadcastReceiver, new IntentFilter(GetDataService.BROADCAST_ACTION));
        startService(intent);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        setContentView(R.layout.activity_main);
        String sharedPrefListJson = sharedPreferences.getString("pObListJson", "NA");
        List<PhotoObject> lopo = gson.fromJson(sharedPrefListJson,
                new TypeToken<List<PhotoObject>>() {}.getType());
//        TODO: For getting tags from the user (Work In Progress)
//        if (!initial_hastags.equals(prefs.getString("hashtag", "goldengate"))) {
//            System.out.println("HELLO");
//            lopo.clear();
//            editor.putString("pObListJson", "").apply();
//            intent = new Intent(getApplicationContext(), GetDataService.class);
//            registerReceiver(broadcastReceiver, new IntentFilter(GetDataService.BROADCAST_ACTION));
//            startService(intent);
//            return;
//        }
        if (myLink.contains("access_token")) {
            lopo.clear();
            editor.putString("pObListJson", "").apply();
            intent = new Intent(getApplicationContext(), GetDataService.class);
            intent.putExtra("test", myLink.substring(myLink.indexOf("=") + 1));
            registerReceiver(broadcastReceiver, new IntentFilter(GetDataService.BROADCAST_ACTION));
            startService(intent);
        } else {
            avf = (AdapterViewFlipper) findViewById(R.id.adapterViewFlipper);
            avf.setAdapter(new CustomAdapter(this, lopo));
        }
//        TODO: For getting tags from the user (Work In Progress)
//        if (myLink.contains("access_token") || !prefs.getString("hashtag", "test").equals("goldengate")) {
//            lopo.clear();
//            editor.putString("pObListJson", "").apply();
//            intent = new Intent(getApplicationContext(), GetDataService.class);
//            if (myLink.contains("access_token")) {
//                intent.putExtra("test", myLink.substring(myLink.indexOf("=") + 1));
//            }
//            registerReceiver(broadcastReceiver, new IntentFilter(GetDataService.BROADCAST_ACTION));
//            startService(intent);
//        } else {
//            avf = (AdapterViewFlipper) findViewById(R.id.adapterViewFlipper);
//            avf.setAdapter(new CustomAdapter(this, lopo));
//        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent preferences = new Intent(this, AppPreferences.class);
                startActivity(preferences);
                return true;
            case R.id.connect:
                Intent login = new Intent(this, Login.class);
                startActivity(login);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

//    Called after receiving the broadcast - retrieves the list of PhotoObjects and sets the adapter
    private void updateUI(Intent intent) {
        String photoObjectsListData = intent.getStringExtra(GetDataService.RETRIEVE_PHOTO_OBJECTS_LIST);
        editor.putString("pObListJson", photoObjectsListData).apply();

        List<PhotoObject> photoObjects = gson.fromJson(photoObjectsListData,
                new TypeToken<List<PhotoObject>>() {}.getType());

        unregisterReceiver(broadcastReceiver);

        avf = (AdapterViewFlipper) findViewById(R.id.adapterViewFlipper);
        avf.setAdapter(new CustomAdapter(this, photoObjects));
    }

//    The adapter is the main display in the app - it contains the image and the text box below it
    public class CustomAdapter extends BaseAdapter {

        private Context context;
        private Holder holder;
        private LayoutInflater inflater;
        private List<PhotoObject> photos;

        public CustomAdapter(Context context, List<PhotoObject> photos) {
            this.context = context;
            this.photos = photos;
            inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (!prefs.getString("duration", "zero").equals("zero")) {
                inputInterval = Double.parseDouble(prefs.getString("duration", "zero"));
                inputInterval *= 1000;
                flipIntervalInt = (int) inputInterval;
            }
            avf.setFlipInterval(flipIntervalInt);
            avf.startFlipping();
            Collections.shuffle(this.photos);
        }

//        4 methods need to be overwritten
        @Override
        public int getCount() {
            return photos.size();
        }

        @Override
        public Object getItem(int position) {
            if(position >= photos.size()) {
                position = position % photos.size();
            }
            return photos.get(position).url;
        }

        @Override
        public long getItemId(int position) {
            if(position >= photos.size()) {
                position = position % photos.size();
            }
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.view_flipper, parent, false);

                holder = new Holder();
                holder.img1 = (ImageButton) convertView.findViewById(R.id.imgBtn1);
                holder.img2 = (ImageButton) convertView.findViewById(R.id.imgBtn2);
                holder.tv1 = (TextView) convertView.findViewById(R.id.tv1);
                holder.pause = (ImageView) convertView.findViewById(R.id.pause);

                convertView.setTag(holder);
            } else {
                holder = (Holder) convertView.getTag();
            }

//            Start/stop the slideshow when the image is clicked
            holder.img1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (avf.isFlipping()) {
                        avf.stopFlipping();
                        holder.pause.setVisibility(View.VISIBLE);
                    } else {
                        avf.showNext();
                        avf.startFlipping();
                        Toast.makeText(MainActivity.this, getResources()
                                .getString(R.string.restart), Toast.LENGTH_SHORT).show();
                    }
                }
            });

//            Picasso is an external library that handles image loading and caching
            Picasso.with(context)
                    .load(photos.get(position).url)
                    .placeholder(R.drawable.loading)
                    .into(holder.img1);
            String show = photos.get(position).caption + lineSep + (photos.get(position).date)
                    .substring(0, 10);
            if (position == photos.size() - 1)
                position = -1;
            Picasso.with(context)
                    .load(photos.get(position + 1).url)
                    .placeholder(R.drawable.loading)
                    .into(holder.img2);

//            Display caption and date below the image
            holder.tv1.setText(show);

            return convertView;
        }

        public class Holder {
            ImageButton img1;
            ImageButton img2;
            TextView tv1;
            ImageView pause;
        }
    }
}
