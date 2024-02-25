import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Tracker {
    private final ConcurrentHashMap<String, PeerInfo> registeredPeers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, PeerInfo> contactInfo = new ConcurrentHashMap<>();
    private final ArrayList<String> systemFiles = new ArrayList<>();
    private final ConcurrentHashMap<String, ArrayList<Integer>> availability = new ConcurrentHashMap<>();

    public static void main(String[] args){
        Tracker tracker = new Tracker();
        tracker.openServer();

    }

    public void openServer() {
        ServerSocket serverSocket = null;
        Socket peerSocket = null;
        registeredPeers.clear();
        contactInfo.clear();

        try {
            serverSocket = new ServerSocket(9080);

            while(true) {
                peerSocket = serverSocket.accept();
                System.out.println("peer connected");
                TrackerThread tt = new TrackerThread(peerSocket, registeredPeers, contactInfo, systemFiles, availability);
                Thread thread = new Thread(tt);
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}