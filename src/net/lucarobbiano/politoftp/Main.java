package net.lucarobbiano.politoftp;

import java.text.*;
import java.util.*;

import net.lucarobbiano.politoftp.ftp.*;
import net.lucarobbiano.politoftp.politoclient.*;

public class Main {

    public static void main(String[] args) throws Exception {
        System.out.println(new SimpleDateFormat("MMM d y", Locale.ENGLISH).format(new Date()));

        FtpServer ftp = new FtpServer(12021);
        ftp.setAuthenticationProvider(new PolitoAuthenticationProvider());
        ftp.Listen();
    }

}
