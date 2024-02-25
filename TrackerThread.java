

import javax.swing.plaf.synth.SynthCheckBoxMenuItemUI;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

public class TrackerThread implements Runnable{

    //class attributes
    Socket peerSocket = null;
    private ConcurrentHashMap<String, PeerInfo> registeredPeers = null; //username, PeerInfo
    private ConcurrentHashMap<Integer, PeerInfo> contactInfo = null; //token_id, PeerInfo
    private ArrayList<String> systemFiles = null;
    private ArrayList<PeerInfo> peerInfoList = null;
    private ConcurrentHashMap<String, ArrayList<Integer>> availability = null; //filename, array of token_ids


    //in-class global variables
    DataInputStream dataInputStream = null;
    DataOutputStream dataOutputStream = null;
    ObjectInputStream objectInputStream = null;
    ObjectOutputStream objectOutputStream = null;

    //constructor
    public TrackerThread(Socket peerSocket, ConcurrentHashMap<String, PeerInfo> registeredPeers,
                         ConcurrentHashMap<Integer, PeerInfo> contactInfo, ArrayList<String> systemFiles,
                         ConcurrentHashMap<String, ArrayList<Integer>> availability) {
        this.peerSocket = peerSocket;
        this.registeredPeers = registeredPeers;
        this.contactInfo = contactInfo;
        this.systemFiles = systemFiles;
        this.availability = availability;
    }

