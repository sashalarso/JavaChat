

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.*;
import java.util.stream.Collectors;


import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import javax.crypto.Cipher;


public class ServiceChat implements Runnable {

    private static final boolean JAVACARDMODE = true;

    private Socket socket;
    private Client client;
    private final Scanner in;

    /* User commands */
    private static final String LOGOUT = "LOGOUT"; // /logout or /exit
    private static final String LIST = "LIST"; // /list
    private static final String PRIVMSG = "PRIVMSG"; // /msg username msg
    private static final String MSG = "MSG"; // msg
    private static final String SENDFILE = "SENDFILE"; //sendFile username filename

    /* Admin commands */
    private static final String KILLUSER = "KILLUSER"; // /kill username
    private static final String KILLALL = "KILLALL"; // /killall
    private static final String HALT = "HALT"; // /halt
    private static final String DELETEACCOUNT = "DELETEACCOUNT"; // /deleteAccount username
    private static final String ADDACCOUNT = "ADDACCOUNT"; // /addAccount username password
    private static final String LOADBDD = "LOADBDD"; // /loadBDD bdd.txt
    private static final String SAVEBDD = "SAVEBDD"; // /saveBDD bdd.txt

    /* States */
    private static final String ISUSERCONNECTED = "ISUSERCONNECTED";

    private final int NBMAXUSERCONNECTED;
    private boolean fileTransferMode = false;
    private PrintWriter outRetrievingClient;

    public static final Map<String, PrintWriter> connectedClients = new HashMap<>();
    public static List<Client> registeredClients = new LinkedList<>();

    private KeyFactory factory;
	private PublicKey pub;
    private final int DATASIZE = 128;	
    

    public ServiceChat(Socket socket, int NBMAXUSERCONNECTED) throws IOException {
        this.socket = socket;
        this.NBMAXUSERCONNECTED = NBMAXUSERCONNECTED;
        this.in = new Scanner(socket.getInputStream());
        this.client = new Client(new PrintWriter(socket.getOutputStream(), true), socket);
    }

    public ServiceChat(int NBMAXUSERCONNECTED) { // Admin
        this.NBMAXUSERCONNECTED = NBMAXUSERCONNECTED;
        this.in = new Scanner(System.in);
        this.client = new Client(new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true));

