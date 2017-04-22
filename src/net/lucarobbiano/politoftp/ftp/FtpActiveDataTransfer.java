package net.lucarobbiano.politoftp.ftp;

import java.io.*;
import java.net.*;

import org.apache.commons.codec.binary.*;

import net.lucarobbiano.politoftp.ftp.FtpServer.*;

class FtpActiveDataTransfer implements FtpDataTransferMode {
    private Socket socket;
    private FtpSession ftpSession;
    private TransferType type = TransferType.ASCII;

    @Override
    public TransferType getTransferType() {
        return type;
    }

    @Override
    public void setTransferType(TransferType type) {
        this.type = type;
    }

    FtpActiveDataTransfer(FtpSession ftpSession, EndPoint localEndpoint) throws IOException {
        this.ftpSession = ftpSession;
        socket = new Socket(localEndpoint.getIp(), localEndpoint.getPort());
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    @Override
    public void sendData(String data) throws IOException {
        if (type == TransferType.Binary) {
            sendData(StringUtils.getBytesUtf8(data));
        } else if (type == TransferType.ASCII) {
            sendData(StringUtils.getBytesUsAscii(data));
        }
    }

    @Override
    public void sendData(byte[] data) throws IOException {
        startTransfer();
        sendBuffer(data);
        endTransfer();
    }

    @Override
    public void sendData(InputStream dataStream, long length) throws IOException {
        startTransfer();

        long totalSent = 0;
        OutputStream socketStream = socket.getOutputStream();
        while (totalSent < length) {
            int bufsize = (length - totalSent > 1024) ? 1024 : (int) (length - totalSent);
            byte buf[] = new byte[bufsize];

            int readBytes = dataStream.read(buf);
            socketStream.write(buf, 0, readBytes);
            socketStream.flush();
            totalSent += readBytes;
        }

        endTransfer();
    }

    private void sendBuffer(byte[] data) throws IOException {
        socket.getOutputStream()
              .write(data);
        socket.getOutputStream()
              .flush();
    }

    private void startTransfer() {
        String typestr = "";
        switch (type) {
            case ASCII:
                typestr = "ASCII";
                break;
            case Binary:
                typestr = "BINARY";
                break;
        }

        ftpSession.sendMessage(150, "Opening " + typestr + " mode data connection");
    }

    private void endTransfer() throws IOException {
        ftpSession.sendMessage(226, "Transfer complete");
        close();
    }

}
