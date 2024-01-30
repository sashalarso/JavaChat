import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class ClientChat extends Thread {
    
    private Socket socket;
    private BufferedReader inputConsole, inputNetwork;
    private PrintStream outputConsole, outputNetwork;

    public static void main(String[] args) {
        new ClientChat(args);
    }

    public ClientChat(String[] args) {
        initStreams(args[0],Integer.parseInt(args[1]));
        start();
        listenConsole();
    }

    public void initStreams(String adress, int port) {
        try {
            
            this.socket = new Socket(adress, port);
            this.inputNetwork = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.outputNetwork = new PrintStream(socket.getOutputStream());

           
            this.inputConsole = new BufferedReader(new InputStreamReader(System.in));
            this.outputConsole = System.out;

        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public void connect(){
        
    }

    public void run() {
        listenNetwork();
    }

    public void sendServer(String message){
        this.outputNetwork.println(message);
    }

    public void listenNetwork() {
        try {
            String message;
            while ((message = inputNetwork.readLine()) != null) {
                outputConsole.println(message);
            }
        } catch (IOException e) {
            System.out.println("Connection to server closed.");
        }
    }

    public void listenConsole() {
        try {
            String userInput;
            while ((userInput = inputConsole.readLine()) != null) {
                
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}