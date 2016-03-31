package com.example.riteden.sunshine.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.util.Log;
import android.text.format.Time;
import android.widget.Toast;

import com.example.riteden.sunshine.app.data.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.lang.Object;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.StringTokenizer;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.CursorLoader;
/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>{

    private int mPosition;
    private ListView mlistView;
    private ForecastAdapter AA;
    private final String LIST_POSITION = "list_position";
    private final String LOG_TAG = MainActivityFragment.class.getSimpleName();
    private static final int FORECAST_LOADER = 0;
    private static final String[] FORECAST_COLUMNS = {

            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_LOCATION_SETTING = 5;
    static final int COL_WEATHER_CONDITION_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;
    private static final int MY_LOADER_ID = 0;

    public MainActivityFragment() {

    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onItemSelected(Uri dateUri);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }


    // since we read the location when we create the loader, all we need to do is restart things
    void onLocationChanged( ) {
        refresh();
        getLoaderManager().restartLoader(MY_LOADER_ID, null, this);
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // TODO Add your menu entries here
        inflater.inflate(R.menu.forcastfragment, menu);
        inflater.inflate(R.menu.main, menu);
        inflater.inflate(R.menu.maps, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }


    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String locationSetting = Utility.getPreferredLocation(getActivity());
        // Sort order:  Ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting, System.currentTimeMillis());

        // The CursorAdapter will take data from our cursor and populate the ListView
        // However, we cannot use FLAG_AUTO_REQUERY since it is deprecated, so we will end
        // up with an empty list the first time we run.

        CursorLoader loader = new CursorLoader(getActivity(), weatherForLocationUri,
                FORECAST_COLUMNS, null, null, sortOrder);
        return loader;
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        AA.swapCursor(data);
        if(mPosition != ListView.INVALID_POSITION)
            mlistView.setSelection(mPosition);
    }

    public void onLoaderReset(Loader<Cursor> loader){
        AA.swapCursor(null);
    }

    @Override
        public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                // Not implemented here
                refresh();
                return true;
            case R.id.action_settings:
                Intent settingIntent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(settingIntent);
                return true;
            case R.id.action_map:
                String location = Utility.getPreferredLocation(getActivity());
                Uri gmmIntentUri = Uri.parse("geo:0,0?").buildUpon().appendQueryParameter("q", location).build();
                showMap(gmmIntentUri);
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void showMap(Uri geoLocation) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    public void refresh(){
        FetchWeatherTask weatherTask = new FetchWeatherTask(getActivity());

        String location = Utility.getPreferredLocation(getActivity());
        Log.v(LOG_TAG, "location = " + location);
        weatherTask.execute(location);
    }

    @Override
    public void onStart(){
        super.onStart();
        refresh();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state
        if (mPosition != ListView.INVALID_POSITION)
            savedInstanceState.putInt(LIST_POSITION, mPosition);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootview = inflater.inflate(R.layout.fragment_main, container, false);
        //List<String> myList = new ArrayList<String>();
        if(savedInstanceState != null && savedInstanceState.containsKey(LIST_POSITION)){
            mPosition = savedInstanceState.getInt(LIST_POSITION);
        }
        //myList.add(new FetchWeatherTask().execute());
        mlistView = (ListView) rootview.findViewById(R.id.listview_forecast);
        AA = new ForecastAdapter(getActivity(), null, 0);

        mlistView.setAdapter(AA);
        mlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                //TODO: CHECK IF THE IMPLEMENTATION IS RIGHT
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                if (cursor != null) {
                    String locationSetting = Utility.getPreferredLocation(getActivity());
                    Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                            locationSetting, System.currentTimeMillis());
                    ((Callback)getActivity()).onItemSelected(weatherForLocationUri);
                }
                mPosition = position;
            }
        });


    // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.


        return rootview;

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(MY_LOADER_ID, null, this);
        super.onActivityCreated(savedInstanceState);

    }



}
