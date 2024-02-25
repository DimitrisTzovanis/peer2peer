import java.io.*;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.io.File;
import java.io.FileInputStream;


public class Peer{
    private static int TrackerPort;
    private static String Trackerip;
    //class attributes
    private int portID;
    private String ipAdress = null;
    private String directory;
    private int token_id;
    private ArrayList<String> listFiles = null;
    private ArrayList<PeerInfo> listOfUsers = null;
    boolean LoggedIn;
    private String requestedFile = null;
    File[] listOfFiles;
    ObjectInputStream objectInputStream = null;
    ObjectOutputStream objectOutputStream = null;
    DataOutputStream dataOutputStream;
    DataInputStream dataInputStream;



    public static void main(String[] args) throws IOException, ClassNotFoundException {
        int TrackerPort = 9080;
        String Trackerip = "localhost";
        String directory = "shared_directory";
        Peer peer = new Peer(TrackerPort, Trackerip, directory);
        peer.startClient();
    }

    //constructor
    Peer(int TrackerPort, String Trackerip, String directory){
        this.TrackerPort = TrackerPort;
        this.Trackerip = Trackerip;
        this.directory = directory;
        this.portID = GeneratePort();
    }

    //class methods
    void startClient() throws IOException, ClassNotFoundException {
        //diavazei ta arxeia mesa ston folder
        File folder = new File(directory);
        listOfFiles = folder.listFiles();
        File fileDownloadList = null;
        //list of txt names
        for (File file : listOfFiles) {
            if (file.getName().equals("fileDownloadList.txt")) {
                fileDownloadList= file;
                System.out.println("file found");
            }
        }


        System.out.println("peer server opened");
        Socket socket = new Socket(Trackerip, TrackerPort);
        ipAdress = getIpAdress(socket);
        try {
            //kanei thn sindesh

            System.out.println("successfully connected");
            dataOutputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            dataInputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

        }catch (Exception e){
            e.printStackTrace();
        }
        Scanner userInput = new Scanner(System.in);
        while(true){
            if(!LoggedIn){
                System.out.println("---------------------------------");
                System.out.println("Login or Register? Type  LOGIN or REGISTER");
                System.out.println("---------------------------------");
                String message = userInput.nextLine();

                dataOutputStream.writeUTF(message);        //Stelnei ston server an thelei na kanei login h register
                dataOutputStream.flush(); // send the message
                if(message.equals("REGISTER")){
                    register(userInput);
                }
                else if(message.equals("LOGIN")){
                    LoggedIn = login(userInput);
                    if(LoggedIn ){
                        inform(fileDownloadList);
                    }
                }
            }
            else{
                System.out.println("Type a Command: SEARCH, DOWNLOAD or LOGOUT");
                String message = userInput.nextLine();
                // write the message we want to send
                if(message.equals("LOGOUT")){
                    LoggedIn = logout(message);
                    socket.close();
                    break;
                }
                else if(message.equals("SEARCH")){
                    list("LIST");
                }
                else if(message.equals("DOWNLOAD")){
                    if(details("DETAILS", userInput, socket)){
                        simpledownload("SIMPLEDOWNLOAD");
                    }
                }
                else{
                    System.out.println("*WRONG COMMAND*");
                }
            }
        }
    }

    public void register(Scanner userInput) throws IOException {

        System.out.println("username:");
        String newusername = userInput.nextLine();
        dataOutputStream.writeUTF(newusername);       //stelnei ston server username
        dataOutputStream.flush(); // send the message

        System.out.println("password:");
        String newpassword = userInput.nextLine();
        dataOutputStream.writeUTF(newpassword);      //stelnei sto server password
        dataOutputStream.flush(); // send the message
        String serverreply = dataInputStream.readUTF();
        System.out.println(serverreply);
    }

