package stories.app.services;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import stories.app.models.responses.ServiceResponse;
import stories.app.utils.Constants;
import stories.app.utils.LocalStorage;

public class FriendshipRequestsService {

    private String URL = Constants.appServerURI;

    public ServiceResponse<ArrayList<HashMap<String, String>>> getUsers(String partialUsername, String userID) {
        HttpURLConnection client = null;

        try {
            URL url = new URL(URL + "/users/search/" + userID + "/" + partialUsername);
            client = (HttpURLConnection) url.openConnection();
            client.setRequestMethod("GET");
            String token = String.format("Bearer %s", LocalStorage.getToken());
            client.setRequestProperty("Authorization", token);

            client.connect();

            BufferedReader br;

            int statusCode = client.getResponseCode();

            if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED){
                return new ServiceResponse<>(ServiceResponse.ServiceStatusCode.UNAUTHORIZED);
            }

            if (200 <= statusCode && statusCode <= 299) {
                br = new BufferedReader(new InputStreamReader(client.getInputStream()));
            } else {
                br = new BufferedReader(new InputStreamReader(client.getErrorStream()));
            }

            StringBuilder sb = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                sb.append(output);
            }
            String result = sb.toString();

            JSONObject jsonObject = new JSONObject(result);
            JSONArray jsonArray = jsonObject.getJSONArray("found_users");

