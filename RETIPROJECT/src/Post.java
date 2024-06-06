


import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Post {

    public int Id=1;
    public String autore;
    public String titolo;
    public String contenuto;
    public Map<String,ArrayList<String>> commenti;
    public Map<String,Integer> voti;
    public boolean rewinPosts;
    public int iterated=1;

    public Post(String autore,String titolo,String contenuto){
        
        this.autore=autore;
        this.titolo=titolo;
        this.contenuto=contenuto;
        this.commenti=new ConcurrentHashMap<String,ArrayList<String>>();
        this.voti=new ConcurrentHashMap<String,Integer>();
        this.rewinPosts=false;
        
    }

    public Post(){
    }

    public void setrewinPosts(boolean b){
        rewinPosts=b;
    }

    public boolean getrewinPosts(){
        return rewinPosts;
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public String getAutore() {
        return autore;
    }

    public void setAutore(String autore) {
        this.autore = autore;
    }

    public String getTitolo() {
        return titolo;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public String getContenuto() {
        return contenuto;
    }

    public void setContenuto(String contenuto) {
        this.contenuto = contenuto;
    }

    public Map<String, ArrayList<String>> getCommenti() {
        return commenti;
    }

    public void setCommenti(Map<String, ArrayList<String>> commenti) {
        this.commenti = commenti;
    }

    public Map<String, Integer> getVoti() {
        return voti;
    }

    public void setVoti(Map<String, Integer> voti) {
        this.voti = voti;
    }
    
}