    public boolean login(Scanner userInput) throws IOException{
        System.out.println("\n username:");
        String username = userInput.nextLine();
        dataOutputStream.writeUTF(username);      //stelnei ston server username
        dataOutputStream.flush(); // send the message

        System.out.println("\n password:");
        String password = userInput.nextLine();
        dataOutputStream.writeUTF(password);            //stelnei sto server password
        dataOutputStream.flush(); // send the message
        token_id = dataInputStream.readInt();
        if(token_id == 0){
            System.out.println("Unsuccessful Login");
            return false;
        }else {
            System.out.println("Successful Login");
            System.out.println("token recieved");
            return true;
        }
    }

    public void inform(File fileDownloadList) throws IOException{
        PeerServer peerServer = new PeerServer(portID, listOfFiles);
        Thread pt = new Thread(peerServer);
        pt.start();

        int bytes = 0;
        // send file size
        dataOutputStream.writeLong(fileDownloadList.length());
        // break file into chunks
        FileInputStream fileInputStream = new FileInputStream(fileDownloadList);
        byte[] buffer = new byte[4*1024];
        while ((bytes=fileInputStream.read(buffer))!=-1){
            dataOutputStream.write(buffer,0,bytes);

        }
        System.out.println("file sent");
        dataOutputStream.flush();

        dataOutputStream.writeUTF(ipAdress);            //stelnei sto server to ip tou
        dataOutputStream.flush(); // send the message
        dataOutputStream.writeInt(portID);               //stelnei sto server to port tou
        dataOutputStream.flush(); // send the message
    }

    public boolean logout(String message) throws IOException{
        dataOutputStream.writeUTF(message);//stelnei sto server to mhnima tou
        dataOutputStream.flush(); // send the message
        dataOutputStream.writeInt(token_id);//stelnei sto server to mhnima tou
        dataOutputStream.flush();
        dataInputStream.close();
        dataOutputStream.close();
        System.out.println("logged out");
        return true;
    }

    public void list(String message) throws IOException, ClassNotFoundException {
        System.out.println("recieving list of files...");
        dataOutputStream.writeUTF(message);       //stelnei sto server to mhnima tou
        dataOutputStream.flush(); // send the message

        int size = dataInputStream.readInt();
        byte[] data = new byte[size];
        dataInputStream.readFully(data);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais);
        listFiles = (ArrayList<String>) ois.readObject();

