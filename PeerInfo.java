import java.io.Serializable;

public class PeerInfo implements Serializable {
    //class attributes
    private String user_name;
    private String password;
    private int count_downloads;
    private int count_failures;


    //class attributes needed for the contact info
    private String ip_adress = null;
    private int port = 0;


    //constructor
    public PeerInfo(String user_name, String password, int count_downloads, int count_failures) {
        this.user_name = user_name;
        this.password = password;
        this.count_downloads = count_downloads;
        this.count_failures = count_failures;
    }

    //class methods
    public int getCount_downloads() {
        return count_downloads;
    }

    public int getCount_failures() {
        return count_failures;
    }

    public String getIp_adress() {
        return ip_adress;
    }

    public void setIp_adress(String ip_adress) {
        this.ip_adress = ip_adress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void addDownload() {
        this.count_downloads++;
    }

    public void addFailure() {
        this.count_failures++;
    }

    public boolean verify(String user_name, String password){
        if(this.user_name.equals(user_name) && this.password.equals(password)) {
            return true;
        }
        return false;
    }
}