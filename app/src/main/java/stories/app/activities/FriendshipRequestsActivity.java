package stories.app.activities;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import stories.app.R;
import stories.app.adapters.UsersRecyclerViewAdapter;
import stories.app.services.FriendshipRequestsService;
import stories.app.models.responses.ServiceResponse;
import stories.app.utils.LocalStorage;

public class FriendshipRequestsActivity extends AppCompatActivity implements UsersRecyclerViewAdapter.ItemClickListener {

    private RecyclerView recyclerView;
    private UsersRecyclerViewAdapter recyclerViewAdapter;
    private RecyclerView.LayoutManager recyclerViewLayoutManager;
    private ArrayList<HashMap<String,String>> dataset = new ArrayList<HashMap<String,String>>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_friendship_requests);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        TextView result = findViewById(R.id.friendship_requests_result);
        result.setText("");

        SearchView searchView = this.findViewById(R.id.friends_search_bar);
        searchView.setOnQueryTextListener(new SearchQueryHandler());
        searchView.setFocusable(true);
        searchView.requestFocus();

        // set up the RecyclerView
        recyclerView = findViewById(R.id.users_recycler_view);
        recyclerViewLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(recyclerViewLayoutManager);
        recyclerViewAdapter = new UsersRecyclerViewAdapter(this, dataset, "users");
        recyclerViewAdapter.setClickListener(this);
        recyclerView.setAdapter(recyclerViewAdapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_friendship_requests, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.friendship_requests_sent) {
            Intent navigationIntent = new Intent(FriendshipRequestsActivity.this, FriendshipRequestsSentActivity.class);
            startActivity(navigationIntent);
            return(true);
        }

        if (item.getItemId() == R.id.friendship_requests_received) {
            Intent navigationIntent = new Intent(FriendshipRequestsActivity.this, FriendshipRequestsReceivedActivity.class);
            startActivity(navigationIntent);
            return(true);
        }

        return(super.onOptionsItemSelected(item));
    }

    protected class SearchQueryHandler implements SearchView.OnQueryTextListener {
        public boolean onQueryTextChange(String s) {
            return true;
        }

        public boolean onQueryTextSubmit(String s){
            SearchView searchView = findViewById(R.id.friends_search_bar);

            new GetUsersTask().execute(searchView.getQuery().toString());
            searchView.clearFocus();
            return true;
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        CreateFriendshipRequestTask task = new CreateFriendshipRequestTask(this);
        task.execute(recyclerViewAdapter.getItem(position).get("username"));
    }

    @Override
    public void onSendMessageClick(View v, int position) {
        Intent navigationIntent = new Intent(FriendshipRequestsActivity.this, DirectMessagesConversationActivity.class);
        navigationIntent.putExtra("friendUsername", recyclerViewAdapter.getItem(position).get("username"));
        startActivity(navigationIntent);
    }

    @Override
    public void onProfileButtonClick(View v, int position) {
        Intent navigationIntent = new Intent(FriendshipRequestsActivity.this, ProfileActivity.class);
        navigationIntent.putExtra("username", recyclerViewAdapter.getItem(position).get("username"));
        startActivity(navigationIntent);
    }

    protected class GetUsersTask extends AsyncTask<String, Void, ServiceResponse<ArrayList<HashMap<String,String>>>> {
        private FriendshipRequestsService friendshipRequestsService = new FriendshipRequestsService();
        private Snackbar snackbar;
        private TextView result;

        public GetUsersTask() {
            this.snackbar = Snackbar.make(findViewById(R.id.activity_friendship_requests), "Buscando...", Snackbar.LENGTH_INDEFINITE);
            this.result = findViewById(R.id.friendship_requests_result);
        }

        protected void onPreExecute() {
            this.snackbar.show();
            this.result.setText("");
        }

        protected ServiceResponse<ArrayList<HashMap<String,String>>> doInBackground(String... params) {
            return friendshipRequestsService.getUsers(
                    params[0],
                    LocalStorage.getUser().id
            );
        }

        protected void onPostExecute(ServiceResponse<ArrayList<HashMap<String,String>>> response) {

            this.snackbar.dismiss();

            ServiceResponse.ServiceStatusCode statusCode = response.getStatusCode();

            if (statusCode == ServiceResponse.ServiceStatusCode.UNAUTHORIZED) {
                Intent navigationIntent = new Intent(FriendshipRequestsActivity.this, LogInActivity.class);
                startActivity(navigationIntent);
                return;
            }

            if (statusCode == ServiceResponse.ServiceStatusCode.SUCCESS) {
                ArrayList<HashMap<String,String>> result = response.getServiceResponse();
                dataset.clear();

                for (int i = 0; i < result.size(); i++) {
                    dataset.add(result.get(i));
                }

                this.result.setText("Se encontraron " + result.size() + " resultados");
                recyclerViewAdapter.notifyDataSetChanged();
            }
        }
    }

    protected class CreateFriendshipRequestTask extends AsyncTask<String, Void, ServiceResponse<String>> {
        private FriendshipRequestsService friendshipRequestsService = new FriendshipRequestsService();
        private Snackbar snackbar;
        private Context context;

        public CreateFriendshipRequestTask (Context context){
            this.snackbar = Snackbar.make(findViewById(R.id.activity_friendship_requests), "Realizando operación...", Snackbar.LENGTH_INDEFINITE);
            this.context = context;
        }

        protected void onPreExecute() {
            this.snackbar.show();
        }

        protected ServiceResponse<String> doInBackground(String... params) {
            return friendshipRequestsService.createFriendshipRequest(
                    LocalStorage.getUser().username,
                    params[0]
            );
        }

        protected void onPostExecute(ServiceResponse<String> response) {

            this.snackbar.dismiss();

            ServiceResponse.ServiceStatusCode statusCode = response.getStatusCode();
            if (statusCode == ServiceResponse.ServiceStatusCode.SUCCESS) {
                String result = response.getServiceResponse();
                if (result != "") {
                    Toast.makeText(this.context, "Enviaste una solicitud de amistad a: " + result, Toast.LENGTH_SHORT).show();

                    for (int i = 0; i < dataset.size(); i++) {
                        if (dataset.get(i).get("username") == result) {
                            dataset.remove(i);
                        }
                    }
                    recyclerViewAdapter.notifyDataSetChanged();
                }
            } else if (statusCode == ServiceResponse.ServiceStatusCode.UNAUTHORIZED) {
                Intent navigationIntent = new Intent(FriendshipRequestsActivity.this, LogInActivity.class);
                startActivity(navigationIntent);
            } else if (statusCode == ServiceResponse.ServiceStatusCode.CONFLICT) {
                Toast.makeText(this.context, "El usuario ya es tu amigo o tiene una solicitud de amistad pendiente.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
