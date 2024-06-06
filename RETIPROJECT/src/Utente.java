

import java.util.ArrayList;

public class Utente {
        
    public String username;
    public String password;
    public ArrayList<String> taglist;

    public Utente(){
        this.username = null;
        this.password =null;
        this.taglist = new ArrayList<String>();
    }

    public Utente(String username, String password,ArrayList<String> taglist){
        this.username = username;
        this.password = password;
        this.taglist = taglist;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public ArrayList<String> getTaglist() {
        return taglist;
    }
}