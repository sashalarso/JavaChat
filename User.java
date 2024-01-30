import java.io.PrintWriter;

public class User {
    String login;
    String password;
    PrintWriter out;
    boolean isOnline;
    public User(String login,String password,PrintWriter out,boolean isOnline){
        this.login=login;
        this.password=password;
        this.out=out;
        this.isOnline=isOnline;
    }
    public String getLogin(){
        return this.login;
    }
    public String getPassword(){
        return this.password;
    }
    public PrintWriter getOut(){
        return this.out;
    }
}
