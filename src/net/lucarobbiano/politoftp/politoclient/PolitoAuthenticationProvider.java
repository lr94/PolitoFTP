package net.lucarobbiano.politoftp.politoclient;

import java.util.function.*;

import net.lucarobbiano.politoftp.ftp.*;

public class PolitoAuthenticationProvider implements FtpAuthenticationProvider {

    private Consumer<PolitoClient> afterLogin;

    @Override
    public boolean login(String username, String password, FtpServer ftpServer) {
        boolean valToRet;

        PolitoClient pClient = null;
        try {
            pClient = new PolitoClient(username, password);
            pClient.login();

            FilesystemEntry root = pClient.getFilesystemRoot();

            ftpServer.setRootFilesystemEntry(root);
            valToRet = true;
        } catch (Exception e) {
            valToRet = false;
        }

        if (afterLogin != null) {
            afterLogin.accept(pClient);
        }
        return valToRet;
    }

    public void setLoginCallback(Consumer<PolitoClient> consumer) {
        afterLogin = consumer;
    }

}
