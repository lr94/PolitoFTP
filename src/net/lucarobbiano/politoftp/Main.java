package net.lucarobbiano.politoftp;

import net.lucarobbiano.politoftp.ftp.*;
import net.lucarobbiano.politoftp.politoclient.*;

public class Main {

    public static void main(String[] args) throws Exception {
        int port = 2021;
        if (args.length > 0 && args[0].matches("\\d+")) {
            port = Integer.parseInt(args[0]);
        }

        FtpServer ftp = new FtpServer(port);
        ftp.setAuthenticationProvider(new PolitoAuthenticationProvider());

        System.out.println("Listening on port " + port);
        ftp.listen();
    }

}
