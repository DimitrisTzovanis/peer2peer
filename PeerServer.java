import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

// creates PeerThreads for each tracker or peer or

public class PeerServer implements Runnable{
    //class attributes
    private int portID;
    private File[] listOfFiles;
    public PeerServer(int portID, File[] listOfFiles){
        this.portID = portID;
        this.listOfFiles = listOfFiles;
    }

    //constructor

    //class methods
    public void run() {
        try{
            ServerSocket serverSocket = new ServerSocket(portID);
            while (true) {
                Socket connectionSocket =  serverSocket.accept();
                PeerThread pt = new PeerThread(connectionSocket, listOfFiles);
                Thread t = new Thread(pt);
                t.start();
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
