package stories.app.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;
import stories.app.R;
import stories.app.models.Story;
import stories.app.models.responses.ServiceResponse;
import stories.app.services.StoryService;
import stories.app.utils.Base64UtilityClass;
import stories.app.utils.Dates;
import stories.app.utils.LocalStorage;

public class StoriesMapActivity extends AppCompatActivity implements GoogleMap.OnInfoWindowClickListener, OnMapReadyCallback {

    private ArrayList<Story> stories = new ArrayList<>();
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stories_map);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_stories_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.stories_list) {
            Intent navigationIntent = new Intent(StoriesMapActivity.this, HomeActivity.class);
            startActivity(navigationIntent);
            return(true);
        }
        return(super.onOptionsItemSelected(item));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng center = new LatLng(-34.606336, -58.381572);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 10));
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        mMap.setOnInfoWindowClickListener(this);

        new GetStoriesVisiblesToUserTask().execute(LocalStorage.getUser().id);
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        String username = marker.getSnippet().split(",")[3];
        Intent navigationIntent = new Intent(StoriesMapActivity.this, ProfileActivity.class);
        navigationIntent.putExtra("username", username);
        startActivity(navigationIntent);
    }

    protected class GetStoriesVisiblesToUserTask extends AsyncTask<String, Void, ServiceResponse<ArrayList<Story>>> {
        private StoryService storyService = new StoryService();

        protected ServiceResponse<ArrayList<Story>> doInBackground(String... params) {
            return storyService.getStoriesVisiblesToUser(params[0]);
        }

        protected void onPostExecute(ServiceResponse<ArrayList<Story>> response) {
            ServiceResponse.ServiceStatusCode statusCode = response.getStatusCode();
            if (statusCode == ServiceResponse.ServiceStatusCode.SUCCESS) {
                ArrayList<Story> stories = response.getServiceResponse();

                for (int i = 0; i < stories.size(); i++) {
                    Story story = stories.get(i);
                    if (!story.location.isEmpty()) {
                        String[] latLngSplit = story.location.split(",");

                        LatLng storyLatLng = new LatLng(Double.parseDouble(latLngSplit[0]), Double.parseDouble(latLngSplit[1]));
                        String formattedDate = "";

                        try {
                            Date date = Dates.convertUtcStringToDate(story.timestamp);
                            formattedDate = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(date);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        String snippet = story.name + "," + formattedDate + "," + story.title + "," + story.username;

                        mMap.addMarker(new MarkerOptions()
                                .position(storyLatLng)
                                .title(story.profilePic)
                                .snippet(snippet));

                    }
                }

                mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());
            } else if (statusCode == ServiceResponse.ServiceStatusCode.UNAUTHORIZED) {
                Intent navigationIntent = new Intent(StoriesMapActivity.this, LogInActivity.class);
                startActivity(navigationIntent);
            }
        }
    }

    class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        // These are both viewgroups containing an ImageView with id "badge" and two TextViews with id
        // "title" and "snippet".
        private final View mWindow;

        private final View mContents;

        CustomInfoWindowAdapter() {
            mWindow = getLayoutInflater().inflate(R.layout.stories_map_info_window, null);
            mContents = getLayoutInflater().inflate(R.layout.stories_map_info_window, null);
        }

        @Override
        public View getInfoWindow(Marker marker) {
            //render(marker, mWindow);
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            render(marker, mContents);
            return mContents;
        }

        private void render(Marker marker, View view) {
            String profilePicString = marker.getTitle();
            String snippet = marker.getSnippet();

            CircleImageView profilePic = ((CircleImageView)view.findViewById(R.id.profile_pic));
            profilePic.setImageBitmap(Base64UtilityClass.toBitmap(profilePicString));

            String name = snippet.split(",")[0];
            String storyTitle = snippet.split(",")[1];
            String timestamp = snippet.split(",")[2];

            TextView nameUI = ((TextView) view.findViewById(R.id.full_name));
            TextView titleUI = ((TextView) view.findViewById(R.id.story_title));
            TextView timestampUI = ((TextView) view.findViewById(R.id.timestamp));

            nameUI.setText(name);
            titleUI.setText(storyTitle);
            timestampUI.setText(timestamp);
        }
    }
}
