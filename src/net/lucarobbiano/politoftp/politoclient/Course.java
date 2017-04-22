package net.lucarobbiano.politoftp.politoclient;

import java.io.*;

import org.apache.http.*;
import org.json.*;

public class Course {
    private PolitoClient politoClient;

    private String code;
    private String name;
    private int id_inc;

    private JSONObject courseData;

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    protected PolitoClient getPolitoClient() {
        return politoClient;
    }

    protected Course(PolitoClient politoClient, JSONObject data) {
        this.politoClient = politoClient;

        this.code = data.getString("cod_ins");
        this.name = data.getString("nome_ins_1");
        this.id_inc = data.getInt("id_inc_1");
    }

    public void getData() throws JSONException, ParseException, IOException {
        if (courseData != null) {
            return;
        }

        JSONObject request = new JSONObject();
        request.put("incarico", this.id_inc);
        request.put("cod_ins", this.code);

        JSONObject response = politoClient.sendRequest("materia_dettaglio.php", request);

        this.courseData = response;
    }

    public PolitoFilesystemEntry getFileSystemEntry() throws JSONException, Exception {
        if (courseData == null) {
            getData();
        }

        // this.code + " - " +
        return new PolitoFilesystemEntry(this.name, courseData.getJSONObject("data")
                                                              .getJSONArray("materiale"),
                this);
    }
}
