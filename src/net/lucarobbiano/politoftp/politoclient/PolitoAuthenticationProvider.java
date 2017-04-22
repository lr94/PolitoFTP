package net.lucarobbiano.politoftp.politoclient;

import net.lucarobbiano.politoftp.ftp.*;

public class PolitoAuthenticationProvider implements FtpAuthenticationProvider {

    @Override
    public boolean login(String username, String password, FtpServer ftpServer) {
        try {
            PolitoClient pClient = new PolitoClient(username, password);
            pClient.login();

            FilesystemEntry root = pClient.getFilesystemRoot();

            ftpServer.setRootFilesystemEntry(root);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
