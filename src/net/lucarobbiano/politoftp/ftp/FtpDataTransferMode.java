package net.lucarobbiano.politoftp.ftp;

import java.io.*;

interface FtpDataTransferMode {
    enum TransferType {
        ASCII, Binary
    }

    TransferType getTransferType();

    void setTransferType(TransferType type);

    void sendData(String data) throws IOException;

    void sendData(byte[] data) throws IOException;

    void sendData(InputStream dataStream, long length) throws IOException;

    void close() throws IOException;
}
