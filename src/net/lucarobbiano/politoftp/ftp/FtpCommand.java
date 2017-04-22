package net.lucarobbiano.politoftp.ftp;

import java.net.*;

class FtpCommand {
    public enum Command {
        Unknown, USER, PASS, SYST, FEAT, PWD, TYPE, PORT, LIST, QUIT, PASV, CWD, RETR;
    }

    private Command command;
    private String command_str;

    private String argument;

    Command getCommand() {
        return command;
    }

    String getArgument() {
        return argument;
    }

    public FtpCommand(String line) throws ProtocolException {
        command_str = getCommandNameFromLine(line);

        if (command_str.equals("USER")) {
            this.command = Command.USER;
            this.argument = getArgumentFromLine(line);
        } else if (command_str.equals("PASS")) {
            this.command = Command.PASS;
            this.argument = getArgumentFromLine(line);
        } else if (command_str.equals("SYST")) {
            this.command = Command.SYST;
        } else if (command_str.equals("FEAT")) {
            this.command = Command.FEAT;
        } else if (command_str.equals("PWD")) {
            this.command = Command.PWD;
        } else if (command_str.equals("TYPE")) {
            this.command = Command.TYPE;
            this.argument = getArgumentFromLine(line);
            if (!(argument.equals("I") || argument.equals("A"))) {
                throw new java.net.ProtocolException();
            }
        } else if (command_str.equals("PORT")) {
            this.command = Command.PORT;
            this.argument = getArgumentFromLine(line);
        } else if (command_str.equals("LIST")) {
            this.command = Command.LIST;
        } else if (command_str.equals("QUIT")) {
            this.command = Command.QUIT;
        } else if (command_str.equals("PASV")) {
            this.command = Command.PASV;
        } else if (command_str.equals("CWD")) {
            this.command = Command.CWD;
            this.argument = getArgumentFromLine(line);
        } else if (command_str.equals("RETR")) {
            this.command = Command.RETR;
            this.argument = getArgumentFromLine(line);
        } else {
            this.command = Command.Unknown;
        }
    }

    @Override
    public String toString() {
        return this.command_str + ((argument != null) ? " " + argument : "");
    }

    private String getCommandNameFromLine(String line) {
        int firstSpace = line.indexOf(' ');
        if (firstSpace < 0) {
            return line;
        }
        return line.substring(0, firstSpace);
    }

    private String getArgumentFromLine(String line) throws ProtocolException {
        int firstSpaceIndex = line.indexOf(' ');
        if (firstSpaceIndex == line.length() - 1) {
            throw new ProtocolException();
        }

        return line.substring(firstSpaceIndex + 1);
    }
}
