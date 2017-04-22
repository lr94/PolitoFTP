package net.lucarobbiano.politoftp.politoclient;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.*;

import javax.security.auth.login.*;

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.apache.http.message.*;
import org.apache.http.util.*;
import org.json.*;

public class PolitoClient {
    private String username;
    private String password;

    private HttpClient httpClient;
    private BasicCookieStore cookieStore;

    private String uuid = "";
    private String token = "";

    private List<JSONObject> courses;

    public String getUsername() {
        return username;
    }

    protected HttpClient getHttpClient() {
        return httpClient;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<Course> getCourses() {
        // For each JSON Object representing a course, create the course object
        // and add it to the list to be returned
        return courses.stream()
                      .map(jo -> new Course(this, jo))
                      .collect(Collectors.toList());
    }

    public PolitoClient(String username, String password) throws Exception {
        this.username = username;
        this.password = password;

        // Unic Device ID
        this.uuid = getMAC() + "_3";

        cookieStore = new BasicCookieStore();
        httpClient = HttpClients.custom()
                                .setDefaultCookieStore(cookieStore)
                                .setRedirectStrategy(new LaxRedirectStrategy())
                                .build();
    }

    private void login(boolean allowRecursion) throws Exception {
        // Try to login
        JSONObject loginData = new JSONObject();
        loginData.put("username", username);
        loginData.put("password", password);

        JSONObject resp = sendRequest("login.php", loginData);

        Integer status = resp.getJSONObject("esito")
                             .getJSONObject("generale")
                             .getInt("stato");

        // In case of login failure...
        if (status < 0) {
            // Check if the error was due to a unregistered device
            String error = resp.getJSONObject("esito")
                               .getJSONObject("generale")
                               .getString("error");
            if (error.equals("Invalid regID")) {
                // Register the device on the remote database
                this.saveOnDB();
                if (allowRecursion) {
                    // Try to login again (without recursion)
                    login(false);
                } else {
                    throw new Exception();
                }
                return;
            } else {
                // TODO sometimes there is a JSON parse error
                System.err.println("Errore: " + error);
                throw new LoginException();
            }
        }

        // Get the authentication token
        this.token = resp.getJSONObject("data")
                         .getJSONObject("login")
                         .getString("token");

        // Get the username (which should be the same we already knew)
        username = resp.getJSONObject("data")
                       .getJSONObject("anagrafica")
                       .getString("utente");

        // Get student information
        resp = sendRequest("studente.php", new JSONObject());

        // Get all the courses JSON data
        courses = new ArrayList<>();
        JSONArray carico_didattico = resp.getJSONObject("data")
                                         .getJSONArray("carico_didattico");
        for (int i = 0; i < carico_didattico.length(); i++) {
            JSONObject jo = carico_didattico.getJSONObject(i);
            courses.add(jo);
        }
    }

    public PolitoFilesystemEntry getFilesystemRoot() throws JSONException, Exception {
        // List of filesystem entries (will contain an entry for each course)
        List<PolitoFilesystemEntry> children = new ArrayList<>();
        // Get all the courses
        List<Course> courses = getCourses();

        // For each course add the filesystem entry to the list
        for (Course course : courses) {
            children.add(course.getFileSystemEntry());
        }

        // Create the root filesystem entry containing a directory for each
        // course
        return new PolitoFilesystemEntry("", children, null);
    }

    // Wrapper: login(boolean allowRecursion) is protected
    public void login() throws Exception {
        this.login(true);
    }

    // Register the device on the remote database
    private void saveOnDB() throws Exception {
        JSONObject data = new JSONObject();
        data.put("uuid", this.uuid);
        data.put("device_platform", System.getProperty("os.name"));
        data.put("device_version", "0.5.0");
        data.put("device_model", "PolitoFTP");
        data.put("device_manufacturer", "lr94");
        // Debug
        System.err.println("Salvataggio: " + data.toString());

        JSONObject resp = sendRequest("register.php", data);

        Integer status = resp.getJSONObject("esito")
                             .getJSONObject("generale")
                             .getInt("stato");

        if (status != 0) {
            throw new Exception();
        }
    }

    protected JSONObject sendRequest(String file, JSONObject data) throws JSONException, ParseException, IOException {
        // If the user has already logged in we need to send the authentication
        // token
        if (token != "" && !data.has("token")) {
            data.put("token", token);
        }
        // We always need to send the registration ID of the device
        if (!data.has("regID")) {
            data.put("regID", this.uuid);
        }

        String url = "https://app.didattica.polito.it/" + file;

        // Prepare JSON data to be sent
        HttpPost request = new HttpPost(url);
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("data", data.toString()));

        // Send the request
        request.setEntity(new UrlEncodedFormEntity(params));
        HttpResponse response = httpClient.execute(request);

        // Download the response and parse it
        return new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
    }

    private String getMAC() throws Exception {
        Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();

        // We are using the network, so we are supposed to have at least one
        // network interface
        if (!nis.hasMoreElements())
            throw new Exception();

        // Get the first MAC address
        NetworkInterface ni = nis.nextElement();
        byte[] mac = ni.getHardwareAddress();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            sb.append(String.format("%02X", mac[i]));
        }

        return sb.toString();
    }
}
