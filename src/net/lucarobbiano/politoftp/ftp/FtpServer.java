package net.lucarobbiano.politoftp.ftp;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.stream.*;

import net.lucarobbiano.politoftp.ftp.FilesystemEntry.*;
import net.lucarobbiano.politoftp.ftp.FtpDataTransferMode.*;

public class FtpServer {
    private int port;
    protected FilesystemEntry rootFsEntry;
    private ServerSocket listener;
    private FtpAuthenticationProvider authenticationProvider;

    public int getPort() {
        return port;
    }

    public FilesystemEntry getRootFilesystemEntry() {
        return rootFsEntry;
    }

    public void setRootFilesystemEntry(FilesystemEntry rootFilesystemEntry) {
        this.rootFsEntry = rootFilesystemEntry;
    }

    public FtpAuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public void setAuthenticationProvider(FtpAuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }

    public FtpServer(int port) {
        this.port = port;
    }

    public FtpServer(int port, FilesystemEntry rootFilesystemEntry) {
        this.port = port;
        this.rootFsEntry = rootFilesystemEntry;
    }

    public void listen() throws IOException {
        listener = new ServerSocket(this.port);

        while (true) {
            Socket clientSocket = listener.accept();
            FtpSession worker = new FtpSession(clientSocket);
            Thread thread = new Thread(worker);
            thread.start();
        }
    }

    protected class FtpSession implements Runnable {
        private Socket socket;
        private Scanner scanner;
        private PrintWriter printer;

        private FilesystemEntry currentFolder;

        private String user;

        private FtpDataTransferMode.TransferType type = FtpDataTransferMode.TransferType.ASCII;
        private FtpDataTransferMode transfer;

        protected FtpServer getFtpServer() {
            return FtpServer.this;
        }

        protected InetAddress getLocalAddress() {
            return socket.getLocalAddress();
        }

        protected FtpSession(Socket clientSocket) {
            this.socket = clientSocket;
        }

        protected void sendMessage(int code, String message) {
            printer.println(code + " " + message);
        }

        protected FtpCommand receiveCommand() throws ProtocolException {
            String line = scanner.nextLine();
            FtpCommand command = new FtpCommand(line);

            return command;
        }

        @Override
        public void run() {
            try {
                scanner = new Scanner(socket.getInputStream());
                printer = new PrintWriter(socket.getOutputStream(), true);

                sendMessage(220, "Hello");

                boolean cont = true;
                while (cont) {
                    try {
                        FtpCommand command = receiveCommand();
                        cont = processCommand(command);

                    } catch (ProtocolException e) {
                        // TODO: handle exception
                        e.printStackTrace();
                        sendMessage(500, "Generic error");
                    }
                }
                scanner.close();
                printer.close();
            } catch (Exception e) {

            }
        }

        private boolean processCommand(FtpCommand command) throws IOException {
            switch (command.getCommand()) {
                case USER:
                    user = command.getArgument();
                    sendMessage(331, "Password required for " + user);
                    break;
                case PASS:
                    if (user == null) {
                        sendMessage(503, "Login with USER first");
                    } else {
                        String pass = command.getArgument();
                        boolean result = FtpServer.this.authenticationProvider.login(user, pass, FtpServer.this);
                        if (result) {
                            sendMessage(230, "User " + user + " logged in");
                            currentFolder = FtpServer.this.rootFsEntry;
                        } else {
                            sendMessage(530, "Login incorrect");
                            user = null;
                        }
                    }
                    break;
                case SYST:
                    sendMessage(215, "UNIX Type: L8");
                    break;
                case FEAT:
                    printer.println("211-Features:");
                    sendMessage(211, "End");
                    break;
                case PWD:
                    sendMessage(257, "\"" + getCurrentPath() + "\" is the current directory");
                    break;
                case CWD:
                    FilesystemEntry wd = navigateToPath(command.getArgument());
                    if (wd == null) {
                        sendMessage(550, command.getArgument() + ": No such file or directory");
                    } else if (wd.getEntryType() == EntryType.File) {
                        sendMessage(550, command.getArgument() + ": Is not a directory");
                    } else {
                        currentFolder = wd;
                        sendMessage(250, "CWD command successful");
                    }
                    break;
                case TYPE:
                    if (command.getArgument()
                               .equals("I")) {
                        type = TransferType.Binary;
                    } else {
                        type = TransferType.ASCII;
                    }
                    if (transfer != null) {
                        transfer.setTransferType(type);
                    }
                    sendMessage(200, "Type set to " + command.getArgument());
                    break;
                case PORT:
                    if (transfer != null) {
                        transfer.close();
                    }
                    transfer = new FtpActiveDataTransfer(this, new EndPoint(command.getArgument()));
                    transfer.setTransferType(type);
                    sendMessage(200, "PORT command successful");
                    break;
                case PASV:
                    if (transfer != null) {
                        transfer.close();
                    }
                    FtpPassiveDataTransfer passiveTransfer = new FtpPassiveDataTransfer(this);
                    transfer = passiveTransfer;
                    transfer.setTransferType(type);
                    sendMessage(227, "Entering Passive Mode (" + passiveTransfer.getLocalEndPoint()
                                                                                .toFtpEndpointString()
                            + ")");
                    passiveTransfer.listen();
                    break;
                case LIST:
                    if (transfer == null) {
                        sendMessage(425, "Unable to build data connection");
                    }
                    transfer.sendData(listFiles(currentFolder));
                    transfer = null;
                    break;
                case RETR:
                    FilesystemEntry fse = navigateToPath(command.getArgument());
                    if (fse == null) {
                        sendMessage(550, command.getArgument() + ": No such file or directory");
                    } else {
                        if (transfer == null) {
                            sendMessage(425, "Unable to build data connection");
                        } else {
                            long size = fse.getActualSize();
                            if (size < 0) {
                                size = fse.getSize();
                            }

                            InputStream stream = fse.getDataStream();
                            transfer.sendData(stream, size);
                            transfer = null;
                        }
                    }
                    break;
                case QUIT:
                    return false;
                default:
                    sendMessage(500, "Command not recognized");
                    break;
            }
            return true;
        }