            ArrayList<HashMap<String, String>> users = new ArrayList<HashMap<String, String>>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject itemObject = jsonArray.getJSONObject(i);
                HashMap<String, String> map = new HashMap<>();
                map.put("username", itemObject.getString("username"));
                map.put("profilePic", itemObject.getString("profile_pic"));
                map.put("name", itemObject.getString("name"));
                // Pulling items from the array
                users.add(map);
            }

            return new ServiceResponse<>(ServiceResponse.ServiceStatusCode.SUCCESS, users);

        } catch(MalformedURLException error) {
            //Handles an incorrectly entered URL
            ArrayList<HashMap<String,String>> result = new ArrayList<>();
            HashMap<String,String> map = new HashMap<>();
            map.put("error", "MalformedURLException");
            result.add(map);
            return new ServiceResponse<>(ServiceResponse.ServiceStatusCode.ERROR, result);
        }
        catch(SocketTimeoutException error) {
            //Handles URL access timeout.
            ArrayList<HashMap<String,String>> result = new ArrayList<>();
            HashMap<String,String> map = new HashMap<>();
            map.put("error", "SocketTimeoutException");
            result.add(map);
            return new ServiceResponse<>(ServiceResponse.ServiceStatusCode.ERROR, result);
        }
        catch (IOException error) {
            //Handles input and output errors
            ArrayList<HashMap<String,String>> result = new ArrayList<>();
            HashMap<String,String> map = new HashMap<>();
            map.put("error", "IOException");
            result.add(map);
            return new ServiceResponse<>(ServiceResponse.ServiceStatusCode.ERROR, result);
        }
        catch (JSONException error) {
            //Handles input and output errors
            ArrayList<HashMap<String,String>> result = new ArrayList<>();
            HashMap<String,String> map = new HashMap<>();
            map.put("error", "JSONException");
            result.add(map);
            return new ServiceResponse<>(ServiceResponse.ServiceStatusCode.ERROR, result);
        }
        finally {
            if(client != null) {
                client.disconnect();
            }
        }
    }

    public ServiceResponse<String> createFriendshipRequest(String fromUsername, String toUsername) {
        HttpURLConnection client = null;

        try {
            URL url = new URL(URL + "/friendship/request");
            client = (HttpURLConnection) url.openConnection();
            client.setRequestMethod("POST");
            client.setRequestProperty("Content-Type", "application/json");
            client.setRequestProperty("Accept", "application/json");
            String token = String.format("Bearer %s", LocalStorage.getToken());
            client.setRequestProperty("Authorization", token);

            JSONObject credentials = new JSONObject();
            credentials.put("from_username",fromUsername);
            credentials.put("to_username", toUsername);

            OutputStream outputStream = client.getOutputStream();
            outputStream.write(credentials.toString().getBytes("UTF-8"));
            outputStream.close();

            client.connect();

            BufferedReader br;

            int statusCode = client.getResponseCode();

            if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED){
                return new ServiceResponse<>(ServiceResponse.ServiceStatusCode.UNAUTHORIZED);
            }

            if (200 <= statusCode && statusCode <= 299) {
                br = new BufferedReader(new InputStreamReader(client.getInputStream()));
            } else if (statusCode == 409) {
                return new ServiceResponse<>(ServiceResponse.ServiceStatusCode.CONFLICT, "");
            } else {
                br = new BufferedReader(new InputStreamReader(client.getErrorStream()));
            }

            StringBuilder sb = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                sb.append(output);
            }
            String result = sb.toString();

            return new ServiceResponse<>(ServiceResponse.ServiceStatusCode.SUCCESS, toUsername);

        } catch(Exception error) {
            return new ServiceResponse<>(ServiceResponse.ServiceStatusCode.ERROR, "");
        }
        finally {
            if(client != null) {
                client.disconnect();
            }
        }
    }

    public ServiceResponse<ArrayList<HashMap<String,String>>> getFriendshipRequestsReceived(String toUsername) {
        HttpURLConnection client = null;

        try {
            URL url = new URL(URL + "/friendship/request/received/" + toUsername);
            client = (HttpURLConnection) url.openConnection();
            client.setRequestMethod("GET");
            String token = String.format("Bearer %s", LocalStorage.getToken());
            client.setRequestProperty("Authorization", token);

            client.connect();

            BufferedReader br;

            int statusCode = client.getResponseCode();

            if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED){
                return new ServiceResponse<>(ServiceResponse.ServiceStatusCode.UNAUTHORIZED);
            }

            if (200 <= statusCode && statusCode <= 299) {
                br = new BufferedReader(new InputStreamReader(client.getInputStream()));
            } else {
                br = new BufferedReader(new InputStreamReader(client.getErrorStream()));
            }

            StringBuilder sb = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                sb.append(output);
            }
            String result = sb.toString();

            JSONObject jsonObject = new JSONObject(result);
            JSONArray jsonArray = jsonObject.getJSONArray("friendship_requests");

            ArrayList<HashMap<String,String>> friendship_requests = new ArrayList<>();

            for (int i=0; i < jsonArray.length(); i++) {
                JSONObject itemObject = jsonArray.getJSONObject(i);
                HashMap<String,String> map = new HashMap<>();
                map.put("username", itemObject.getString("from_username"));
                map.put("profilePic", itemObject.getString("profile_pic"));
                map.put("name", itemObject.getString("name"));
                // Pulling items from the array
                friendship_requests.add(map);
            }

            return new ServiceResponse<>(ServiceResponse.ServiceStatusCode.SUCCESS, friendship_requests);

        } catch(MalformedURLException error) {
            //Handles an incorrectly entered URL
            ArrayList<HashMap<String,String>> result = new ArrayList<>();
            HashMap<String,String> map = new HashMap<>();
            map.put("error", "MalformedURLException");
            result.add(map);
            return new ServiceResponse<>(ServiceResponse.ServiceStatusCode.ERROR, result);
        }
        catch(SocketTimeoutException error) {
            //Handles URL access timeout.
            ArrayList<HashMap<String,String>> result = new ArrayList<>();
            HashMap<String,String> map = new HashMap<>();
            map.put("error", "SocketTimeoutException");
            result.add(map);
            return new ServiceResponse<>(ServiceResponse.ServiceStatusCode.ERROR, result);
        }
        catch (IOException error) {
            //Handles input and output errors
            ArrayList<HashMap<String,String>> result = new ArrayList<>();
            HashMap<String,String> map = new HashMap<>();
            map.put("error", "IOException");
            result.add(map);
            return new ServiceResponse<>(ServiceResponse.ServiceStatusCode.ERROR, result);
        }
        catch (JSONException error) {
            //Handles input and output errors
            ArrayList<HashMap<String,String>> result = new ArrayList<>();
            HashMap<String,String> map = new HashMap<>();
            map.put("error", "JSONException");
            result.add(map);
            return new ServiceResponse<>(ServiceResponse.ServiceStatusCode.ERROR, result);
        }
        finally {
            if(client != null) {
                client.disconnect();
            }
        }
    }

    public ServiceResponse<ArrayList<HashMap<String,String>>> getFriendshipRequestsSent(String fromUsername) {
        HttpURLConnection client = null;

        try {
            URL url = new URL(URL + "/friendship/request/sent/" + fromUsername);
            client = (HttpURLConnection) url.openConnection();
            client.setRequestMethod("GET");
            String token = String.format("Bearer %s", LocalStorage.getToken());
            client.setRequestProperty("Authorization", token);

            client.connect();

            BufferedReader br;

            int statusCode = client.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED){
                return new ServiceResponse<>(ServiceResponse.ServiceStatusCode.UNAUTHORIZED);
            }

            if (200 <= statusCode && statusCode <= 299) {
                br = new BufferedReader(new InputStreamReader(client.getInputStream()));
            } else {
                br = new BufferedReader(new InputStreamReader(client.getErrorStream()));
            }

            StringBuilder sb = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                sb.append(output);
            }
            String result = sb.toString();

            JSONObject jsonObject = new JSONObject(result);
            JSONArray jsonArray = jsonObject.getJSONArray("friendship_requests");

            ArrayList<HashMap<String,String>> friendship_requests = new ArrayList<>();

            for (int i=0; i < jsonArray.length(); i++) {
                JSONObject itemObject = jsonArray.getJSONObject(i);
                HashMap<String,String> map = new HashMap<>();
                map.put("username", itemObject.getString("to_username"));
                map.put("profilePic", itemObject.getString("profile_pic"));
                map.put("name", itemObject.getString("name"));
                // Pulling items from the array
                friendship_requests.add(map);
            }

            return new ServiceResponse<>(ServiceResponse.ServiceStatusCode.SUCCESS, friendship_requests);

        } catch(MalformedURLException error) {
            //Handles an incorrectly entered URL
            ArrayList<HashMap<String,String>> result = new ArrayList<>();
            HashMap<String,String> map = new HashMap<>();
            map.put("error", "MalformedURLException");
            result.add(map);
            return new ServiceResponse<>(ServiceResponse.ServiceStatusCode.ERROR, result);
        }
        catch(SocketTimeoutException error) {
            //Handles URL access timeout.
            ArrayList<HashMap<String,String>> result = new ArrayList<>();
            HashMap<String,String> map = new HashMap<>();
            map.put("error", "SocketTimeoutException");
            result.add(map);
            return new ServiceResponse<>(ServiceResponse.ServiceStatusCode.ERROR, result);
        }
        catch (IOException error) {
            //Handles input and output errors
            ArrayList<HashMap<String,String>> result = new ArrayList<>();
            HashMap<String,String> map = new HashMap<>();
            map.put("error", "IOException");
            result.add(map);
            return new ServiceResponse<>(ServiceResponse.ServiceStatusCode.ERROR, result);
        }
        catch (JSONException error) {
            //Handles input and output errors
            ArrayList<HashMap<String,String>> result = new ArrayList<>();
            HashMap<String,String> map = new HashMap<>();
            map.put("error", "JSONException");
            result.add(map);
            return new ServiceResponse<>(ServiceResponse.ServiceStatusCode.ERROR, result);
        }
        finally {
            if(client != null) {
                client.disconnect();
            }
        }
    }

    public ServiceResponse<String> deleteFriendshipRequest(String fromUsername, String toUsername) {
        HttpURLConnection client = null;

        try {
            URL url = new URL(URL + "/friendship/request/" + fromUsername + "/" + toUsername);
            client = (HttpURLConnection) url.openConnection();
            client.setRequestMethod("DELETE");
            String token = String.format("Bearer %s", LocalStorage.getToken());
            client.setRequestProperty("Authorization", token);

            client.connect();

            int statusCode = client.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED){
                return new ServiceResponse<>(ServiceResponse.ServiceStatusCode.UNAUTHORIZED);
            }

            BufferedReader br;

            if (200 <= statusCode && statusCode <= 299) {
                br = new BufferedReader(new InputStreamReader(client.getInputStream()));
            } else {
                br = new BufferedReader(new InputStreamReader(client.getErrorStream()));
            }

            StringBuilder sb = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                sb.append(output);
            }
            String result = sb.toString();

            return new ServiceResponse<>(ServiceResponse.ServiceStatusCode.SUCCESS, toUsername);

        } catch(Exception error) {
            return new ServiceResponse<>(ServiceResponse.ServiceStatusCode.ERROR, "");
        }
        finally {
            if(client != null) {
                client.disconnect();
            }
        }
    }

    public ServiceResponse<String> acceptFriendshipRequest(String fromUsername, String toUsername) {
        HttpURLConnection client = null;

        try {
            URL url = new URL(URL + "/friendship");
            client = (HttpURLConnection) url.openConnection();
            client.setRequestMethod("POST");
            client.setRequestProperty("Content-Type", "application/json");
            client.setRequestProperty("Accept", "application/json");
            String token = String.format("Bearer %s", LocalStorage.getToken());
            client.setRequestProperty("Authorization", token);

            JSONObject credentials = new JSONObject();
            credentials.put("from_username",fromUsername);
            credentials.put("to_username", toUsername);

            OutputStream outputStream = client.getOutputStream();
            outputStream.write(credentials.toString().getBytes("UTF-8"));
            outputStream.close();

            client.connect();

            int statusCode = client.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED){
                return new ServiceResponse<>(ServiceResponse.ServiceStatusCode.UNAUTHORIZED);
            }

            BufferedReader br;

            if (200 <= statusCode && statusCode <= 299) {
                br = new BufferedReader(new InputStreamReader(client.getInputStream()));
            } else {
                br = new BufferedReader(new InputStreamReader(client.getErrorStream()));
            }

            StringBuilder sb = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                sb.append(output);
            }
            String result = sb.toString();

            return new ServiceResponse<>(ServiceResponse.ServiceStatusCode.SUCCESS, fromUsername);

        } catch(Exception error) {
            return new ServiceResponse<>(ServiceResponse.ServiceStatusCode.ERROR, "");
        }
        finally {
            if(client != null) {
                client.disconnect();
            }
        }
    }

}