        // development purpose
        //loadBdd("/loadbdd users.db");
    }

    

    private void displayBytes(byte[] bytes){
		int i = 0;
		for (byte b : bytes) {
			System.out.printf("%02X ", b);
			if (++i%8 == 0)
				System.out.println("");
		}
	}

    private synchronized boolean userConnectedLimitReachedCheck() throws IOException {
        if (ServiceChat.connectedClients.size() >= NBMAXUSERCONNECTED){
            this.client.getOut().println("<SYSTEM> User connected limit reached");
            this.client.getOut().close();
            this.socket.close();
            return true;
        }
        return false;
    }

    private synchronized void broadcastMessage(String username, String msg, boolean system){
        final String x = system ? "<" + username + ">" + " " + msg : "[" + username + "]" + " " + msg;
        for(Map.Entry<String, PrintWriter> client : connectedClients.entrySet())
            client.getValue().println(x);
        
    }

    private synchronized void isUserConnected(String raw) {
        String[] splitRaw = raw.split(" ");
        boolean isfound = false;
        for (Map.Entry<String, PrintWriter> retrievingClient : connectedClients.entrySet()){
            if (splitRaw[3].equals(retrievingClient.getKey())) {
                this.client.getOut().println("<SYSTEM> [SENDFILE]: User is connected: " + splitRaw[3] + " sending: " + splitRaw[4]);
                isfound = true;
            }
        }
        if(!isfound)
            this.client.getOut().println("<SYSTEM> [SENDFILE]: User is not connected");
    }

    private synchronized boolean usernameExists(String username){
        boolean exist = false;

        for (Client client: registeredClients)
            if (client.getUsername().equals(username)) {
                exist = true;
                break;
            }
        return exist;
    }

    private synchronized boolean authentication() throws IOException {
        this.client.getOut().println("<SYSTEM> Welcome!");
        this.client.getOut().println("<SYSTEM> Enter your username");
        String username = this.in.nextLine().trim();

        if(!username.toLowerCase(Locale.ROOT).equals("admin")) {
            if (!usernameExists(username)) {
                register(username);
                return false; // false = register
            } else {
                login(username);
                return true;
            }

        } else {
            this.client.setUsername("Forbidden username");
            logout(this.client);
            return false;
        }
    }

    

   

    

    

    private synchronized void register(String username) throws IOException {
        this.client.getOut().println("<SYSTEM> Register");
        this.client.setUsername(username);

        String password = "";
        String confirmPassword = " ";
        while(!password.equals(confirmPassword)){
            this.client.getOut().println("Enter password: ");
            password = this.in.nextLine().trim();
            this.client.getOut().println("Confirm password: ");
            confirmPassword = this.in.nextLine().trim();
        }
        this.client.setPassword(password);

        registeredClients.add(this.client);

        
        this.client.getOut().println("<SYSTEM> Registration Successful");
        
    }

    private synchronized void login(String username) throws IOException {
        this.client.getOut().println("<SYSTEM> Connecting...");
        int isFound = -1;

        this.client.getOut().println("Enter password: ");
        String password = this.in.nextLine();

        for (Client client : registeredClients)
            if (client.getUsername().equals(username) && client.getPassword().equals(password))
                isFound = registeredClients.indexOf(client);

        if (isFound == -1) {
            this.client.getOut().println("<SYSTEM> Username or password is incorrect");
            this.client.getOut().close();
            this.socket.close();
            return;
        }

        if(!userConnectedLimitReachedCheck()) {
            if (connectedClients.containsKey(username)) {
                this.client.getOut().println("<SYSTEM> User already connected");
                
                this.client.getOut().close();
                this.socket.close();
            } else {
                registeredClients.get(isFound).setOut(this.client.getOut()); // update printwriter
                registeredClients.get(isFound).setSocket(this.client.getSocket()); // update socket
                this.client = registeredClients.get(isFound);

                this.client.getOut().println("<SYSTEM> Connected as: " + this.client.getUsername());
               
                broadcastMessage("SYSTEM", this.client.getUsername() + " is now connected!", true);

                connectedClients.put(this.client.getUsername(), this.client.getOut());
                listClients();
            }
        }
    }

    private synchronized void logout(Client client) throws IOException {
        try {
            for (Client rc: registeredClients) 
                if (client.getUsername().equals(rc.getUsername())) 
                    for (Map.Entry<String, PrintWriter> retrievingClient : connectedClients.entrySet())
                        if (client.getUsername().equals(retrievingClient.getKey())) {
                            connectedClients.remove(client.getUsername());
                            client.getOut().println("<SYSTEM> Disconnecting...");
                            
                            broadcastMessage("SYSTEM", client.getUsername() + " is now disconnected!", true);
                            client.getOut().close();
                            client.getSocket().close();
                        }
        } catch (Exception e) {
        }
    }

    private synchronized void listClients(){
        System.out.println("test");
        this.client.getOut().println("<SYSTEM> [LIST]: Connected users: ");
        this.client.getOut().println("-----------------------------");
        for(Map.Entry<String, PrintWriter> client : connectedClients.entrySet()) {
            String key = client.getKey();
            this.client.getOut().print("<" + key + "> ");
        }
        this.client.getOut().println("\n-----------------------------");
   
    }



    private synchronized void privateMessage(String raw) {
        String[] splitRaw = raw.split(" ");

        if(splitRaw.length == 3 && !splitRaw[1].equals(this.client.getUsername())) {
            for (Client rc: registeredClients) {
                if (splitRaw[1].equals(rc.getUsername())) {
                    for (Map.Entry<String, PrintWriter> client : connectedClients.entrySet()) {
                        if (rc.getUsername().equals(client.getKey())) {
                            client.getValue().println("[From: " + this.client.getUsername() + "]" + " " + splitRaw[2]);
                            
                            return;
                        }
                    }
                    this.client.getOut().println("<SYSTEM> [PRIVMSG]: Private message cancelled, " + rc.getUsername() + " is not connected");
                    return;
                }
            }
        }
        this.client.getOut().println("<SYSTEM> [PRIVMSG]: Private message cancelled");
    }

    private synchronized void saveBdd(String raw) {
        if (raw.split(" ").length != 2) {
            System.out.println("<SYSTEM> Syntax error: Use: /savebdd <database.db>");
            return;
        }

        String bddFile = "../out/secureApp/server/Databases/" + raw.split(" ")[1];
        try {
            new File(bddFile).delete();

            BufferedWriter writer = new BufferedWriter(new FileWriter(bddFile));
            int cpt = 0;
            for (Client client: registeredClients) {
                ++cpt;
                writer.write(client.getUsername() + ":" + client.getRSAExponent() + ":" + client.getRSAModulus() + "\n");
            }
            writer.close();
            
        } catch (IOException e) {
            e.getMessage();
        }
    }

    private synchronized void loadBdd(String raw) {
        if (raw.split(" ").length != 2) {
            System.out.println("<SYSTEM> Syntax error: Use: /loadbdd <database.db>");
            return;
        }

        String bddFile = "../out/secureApp/server/Databases/" + raw.split(" ")[1];
        try {
            BufferedReader reader = new BufferedReader(new FileReader(bddFile));
            String line; int cpt = 0;
            while ((line = reader.readLine()) != null) {
                ++cpt;
                String[] splitLine = line.split(":");
                registeredClients.add(new Client(splitLine[0], splitLine[1], splitLine[2]));
            }
            registeredClients = registeredClients.stream().distinct().collect(Collectors.toList());
            reader.close();
            

        } catch (IOException e) {
            e.getMessage();
        }
    }

    private synchronized void addAccount(String raw) {
        String[] splitRaw = raw.split(" ");
        Optional<Client> c = registeredClients.stream().filter(cl -> cl.getUsername().equals(splitRaw[1])).findFirst();
        if (c.isPresent()){}
          
        else {
            registeredClients.add(new Client(splitRaw[1], splitRaw[2]));
           
        }
    }

    private synchronized void deleteAccount(String raw) throws IOException {
        String[] splitRaw = raw.split(" ");

        for (Client client: registeredClients) {
            if (splitRaw[1].equals(client.getUsername())){
                if(connectedClients.containsKey(client.getUsername()))
                    logout(client);
                registeredClients.remove(client);
               

                return;
            }
        }
       

    }

    private synchronized void haltServer() throws IOException {
        killAll();
       

        this.client.getOut().close();
        System.exit(0);
    }

    private synchronized void killAll() throws IOException {
        int i = 0;
        for (String username: connectedClients.keySet().stream().toList()) {
           
            Optional<Client> c = registeredClients.stream().filter(cl -> cl.getUsername().equals(username)).findFirst();
            if (c.isPresent())
                logout(c.get());
        }
        
    }

    private synchronized void killUser(String raw) throws IOException {
        String[] splitRaw = raw.split(" ");

        for(Map.Entry<String, PrintWriter> client : connectedClients.entrySet())
            if (splitRaw[1].equals(client.getKey())) {
               
                Optional<Client> c = registeredClients.stream().filter(cl -> cl.getUsername().equals(client.getKey())).findFirst();
                if(c.isPresent())
                    logout(c.get());
                return;
            }
        
    }

    private synchronized void sendFile(String raw) { // /sendfile user file
        if(!fileTransferMode) { // not in transfer mode
            String[] splitRaw = raw.split(" ");

            boolean userFound = false;
            for (Map.Entry<String, PrintWriter> retrievingClient : connectedClients.entrySet()) {
                if (splitRaw[1].equals(retrievingClient.getKey())) {
                    userFound = true;
                   
                    this.client.getOut().println("<SYSTEM> [SENDFILE]: Sending " + splitRaw[2] + " to user: " + retrievingClient.getKey());
                    this.outRetrievingClient = retrievingClient.getValue();
                    this.outRetrievingClient.println("<SYSTEM> [SENDFILE]: SENDFILESTART: Retrieving " + splitRaw[2] + " from " + this.client.getUsername());
                    this.fileTransferMode = true;
                    break;
                }
            }
            if(!userFound){
                
                this.client.getOut().println("<SYSTEM> [SENDFILE]: Sending failed, User: " + splitRaw[1] + " is not connected");
            }
        } else { // in transfer mode
            if (raw.equals("<SYSTEM> [SENDFILE]: SENDFILESTOP")){
                this.outRetrievingClient.println("<SYSTEM> [SENDFILE]: SENDFILESTOP");
                this.client.getOut().println("<SYSTEM> [SENDFILE]: File Sent");
                
                this.fileTransferMode = false;
            } else {
                if (raw.startsWith("<SYSTEM> [SENDFILE]"))
                    this.outRetrievingClient.println(raw);
                else
                    broadcastMessage(this.client.getUsername(), raw, false);
            }
        }
    }

    private String commandParser(String text){
        if(text.startsWith("<SYSTEM> [SENDFILE]: ISUSERCONNECTED"))
            return ISUSERCONNECTED;
        else{
            System.out.println(text.split(" ")[0].toLowerCase(Locale.ROOT));
            switch (text.split(" ")[0].toLowerCase(Locale.ROOT)) {
                
                case "/exit", "/logout" -> {return LOGOUT;}
                case "/list" -> {return LIST;}
                case "/msg" -> {return PRIVMSG;}
                case "/sendfile"->{return this.client.isAdmin() ? MSG : SENDFILE;}
                case "/kill" -> {return this.client.isAdmin() ? KILLUSER : MSG;}
                case "/killall" ->{return this.client.isAdmin() ? KILLALL : MSG;}
                case "/halt" ->{return this.client.isAdmin() ? HALT : MSG;}
                case "/deleteaccount"->{return this.client.isAdmin() ? DELETEACCOUNT : MSG;}
                case "/addaccount"->{return this.client.isAdmin() ? ADDACCOUNT : MSG;}
                case "/loadbdd"->{return this.client.isAdmin() ? LOADBDD : MSG;}
                case "/savebdd"->{return this.client.isAdmin() ? SAVEBDD : MSG;}
                default -> {return MSG;}
            }
        }
    }

    @Override
    public void run() {

        try {
            if (this.client.isAdmin() || (JAVACARDMODE && authentication())) {
                while (this.in.hasNextLine()) {
                    String raw = this.in.nextLine().trim();

                    if(fileTransferMode)
                        sendFile(raw);
                    else {
                        switch (commandParser(raw)) {
                            case LOGOUT -> {
                                if (!this.client.isAdmin()) {
                                    logout(this.client);
                                    return;
                                }
                            }
                            case LIST -> listClients();
                            case PRIVMSG -> privateMessage(raw);
                            case MSG -> broadcastMessage(this.client.getUsername(), raw, false);
                            case KILLUSER -> killUser(raw);
                            case KILLALL -> killAll();
                            case HALT -> haltServer();
                            case DELETEACCOUNT -> deleteAccount(raw);
                            case ADDACCOUNT -> addAccount(raw);
                            case LOADBDD -> loadBdd(raw);
                            case SAVEBDD -> saveBdd(raw);
                            case SENDFILE -> sendFile(raw);
                            case ISUSERCONNECTED -> isUserConnected(raw);
                        }
                    }
                }
            }
            logout(this.client);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}