        private String getCurrentPath() {
            FilesystemEntry currentFse = currentFolder;

            List<FilesystemEntry> list = new ArrayList<>();
            while (currentFse != null) {
                list.add(0, currentFse);
                currentFse = currentFse.getParent();
            }

            return "/" + list.stream()
                             .filter(fse -> fse.getParent() != null)
                             .map(fse -> fse.getName())
                             .collect(Collectors.joining("/"));
        }

        private FilesystemEntry navigateToPath(String path) {
            if (path.equals("/")) {
                return FtpServer.this.rootFsEntry;
            }

            FilesystemEntry relativeTo;
            if (path.startsWith("/")) {
                relativeTo = FtpServer.this.rootFsEntry;
                path = path.substring(1);
            } else {
                relativeTo = currentFolder;
            }

            String chunks[] = path.split("/");
            for (String currentChunk : chunks) {
                if (currentChunk.equals(".")) {
                    continue;
                } else if (currentChunk == "") {
                    if (relativeTo.getEntryType() != EntryType.Directory) {
                        return null;
                    }
                    continue;
                } else if (currentChunk.equals("..")) {
                    FilesystemEntry parent = relativeTo.getParent();
                    if (parent != null) {
                        relativeTo = parent;
                    }
                } else {
                    try {
                        FilesystemEntry child = relativeTo.getChild(currentChunk);
                        relativeTo = child;
                    } catch (FileNotFoundException e) {
                        return null;
                    }
                }
            }

            return relativeTo;
        }

        private String listFiles(FilesystemEntry directory) {
            StringBuilder sb = new StringBuilder();
            Iterable<? extends FilesystemEntry> children = currentFolder.getChildren();
            for (FilesystemEntry fse : children) {
                switch (fse.getEntryType()) {
                    case Directory:
                        sb.append("dr-------- " + fse.getOwner() + " " + fse.getGroup() + " " + 0 + " Jan 1 1970 "
                                + fse.getName() + "\n");
                        break;
                    case File:
                        String dateString = formatDate(fse.getDate());

                        sb.append("-r-------- " + fse.getOwner() + " " + fse.getGroup() + " " + fse.getSize() + " "
                                + dateString + " " + fse.getName() + "\n");
                        break;
                }
            }
            return sb.toString();
        }

        private String formatDate(Date date) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            int thisYear = calendar.get(Calendar.YEAR);
            calendar.setTime(date);
            int yearToCheck = calendar.get(Calendar.YEAR);

            SimpleDateFormat sDateFormat;

            if (thisYear == yearToCheck) {
                sDateFormat = new SimpleDateFormat("MMM d HH:mm", Locale.ENGLISH);
            } else {
                sDateFormat = new SimpleDateFormat("MMM d y", Locale.ENGLISH);
            }

            return sDateFormat.format(date);
        }

    }
}
