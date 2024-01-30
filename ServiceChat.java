import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class ServiceChat implements Runnable {
    Socket socket;
    Scanner in;
    PrintWriter out;
    public static int NBMAXUSERS=3;
    public static int nbconnected=0;
    public static ArrayList<PrintWriter> clients=new ArrayList<>(NBMAXUSERS);
    public static ArrayList<User> users=new ArrayList<>();
    public static boolean inLoop=true; 
    public boolean isConnected=false;   
    public User currentUser;

    public ServiceChat(Socket socket) throws IOException{
        this.socket=socket;      
        
    }
    public void initStreams() throws IOException{
        this.in=new Scanner(socket.getInputStream());
        this.out= new PrintWriter(socket.getOutputStream(), true);
        clients.add(this.out);
    }
    public synchronized void broadcastMessage(String message) {
        for (PrintWriter writer : clients) {
            writer.println("<"+currentUser.getLogin()+"> "+message);
        }
    }
    public void signUp(){
        this.out.println("Entrez votre login");
        String login=in.nextLine();
        this.out.println("Entrez votre password");
        String password=in.nextLine();
        users.add(new User(login, password,this.out,false));
    }
    public boolean verifyUser(String login,String password){
        for (User user : users) {
            if (user.getLogin().equals(login) && user.getPassword().equals(password)) {
                if(user.isOnline!=true){
                this.isConnected = true;
                user.isOnline=true;
                currentUser=user;
                currentUser.out=out;
                broadcastMessage("[SYSTEM] " + currentUser.getLogin()+ " is now connected");
                out.println("Connected users : " );
                listUsers();
                return true; // 
                }
            }
        }
        return false;
    }
    public boolean connection(){
        out.println("Entrez votre login");
        String login=in.nextLine();
        out.println("Entrez votre password");
        String password=in.nextLine();
        return verifyUser(login, password);

    }
   
    public void sendMessage(){
        this.out.println("Entrez le destinataire");
        String to=in.nextLine();
        this.out.println("Entrez le message");
        String message=in.nextLine();
        int i=0;
        for (User user : users) {
            if(user.getLogin().equals(to)){
                if(user.isOnline){
                    //clients.get(i).println(message);
                    user.out.println(message);
                }                
            }
            i=i+1;
        }
    }
    public void listUsers(){
        for (User user : users) {
            if(user.isOnline){
                out.println(user.getLogin());
            }            
        }
    }
    public boolean registeredUser(String login){
        for (User user : users) {
            if(user.getLogin().equals(login)){
                return true;
            }            
        }
        return false;
    }
    public void signOut(){
        currentUser.isOnline=false;
        isConnected=false;
        for (User user : users) {
            if (user.getLogin().equals(currentUser.getLogin()) && user.getPassword().equals(currentUser.getPassword())) {
                user.isOnline=false; 
            }
        }
        broadcastMessage("[SYSTEM] " + currentUser.getLogin()+ " is now disconnected");
        
    }

    public boolean connect(){
        boolean result=false;
        out.println("Entrez votre login");
        String login=in.nextLine();
        if (!this.isConnected){
            if(registeredUser(login)){
                result=connection();
            }
            else{
                signUp();
                result=true;
            }
            
        }
        return result;
    }
    public void mainLoop() throws IOException{
        out.println("Please authenticate you first");           
        
        
        while (inLoop){
            String message=in.nextLine();
            switch(message){
                case "/broadcastMsg" :
                    if(isConnected){ 
                        out.println("Message to broadcast");
                        String tobrd=in.nextLine();
                        broadcastMessage(tobrd);
                    }
                    else{
                        out.println("Not authenticated");
                    }  
                    break;
                case "/sendToMsg" : 
                    if(isConnected){
                        sendMessage();
                    }
                    else{
                        out.println("Not authenticated");
                    }  
                    break;
                case "/listUsers": 
                    if(isConnected){
                        listUsers();
                    }         
                    else{
                        out.println("Not authenticated");
                    }           
                    break;
                case "/signUp":
                    signUp();
                    break;
                case "/signIn":
                    if (connection()){isConnected=true;};
                    nbconnected=nbconnected+1;
                    break;
                case "/signOut":
                    if(isConnected){
                        signOut();
                        nbconnected=nbconnected-1;
                    }
                    else{
                        out.println("Not authenticated");
                    }
                    break;
                case "/help":
                    out.println("Availables commands : /signIn /signUp /signOut /broadcastMsg /exit /listUsers /sendToMsg");
                    break;
                case "/exit": 
                    if(isConnected){
                        signOut();
                        nbconnected=nbconnected-1;
                    }    
                    clients.remove(out);
                    socket.close();                                 
                    inLoop=false ;                                       

                    break;
                
                default : out.println("Unrecognized command");
                break;
    
            }
        }       
        
       
    }
    @Override
    public void run() {
        
        try {
            initStreams();
        } catch (IOException e) {
            
            e.printStackTrace();
        }
        
        
        try {
            mainLoop();
        } catch (IOException e) {
            
            e.printStackTrace();
        }
        
        
        
    }
}
