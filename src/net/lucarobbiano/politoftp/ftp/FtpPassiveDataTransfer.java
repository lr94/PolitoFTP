package net.lucarobbiano.politoftp.ftp;

import java.io.*;
import java.net.*;

import org.apache.commons.codec.binary.*;

import net.lucarobbiano.politoftp.ftp.FtpServer.*;

class FtpPassiveDataTransfer implements FtpDataTransferMode {
    private ServerSocket serverSocket;
    private Socket connectedSocket;
    private EndPoint localEndPoint;
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

    public EndPoint getLocalEndPoint() {
        return localEndPoint;
    }

    FtpPassiveDataTransfer(FtpSession ftpSession) throws IOException {
        this.ftpSession = ftpSession;
        serverSocket = new ServerSocket(0);

        localEndPoint = new EndPoint(ftpSession.getLocalAddress(), serverSocket.getLocalPort());
    }

    void listen() throws IOException {
        connectedSocket = serverSocket.accept();
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
        OutputStream socketStream = connectedSocket.getOutputStream();
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

    @Override
    public void close() throws IOException {
        connectedSocket.close();
        serverSocket.close();
    }

    private void sendBuffer(byte[] data) throws IOException {
        connectedSocket.getOutputStream()
                       .write(data);
        connectedSocket.getOutputStream()
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
