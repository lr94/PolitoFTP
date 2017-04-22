package net.lucarobbiano.politoftp;

import net.lucarobbiano.politoftp.ftp.*;
import net.lucarobbiano.politoftp.politoclient.*;

public class Main {

    public static void main(String[] args) throws Exception {
        FtpServer ftp = new FtpServer(12021);
        ftp.setAuthenticationProvider(new PolitoAuthenticationProvider());
        ftp.Listen();
    }

}