        for(String f: listFiles){
            System.out.println(f);
        }
    }
    public boolean details(String message, Scanner userInput, Socket socket) throws IOException, ClassNotFoundException {
        if(listFiles==null){
            System.out.println("There no files to show details. Type SEARCH first");
            return false;
        }
        else{
            System.out.println("which file are you looking for?");
            String name = userInput.nextLine();
            requestedFile = name;
            if(listFiles.contains(name)){

                System.out.println("recieving details of file");
                dataOutputStream.writeUTF(message);  //stelnei sto server to mhnima tou
                dataOutputStream.flush(); // send the message
                dataOutputStream.writeUTF(name);  //stelnei sto server to mhnima tou
                dataOutputStream.flush(); // send the message
                InputStream in = socket.getInputStream();
                byte[] byteArray = new byte[1024];
                int bytesRead = dataInputStream.read(byteArray);

                // Convert the byte array back to an ArrayList
                ByteArrayInputStream bais = new ByteArrayInputStream(byteArray, 0, bytesRead);
                ObjectInputStream ois = new ObjectInputStream(bais);
                listOfUsers = (ArrayList<PeerInfo>) ois.readObject(); //receives list of details

                if(listOfUsers!=null){
                    if(!listOfUsers.isEmpty()){
                        System.out.println("found peers");
                        System.out.println("Number of peers " +listOfUsers.size());
                        return true;
                    }
                    else{
                        System.out.println("no peers found");
                        return false;
                    }
                }
                else {
                    System.out.println("no peers found");
                    return false;
                }
            }
            else{
                System.out.println("The requested file does not exist in the system./nPlease check the file name.");
                return false;
            }
        }
    }

    public void simpledownload(String message) throws IOException{
        dataOutputStream.writeUTF(message);       //stelnei sto server to mhnima tou
        dataOutputStream.flush(); // send the message
        long min = 999999999;
        PeerInfo minPeerInfo = null;

        boolean found = false;
        while(found==false && !listOfUsers.isEmpty()){
            for(Iterator<PeerInfo> it = listOfUsers.iterator(); it.hasNext();){
                PeerInfo peerInfo  = it.next();

                long begin = System.nanoTime(); //metraw xrono apokrishs
                boolean f = checkActive(peerInfo.getIp_adress(), peerInfo.getPort());
                long end = System.nanoTime();
                long time = end - begin;
                double c = Math.pow(0.9,peerInfo.getCount_downloads());
                time = (long) (time * Math.pow(0.9,peerInfo.getCount_downloads()) * Math.pow(1.2, peerInfo.getCount_failures()));
                if(time<min){
                    min = time;
                    minPeerInfo = peerInfo;
                }
            }

            if(minPeerInfo!=null) {
                Socket fileSocket = new Socket(minPeerInfo.getIp_adress(), minPeerInfo.getPort());
                DataOutputStream OutputStream = new DataOutputStream(new BufferedOutputStream(fileSocket.getOutputStream()));
                DataInputStream InputStream = new DataInputStream(new BufferedInputStream(fileSocket.getInputStream()));
                OutputStream.writeUTF("download");
                OutputStream.flush();
                OutputStream.writeUTF(requestedFile);
                OutputStream.flush();
                String reply = InputStream.readUTF();
                OutputStream.flush();


                dataOutputStream.writeUTF(minPeerInfo.getIp_adress());
                dataOutputStream.flush();

                if(reply.equals("found")){
                    int bytes = 0;
                    FileOutputStream fileOutputStream = new FileOutputStream(directory+"/" + requestedFile);
                    long size = InputStream.readLong();    // read file size
                    byte[] buffer = new byte[4*1024];
                    while (size > 0 && (bytes = InputStream.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1) {
                        fileOutputStream.write(buffer,0,bytes);
                        size -= bytes;      // read upto file size
                    }

                    found = true;
                    dataOutputStream.writeUTF("success");       //stelnei sto server to mhnima tou
                    dataOutputStream.flush(); // send the message
                    dataOutputStream.writeUTF(requestedFile);
                    dataOutputStream.flush();
                    dataOutputStream.writeInt(token_id);
                    dataOutputStream.flush();

                }
                else{
                    dataOutputStream.writeUTF("failure");       //stelnei sto server to mhnima tou
                    dataOutputStream.flush(); // send the message
                    listOfUsers.remove(minPeerInfo);


                }


            }
        }
    }

    public boolean checkActive(String ip_adress, int port) throws IOException { //TODO
        Socket requSocket = null;
        DataOutputStream dos = null;
        DataInputStream dis = null;
        try{
            requSocket = new Socket(ip_adress, port);
            dos = new DataOutputStream(new BufferedOutputStream(requSocket.getOutputStream()));
            dis = new DataInputStream(new BufferedInputStream(requSocket.getInputStream()));
            dos.writeUTF("checkactive");
            dos.flush();
            String response  = dis.readUTF();

            //TODO
            //maybe add some messages for extra checking
        }catch (ConnectException serverdown) {
            return false;
        } catch (UnknownHostException eUnknownHost) {
            return false;
        } catch (IOException e) {
            return false;
        } finally {
            try {

                //TODO
                //maybe not add it here but instead in the main try
                dis.close();
                dos.close();
                if(requSocket!=null){
                    requSocket.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dis.close();
        dos.close();
        if(requSocket!=null){
            requSocket.close();
        }

        return true;
    }


    public int GeneratePort(){
        Random random = new Random();
        return random.nextInt(55001) + 10000;
    }

    private String getIpAdress(Socket socket) {
        String ipAdress = null;
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            ipAdress = localHost.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return ipAdress;
        // return (((InetSocketAddress) socket.getRemoteSocketAddress()).getAddress()).toString().replace("/","");
    }
}