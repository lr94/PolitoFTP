package net.lucarobbiano.politoftp;

import net.lucarobbiano.politoftp.ftp.*;
import net.lucarobbiano.politoftp.politoclient.*;

public class Main {

    public static void main(String[] args) throws Exception {
        int port = 2021;
        if (args.length > 0 && args[0].matches("\\d+")) {
            port = Integer.parseInt(args[0]);
        }

        PolitoAuthenticationProvider politoAuth = new PolitoAuthenticationProvider();
        politoAuth.setLoginCallback(p -> {
            if (p != null) {
                if (p.isLoggedIn()) {
                    System.out.println(p.getUsername() + ": logged in.");
                } else {
                    System.out.println(p.getUsername() + ": login failure.");
                }
            } else {
                System.out.println("Error");
            }
        });

        FtpServer ftp = new FtpServer(port);
        ftp.setAuthenticationProvider(politoAuth);

        System.out.println("PolitoFTP 0.5.0-alpha");

        System.out.println("Listening on port " + port);
        ftp.listen();
    }

}
