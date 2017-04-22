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
        return courses.stream()
                      .map(jo -> new Course(this, jo))
                      .collect(Collectors.toList());
    }

    public PolitoClient(String username, String password) throws Exception {
        this.username = username;
        this.password = password;

        this.uuid = getMAC() + "_3";

        cookieStore = new BasicCookieStore();
        httpClient = HttpClients.custom()
                                .setDefaultCookieStore(cookieStore)
                                .setRedirectStrategy(new LaxRedirectStrategy())
                                .build();
    }

    private void login(boolean allowRecursion) throws Exception {
        JSONObject loginData = new JSONObject();
        loginData.put("username", username);
        loginData.put("password", password);

        JSONObject resp = sendRequest("login.php", loginData);

        Integer status = resp.getJSONObject("esito")
                             .getJSONObject("generale")
                             .getInt("stato");

        if (status < 0) {
            String error = resp.getJSONObject("esito")
                               .getJSONObject("generale")
                               .getString("error");
            if (error.equals("Invalid regID")) {
                this.saveOnDB();
                if (allowRecursion) {
                    login(false);
                } else {
                    throw new Exception();
                }
                return;
            } else {
                System.err.println("Errore: " + error);
                throw new LoginException();
            }
        }

        this.token = resp.getJSONObject("data")
                         .getJSONObject("login")
                         .getString("token");

        username = resp.getJSONObject("data")
                       .getJSONObject("anagrafica")
                       .getString("utente");

        resp = sendRequest("studente.php", new JSONObject());

        courses = new ArrayList<>();
        JSONArray carico_didattico = resp.getJSONObject("data")
                                         .getJSONArray("carico_didattico");
        for (int i = 0; i < carico_didattico.length(); i++) {
            JSONObject jo = carico_didattico.getJSONObject(i);
            courses.add(jo);
        }
    }

    public PolitoFilesystemEntry getFilesystemRoot() throws JSONException, Exception {
        List<PolitoFilesystemEntry> children = new ArrayList<>();
        List<Course> courses = getCourses();

        for (Course course : courses) {
            children.add(course.getFileSystemEntry());
        }

        return new PolitoFilesystemEntry("", children, null);
    }

    public void login() throws Exception {
        this.login(true);
    }

    private void saveOnDB() throws Exception {
        JSONObject data = new JSONObject();
        data.put("uuid", this.uuid);
        data.put("device_platform", System.getProperty("os.name"));
        data.put("device_version", "0.5.0");
        data.put("device_model", "PolitoFTP");
        data.put("device_manufacturer", "lr94");
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
        if (token != "" && !data.has("token")) {
            data.put("token", token);
        }
        if (!data.has("regID")) {
            data.put("regID", this.uuid);
        }

        String url = "https://app.didattica.polito.it/" + file;

        HttpPost request = new HttpPost(url);
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("data", data.toString()));

        request.setEntity(new UrlEncodedFormEntity(params));
        HttpResponse response = httpClient.execute(request);

        return new JSONObject(EntityUtils.toString(response.getEntity(), "UTF-8"));
    }

    private String getMAC() throws Exception {
        Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();

        if (!nis.hasMoreElements())
            throw new Exception();

        NetworkInterface ni = nis.nextElement();
        byte[] mac = ni.getHardwareAddress();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            sb.append(String.format("%02X", mac[i]));
        }

        return sb.toString();
    }
}
