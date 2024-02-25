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

public class PeerThread implements Runnable{
    private Socket socket;
    private File[] listOfFiles;

    PeerThread(Socket socket, File[] listOfFiles){
        this.socket = socket;
        this.listOfFiles = listOfFiles;
    }
    public void run(){

        try{
            DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            String msg = dataInputStream.readUTF();
            if(msg.equals("checkactive")){
                dataOutputStream.writeUTF("checkactive");
                dataOutputStream.flush();
            }else if (msg.equals("download")){
                File fileToSend = null;
                boolean found = false;
                dataOutputStream.flush();
                String filename = dataInputStream.readUTF();
                System.out.println(filename);
                for (File file : listOfFiles) {
                    if (file.getName().equals(filename)) {
                        found = true;
                        fileToSend = file;
                    }
                }


                if(found){
                    dataOutputStream.writeUTF("found");
                    dataOutputStream.flush();
                    int bytes = 0;
                    // send file size
                    dataOutputStream.writeLong(fileToSend.length());
                    // break file into chunks
                    FileInputStream fileInputStream = new FileInputStream(fileToSend);
                    byte[] buffer = new byte[4*1024];
                    while ((bytes=fileInputStream.read(buffer))!=-1){
                        dataOutputStream.write(buffer,0,bytes);

                    }
                    System.out.println("file sent");
                    dataOutputStream.flush();
                }else{
                    dataOutputStream.writeUTF("notfound");
                    dataOutputStream.flush();
                }

            }

        }
        catch (Exception e){
            e.printStackTrace();
        }


    }

}