package net.lucarobbiano.politoftp.ftp;

public interface FtpAuthenticationProvider {
    public boolean login(String username, String password, FtpServer ftpServer);
}
