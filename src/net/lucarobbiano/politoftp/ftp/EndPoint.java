package net.lucarobbiano.politoftp.ftp;

import java.net.*;
import java.util.*;
import java.util.stream.*;

class EndPoint {
    private int port;
    private InetAddress ip;

    int getPort() {
        return port;
    }

    InetAddress getIp() {
        return ip;
    }

    public EndPoint(InetAddress ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    EndPoint(String ftpEndpointString) throws UnknownHostException {
        if (!ftpEndpointString.matches("(\\d{1,3},){5}\\d{1,3}")) {
            throw new UnknownHostException();
        }

        List<Integer> byteValues = Arrays.stream(ftpEndpointString.split(","))
                                         .map(s -> Integer.parseInt(s))
                                         .collect(Collectors.toList());

        if (byteValues.stream()
                      .filter(i -> (i > 255 || i < 0))
                      .count() > 0) {
            throw new UnknownHostException();
        }

        port = byteValues.get(4) * 256 + byteValues.get(5);

        byte addr_bytes[] = new byte[4];
        for (int i = 0; i < 4; i++) {
            addr_bytes[i] = (byte) byteValues.get(i)
                                             .intValue();
        }

        ip = InetAddress.getByAddress(addr_bytes);
    }

    String toFtpEndpointString() {
        return ip.toString()
                 .substring(1)
                 .replace('.', ',')
                + "," + (int) (port / 256) + "," + port % 256;
    }

    @Override
    public String toString() {
        return ip.toString()
                 .substring(1)
                + ":" + port;
    }
}
