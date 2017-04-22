package net.lucarobbiano.politoftp.ftp;

import java.io.*;
import java.util.*;

public interface FilesystemEntry {
    public enum EntryType {
        File, Directory
    }

    public EntryType getEntryType();

    public String getName();

    public String getOwner();

    public String getGroup();

    public long getSize();

    public long getActualSize() throws IOException;

    public InputStream getDataStream() throws IOException;

    public Date getDate();

    public FilesystemEntry getChild(String name) throws FileNotFoundException;

    public FilesystemEntry getParent();

    public List<? extends FilesystemEntry> getChildren();
}