    //run method for the thread
    @Override
    public void run() {
        try{
            dataInputStream = new DataInputStream(new BufferedInputStream(peerSocket.getInputStream()));
            dataOutputStream = new DataOutputStream(new BufferedOutputStream(peerSocket.getOutputStream()));
            boolean LoggedIn = false;
            String replyMsg;
            while(true){
                String message = dataInputStream.readUTF();
                if(message.equals("REGISTER")){
                    String user_name = dataInputStream.readUTF();
                    String password = dataInputStream.readUTF();

                    if (register(user_name, password)) {
                        replyMsg = "Registration successful. Now login.";
                        System.out.println("Peer Registeration Successful");
                    } else {
                        replyMsg = "This username already exists. Please enter a new one.";
                    }
                    dataOutputStream.writeUTF(replyMsg);
                    dataOutputStream.flush();
                }
                else if(message.equals("LOGIN")){
                    String user_name = dataInputStream.readUTF();
                    String password = dataInputStream.readUTF();
                    LoggedIn = login(user_name, password);
                    int token_id = 0;
                    if (LoggedIn) {
                        token_id = generateToken();
                        dataOutputStream.writeInt(token_id);
                        dataOutputStream.flush();
                        inform(token_id, user_name, dataInputStream);
                        System.out.println("Peer login Successful");
                    } else {
                        dataOutputStream.writeDouble(token_id);
                        dataOutputStream.flush();
                    }
                }
                else if(LoggedIn && message.equals("LOGOUT")) {
                    int token_id = dataInputStream.readInt();
                    LoggedIn = !logout(token_id);
                    dataOutputStream.writeUTF("Log out successful.");
                    dataOutputStream.flush();
                    System.out.println("peer logged out");

                    break;
                }
                else if(LoggedIn && message.equals("LIST")) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(this.reply_list());
                    byte[] data = baos.toByteArray();
                    dataOutputStream.writeInt(data.length);
                    dataOutputStream.write(data);
                    dataOutputStream.flush();
                    System.out.println("sent list to peer");
                }
                else if(LoggedIn && message.equals("DETAILS")) {
                    String filequery = dataInputStream.readUTF();
                    System.out.println(filequery);
                    ArrayList<PeerInfo> replyInfo = this.reply_details(filequery);
                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
                    objectStream.writeObject(replyInfo);
                    byte[] byteArray = byteStream.toByteArray();

                    // Send the byte array through the socket
                    OutputStream out = peerSocket.getOutputStream();
                    out.write(byteArray);
                    out.flush();
                }
                else if(LoggedIn && message.equals("SIMPLEDOWNLOAD")) {
                    Notify();

                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        finally {
            try {
                dataOutputStream.close(); // close the output stream when we're done// .
                dataInputStream.close();
                peerSocket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    //other class methods
    public boolean register(String user_name, String password){
        if(this.registeredPeers.containsKey(user_name)){
            return false;
        }
        registeredPeers.put(user_name, new PeerInfo(user_name, password, 0, 0));
        return true;
    }

    public boolean login(String user_name, String password){
        if(!this.registeredPeers.containsKey(user_name)){
            return false;
        } else {
            PeerInfo peerInfo = registeredPeers.get(user_name);
            return peerInfo.verify(user_name, password);
        }
    }

    public boolean logout(int token_id){
        PeerInfo exitingPeer = this.contactInfo.get(token_id);
        if(exitingPeer == null){
            System.out.println("peer did not logout");
            return false;
        }
        synchronized (exitingPeer) {
            exitingPeer.setIp_adress(null);
            exitingPeer.setPort(0);
        }


        this.contactInfo.remove(token_id);
        //need to also udpate the filesystem map
        Iterator<ConcurrentHashMap.Entry<String, ArrayList<Integer>>> iterator = availability.entrySet().iterator();
        while (iterator.hasNext()) {
            ConcurrentHashMap.Entry<String, ArrayList<Integer>> entry = iterator.next();
            ArrayList<Integer> list= entry.getValue();
            if(list.contains(token_id)) {
                list.removeIf(id -> id == token_id);
            }
            if(list.isEmpty()){
                synchronized (systemFiles){
                    systemFiles.remove(entry.getKey());
                }
                availability.remove(entry.getKey());
            }
        }

        return true;
    }

    public void Notify() throws IOException {

        //psaxnei ton peer me sigkekrimeno ip address
        PeerInfo PeerInfoFound = null;
        String ipadr = dataInputStream.readUTF();
        Iterator<ConcurrentHashMap.Entry<Integer, PeerInfo>> iterator = contactInfo.entrySet().iterator();
        while (iterator.hasNext()) {
            PeerInfo peerinfo = iterator.next().getValue();
            if(peerinfo.getIp_adress().equals(ipadr)) {
                PeerInfoFound = peerinfo;
            }
        }
        String result = dataInputStream.readUTF();
        if(result.equals("success")){
            PeerInfoFound.addDownload();
            System.out.println("p2p file transfer successful");
            String filesent = dataInputStream.readUTF();
            int token_id = dataInputStream.readInt();
            ArrayList<Integer> insertion = availability.get(filesent);
            insertion.add(token_id);
        }
        else{
            PeerInfoFound.addFailure();
            System.out.println("p2p file transfer not successful");
        }

    }

    public void inform(int token_id, String user_name, DataInputStream dataInputStream){
        String ip_adress = null;
        int port = 0;
        try {
            int bytes = 0;
            FileOutputStream fileOutputStream = new FileOutputStream("file_names.txt");
            long size = dataInputStream.readLong();    // read file size
            byte[] buffer = new byte[4*1024];
            while (size > 0 && (bytes = dataInputStream.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1) {
                fileOutputStream.write(buffer,0,bytes);
                size -= bytes;      // read upto file size
            }
            File file = new File("file_names.txt");
            FileReader fr = new FileReader(file);
            BufferedReader reader = new BufferedReader(fr);
            String filename;
            System.out.println("Reading text file using FileReader");
            while((filename = reader.readLine()) != null){
                if(systemFiles.contains(filename)){
                    ArrayList<Integer> tokens = availability.get(filename);
                    tokens.add(token_id);
                } else {
                    synchronized (systemFiles){
                        systemFiles.add(filename);
                    }
                    availability.put(filename, new ArrayList<>(Arrays.asList(token_id)));
                }
            }
            reader.close();
            fr.close();

            //updating the contact info
            ip_adress = dataInputStream.readUTF();
            port = dataInputStream.readInt();
            PeerInfo peerInfo = this.registeredPeers.get(user_name);
            peerInfo.setIp_adress(ip_adress);
            peerInfo.setPort(port);
            this.contactInfo.put(token_id, peerInfo);
        } catch (IOException e) {
            e.printStackTrace();
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
        } finally {}
        dis.close();
        dos.close();
        if(requSocket!=null){
            requSocket.close();
        }

        return true;
    }

    public ArrayList<String> reply_list(){ //normally returns the systemFiles arraylist
        return systemFiles;
    }

    public ArrayList<PeerInfo> reply_details(String filequery) throws IOException {
        if (availability.containsKey(filequery)){

            ArrayList<Integer> peers = availability.get(filequery);
            peerInfoList = new ArrayList<>();
            Iterator<Integer> it = peers.iterator();
            while (it.hasNext()) {
                int token = it.next();
                PeerInfo peerInfo = contactInfo.get(token);
                if (!checkActive(peerInfo.getIp_adress(), peerInfo.getPort())) {
                    System.out.println("aaaa");
                    peerInfo.setIp_adress(null);
                    peerInfo.setPort(0);
                    contactInfo.remove(token);
                    it.remove();
                } else {
                    peerInfoList.add(peerInfo);
                }
            }

            if(peers.isEmpty()) {
                synchronized (systemFiles){
                    systemFiles.remove(filequery);
                }
                availability.remove(filequery);
            }
            return peerInfoList;
        } else {
            ArrayList<PeerInfo> p1 = new ArrayList<>();
            return p1;
        }
    }

    public int generateToken() {
        int token_id;
        while(true) {
            Random rand = new Random();
            token_id = rand.nextInt(10000);
            if(!(token_id == 0 || this.contactInfo.containsKey(token_id))){
                break;
            }
        }
        return token_id;
    }

}