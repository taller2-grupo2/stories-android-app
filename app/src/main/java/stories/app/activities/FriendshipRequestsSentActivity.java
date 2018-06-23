package stories.app.activities;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;

import stories.app.R;
import stories.app.adapters.UsersRecyclerViewAdapter;
import stories.app.services.FriendshipRequestsService;
import stories.app.models.responses.ServiceResponse;
import stories.app.utils.LocalStorage;

public class FriendshipRequestsSentActivity extends AppCompatActivity implements UsersRecyclerViewAdapter.ItemClickListener{

    private RecyclerView recyclerView;
    private UsersRecyclerViewAdapter recyclerViewAdapter;
    private RecyclerView.LayoutManager recyclerViewLayoutManager;
    private ArrayList<HashMap<String,String>> dataset = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_friendship_requests_sent);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // set up the RecyclerView
        recyclerView = findViewById(R.id.friendship_requests_sent_recycler_view);
        recyclerViewLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(recyclerViewLayoutManager);
        recyclerViewAdapter = new UsersRecyclerViewAdapter(this, dataset, "sent");
        recyclerViewAdapter.setClickListener(this);
        recyclerView.setAdapter(recyclerViewAdapter);

        new GetFriendshipRequestsSent().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_friendship_requests, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.search_friends) {
            Intent navigationIntent = new Intent(FriendshipRequestsSentActivity.this, FriendshipRequestsActivity.class);
            startActivity(navigationIntent);
            return(true);
        } else if (item.getItemId() == R.id.friendship_requests_received) {
            Intent navigationIntent = new Intent(FriendshipRequestsSentActivity.this, FriendshipRequestsReceivedActivity.class);
            startActivity(navigationIntent);
            return(true);
        }
        return(super.onOptionsItemSelected(item));
    }

    @Override
    public void onItemClick(View view, int position) {
        DeleteFriendshipRequestTask task = new DeleteFriendshipRequestTask(this);
        task.execute(recyclerViewAdapter.getItem(position).get("username"));
    }

    @Override
    public void onSendMessageClick(View v, int position) {
    }

    protected class GetFriendshipRequestsSent extends AsyncTask<String, Void, ServiceResponse<ArrayList<HashMap<String,String>>>> {
        private FriendshipRequestsService friendshipRequestsService = new FriendshipRequestsService();

        protected void onPreExecute() {
        }

        protected ServiceResponse<ArrayList<HashMap<String,String>>> doInBackground(String... params) {
            return friendshipRequestsService.getFriendshipRequestsSent(
                    LocalStorage.getUsername()
            );
        }

        protected void onPostExecute(ServiceResponse<ArrayList<HashMap<String,String>>> response) {
            ServiceResponse.ServiceStatusCode statusCode = response.getStatusCode();
            if (statusCode == ServiceResponse.ServiceStatusCode.SUCCESS) {
                ArrayList<HashMap<String,String>> result = response.getServiceResponse();
                dataset.clear();
                for (int i = 0; i < result.size(); i++) {
                    dataset.add(result.get(i));
                }
                recyclerViewAdapter.notifyDataSetChanged();
            } else if (statusCode == ServiceResponse.ServiceStatusCode.UNAUTHORIZED) {
                Intent navigationIntent = new Intent(FriendshipRequestsSentActivity.this, LogInActivity.class);
                startActivity(navigationIntent);
            }
        }
    }

    protected class DeleteFriendshipRequestTask extends AsyncTask<String, Void, ServiceResponse<String>> {
        private FriendshipRequestsService friendshipRequestsService = new FriendshipRequestsService();

        private Context context;

        public DeleteFriendshipRequestTask (Context context){
            this.context = context;
        }

        protected void onPreExecute() {
        }

        protected ServiceResponse<String> doInBackground(String... params) {
            return friendshipRequestsService.deleteFriendshipRequest(
                    LocalStorage.getUsername(),
                    params[0]
            );
        }

        protected void onPostExecute(ServiceResponse<String> response) {
            ServiceResponse.ServiceStatusCode statusCode = response.getStatusCode();
            if (statusCode == ServiceResponse.ServiceStatusCode.SUCCESS) {
                String result = response.getServiceResponse();
                if (result != "") {
                    for (int i = 0; i < dataset.size(); i++) {
                        if (dataset.get(i).get("username") == result) {
                            dataset.remove(i);
                        }
                    }
                    recyclerViewAdapter.notifyDataSetChanged();
                }
            }
            else if (statusCode == ServiceResponse.ServiceStatusCode.UNAUTHORIZED) {
                Intent navigationIntent = new Intent(FriendshipRequestsSentActivity.this, LogInActivity.class);
                startActivity(navigationIntent);
            }
        }
    }
}
