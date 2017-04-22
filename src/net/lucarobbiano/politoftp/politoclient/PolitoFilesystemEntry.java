package net.lucarobbiano.politoftp.politoclient;

import java.io.*;
import java.text.*;
import java.text.ParseException;
import java.util.*;
import java.util.stream.*;

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.json.*;

import net.lucarobbiano.politoftp.ftp.*;

public class PolitoFilesystemEntry implements FilesystemEntry {
    private EntryType entryType;
    private String name;
    private String group = "somebody";
    private int code;
    private int sizeKb;
    private Date date;

    private List<PolitoFilesystemEntry> children;
    private PolitoFilesystemEntry parent;

    private PolitoClient politoClient;
    private Course course;

    private HttpEntity entity;

    @Override
    public EntryType getEntryType() {
        return entryType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOwner() {
        return politoClient.getUsername();
    }

    @Override
    public String getGroup() {
        return group;
    }

    protected void setGroup(String group) {
        this.group = group;
    }

    @Override
    public long getSize() {
        return ((long) sizeKb) * 1024;
    }

    @Override
    public Date getDate() {
        return date;
    }

    public String getDownloadURL() throws Exception {
        if (this.entryType != EntryType.File) {
            throw new Exception();
        }

        JSONObject request = new JSONObject();
        request.put("code", this.code);

        JSONObject response = politoClient.sendRequest("download.php", request);

        Integer status = response.getJSONObject("esito")
                                 .getJSONObject("generale")
                                 .getInt("stato");

        if (status < 0) {
            throw new FileNotFoundException();
        }

        String directUrl = response.getJSONObject("data")
                                   .getString("directurl");

        return directUrl;
    }

    @Override
    public PolitoFilesystemEntry getChild(String name) throws FileNotFoundException {
        if (this.entryType != EntryType.Directory) {
            throw new FileNotFoundException();
        }

        PolitoFilesystemEntry child = children.stream()
                                              .filter(fse -> fse.name.equals(name))
                                              .findAny()
                                              .orElse(null);
        if (child == null) {
            throw new FileNotFoundException();
        }

        return child;
    }

    @Override
    public List<? extends FilesystemEntry> getChildren() {
        return this.children;
    }

    @Override
    public PolitoFilesystemEntry getParent() {
        return parent;
    }

    protected void setParent(PolitoFilesystemEntry parent) {
        this.parent = parent;
    }

    protected PolitoFilesystemEntry(String name, List<PolitoFilesystemEntry> children, PolitoClient politoClient) {
        this.politoClient = politoClient;

        this.name = cleanName(name);
        this.entryType = EntryType.Directory;
        this.children = children;
        this.children.forEach(fse -> fse.setParent(this));
    }

    protected PolitoFilesystemEntry(String name, JSONArray children_data, Course course) throws Exception {
        this.course = course;
        this.politoClient = course.getPolitoClient();
        this.group = course.getCode();

        this.name = cleanName(name);
        this.entryType = EntryType.Directory;
        this.children = LoadChildren(children_data);
    }

    protected PolitoFilesystemEntry(JSONObject data, Course course) throws Exception {
        this.course = course;
        this.politoClient = course.getPolitoClient();
        this.group = course.getCode();

        switch (data.getString("tipo")) {
            case "DIR":
                this.name = cleanName(data.getString("descrizione"));
                this.entryType = EntryType.Directory;
                this.children = LoadChildren(data.getJSONArray("files"));
                break;
            case "FILE":
                this.name = cleanName(data.getString("nomefile"));
                this.entryType = EntryType.File;
                this.sizeKb = data.getInt("size_kb");
                this.date = parseDate(data.getString("data_ins"));
                break;
            default:
                throw new Exception();
        }

        this.code = data.getInt("code");
    }

    private List<PolitoFilesystemEntry> LoadChildren(JSONArray children_data) throws Exception {
        List<PolitoFilesystemEntry> children_list = new ArrayList<>();

        for (int i = 0; i < children_data.length(); i++) {
            JSONObject child_data = children_data.getJSONObject(i);
            PolitoFilesystemEntry child = new PolitoFilesystemEntry(child_data, this.course);
            child.setParent(this);
            children_list.add(child);
        }

        return children_list;
    }

    private String cleanName(String name) {
        char forbiddenChars[] = "<>:\"/\\|?*".toCharArray();
        for (char c : forbiddenChars) {
            name = name.replace(Character.toString(c), "");
        }

        return name;
    }

    private Date parseDate(String date) throws ParseException {
        SimpleDateFormat parser = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.ENGLISH);
        return parser.parse(date);
    }

    public String toString() {
        if (this.entryType == EntryType.File)
            return this.name;

        return this.name + ":{" + this.children.stream()
                                               .map(fse -> fse.toString())
                                               .collect(Collectors.joining(","))
                + "}";
    }

    @Override
    public long getActualSize() throws IOException {
        try {
            if (entity == null) {
                prepareDownload();
            }
            return entity.getContentLength();
        } catch (Exception e) {
            throw new IOException();
        }
    }

    @Override
    public InputStream getDataStream() throws IOException {
        try {
            if (entity == null) {
                prepareDownload();
            }
            return entity.getContent();
        } catch (Exception e) {
            throw new IOException();
        }
    }

    private void prepareDownload() throws Exception {
        HttpClient client = politoClient.getHttpClient();
        HttpGet request = new HttpGet(getDownloadURL());
        HttpResponse response = client.execute(request);

        entity = response.getEntity();
    }

}
