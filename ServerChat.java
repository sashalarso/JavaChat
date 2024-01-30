import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ServerChat{
    int port;
    
    
    public ServerChat(int port) throws IOException{
        this.port=port;
        ServerSocket server = new ServerSocket(port);
        while(true){
            
            Socket connection = server.accept();
            
            System.out.println("new client connected");
            ServiceChat service=new ServiceChat(connection);
            Thread clientThread = new Thread(service);            
            clientThread.start();
            
        }
    }

    public static void main(String[] argv) throws IOException{
        new ServerChat(9999);
    }

    

}