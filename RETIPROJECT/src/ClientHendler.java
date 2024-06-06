

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class ClientHendler implements Runnable {
    private Socket connection;
    private String username;
    private String password;
    private String richiesta;
    private String titolo;
    private String contenuto;
    private String followed;
    private int id;
    private String id2;
    private String richiesta2;
    private int voto;
    private String MultiCastAddress;
    private int MultiCastport;
    WinsomeServer instance;
    InputStreamReader in;
    BufferedReader bf;
    PrintWriter pr;
   
    public ClientHendler(WinsomeServer instance,Socket connection,String MultiCastAddress,int Multicastport) throws IOException{
        this.connection=connection;
        this.MultiCastAddress=MultiCastAddress;
        this.MultiCastport=Multicastport;
        this.instance=instance;
        
            in = new InputStreamReader(connection.getInputStream());
           bf = new BufferedReader(in);
           pr = new PrintWriter(connection.getOutputStream());
          
    }

    @Override
    public void run() {
        while(!Thread.currentThread().isInterrupted()){
           
            try {
                richiesta=bf.readLine();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        if(richiesta.equals("login")){
           
          
                try {
                    username = bf.readLine();
                    password = bf.readLine();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                //appena un client effettua il login gli passo l'indirizzo di multicast
                pr.println(MultiCastAddress);
                pr.flush();
                pr.println(MultiCastport);
                pr.flush();
          
              int  x = instance.Login(username,password);
            
            
                 if(x==2){
                 
                    pr.println(username +" "+" loggato con successo!");
                    pr.flush();
                    try {
                        Thread.sleep(1000);
                        //aggiorno la struttura locale che mantiene i follower
                        //sucessivamente la struttura verrà aggiornata localmente,questo miserve solo per fare un backup dei suoi follower
                     instance.updateFollow(username);
                    } catch (InterruptedException | RemoteException  e) {
                        e.printStackTrace();
                    }
                    
                }
                else if(x==3){
                    pr.println("Password errata");
                    pr.flush();
                }
                else if(x==4){
                    pr.println("errore,Utente" +" "+username+" "+"non esistente");
                    pr.flush();
                }
            }
            if(richiesta.equals("list")){
                    
                    try {
                        richiesta2 = bf.readLine();
                    } catch (IOException e) {
                       
                        e.printStackTrace();
                    }
              
               
                if(richiesta2.equals("users")){
                    ArrayList<String> listausers;
                    listausers=instance.listUsers(username);
                    pr.println("Ecco la lista di utenti con almeno un tag in comune col tuo\n");
                    pr.flush();
                    for(int i=0;i<listausers.size();i++){
                        pr.println(listausers.get(i)+"\n");
                    }
                    pr.println("end");
                    pr.flush();  }
               
                else if(richiesta2.equals("following")){
                    ArrayList<String> listafollowing;
                    listafollowing=instance.listFollowing(username);
                    pr.println("Ecco la lista di utenti di cui sei follower\n");
                    pr.flush();
                    if(listafollowing.isEmpty()){
                        pr.println("Non sei follower di nessun utente");
                        pr.flush();
                        pr.println("end");
                        pr.flush();
                    }
                    else{
                        for(int i=0;i<listafollowing.size();i++){
                            pr.println(listafollowing.get(i)+"\n");
                        }
                        pr.println("end");
                        pr.flush();
                    }
                }
            }

            if(richiesta.equals("follow")){
                    
                try {
                    followed = bf.readLine();
                } catch (IOException e) { e.printStackTrace(); }

              
               int x= instance.followUser(username,followed);
               if(x==1){
                pr.println("segui gia questo utente");
                pr.flush();
               }
               else if(x==2){
                pr.println("non puoi seguire te stesso");
                pr.flush();
               }
               else{
                pr.println("ora segui "+followed);
                pr.flush();}
            }

            if(richiesta.equals("unfollow")){
                    
                try {
                    followed = bf.readLine();
                } catch (IOException e) { e.printStackTrace(); }

              
                int x=instance.unfollowUser(username,followed);
                if(x==1){
                    pr.println("non segui gia questo utente");
                    pr.flush();
                   }
                   else if(x==2){
                    pr.println("non puoi non seguire te stesso");
                    pr.flush();
                   }
                   else{
                pr.println("non segui piu' "+followed);
                pr.flush();}
            }

            if(richiesta.equals("post")){
                ArrayList<Integer> Result;
                 try {
                    titolo=bf.readLine();
                    contenuto=bf.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Result=instance.createPost(username, titolo, contenuto);
                if(Result.get(0)==1){
                    pr.println("il titolo del post non puo essere piu' di 20 caratteri");
                    pr.flush(); }
                else if(Result.get(0)==2){
                    pr.println("il contenuto del post non puo essere piu' di 20 caratteri");
                    pr.flush();  }
                else if(Result.get(0)==3){
                    pr.println("Nuovo post creato con successo! Id="+Result.get(1));
                    pr.flush();}

            }

            if(richiesta.equals("blog")){
                ArrayList<Post> userPosts;
                userPosts=instance.viewBlog(username);
                for(Post p : userPosts){
                    pr.println("Id "+p.getId());
                    pr.println("Autore "+p.getAutore());
                    pr.println("Titolo "+p.getTitolo());
                    pr.println("Contenuto "+p.getContenuto());
                    pr.println("/////////////////");
                }
                pr.println("end");
                pr.flush();
            }

            if(richiesta.equals("show")){
                try {
                    richiesta2 = bf.readLine();
                } catch (IOException e) {
                   
                    e.printStackTrace();
                }

                if(richiesta2.equals("feed")){
                    ArrayList<Post> userFeed = new ArrayList<Post>();
                    userFeed=instance.showFeed(username);
                    if(userFeed.isEmpty()){
                        pr.println("non hai alcun post nel tuo feed");
                    }
                    else{
                         for(Post p : userFeed){
                              pr.println("Id: "+p.getId());
                             pr.println("Autore: "+p.getAutore());
                             pr.println("Titolo: "+p.getTitolo());
                             pr.println("/////////////////");
                           }
                    }
                    pr.println("end");
                    pr.flush();
                }

                if(richiesta2.equals("post")){
                   
                    try {
                      
                        id2=bf.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();}
                        id = Integer.parseInt(id2);
                      
                        Post PostShowed;
                        PostShowed=instance.showPost(id);
                        if(PostShowed==null){
                            pr.println("Non eiste un post con quell'Id");
                            pr.println("end");
                            pr.flush();
                        }
                        else{
                        int votipositivi=0;
                        int votinegativi=0;
                        for(java.util.Map.Entry<String, Integer> entry : PostShowed.getVoti().entrySet()){
                            if(entry.getValue()==1) votipositivi++;
                            if(entry.getValue()==-1) votinegativi++;
                        }
                        pr.println("Id: "+PostShowed.getId());
                      
                        pr.println("Autore: "+PostShowed.getAutore());
                       
                        pr.println("Titolo: "+PostShowed.getTitolo());
                    
                        pr.println("Voti Positivi :"+votipositivi);
                      
                        pr.println("Voti Negativi :"+votinegativi);
                      
                        if(PostShowed.commenti.isEmpty()) {
                            pr.println("Commenti:0");
                            pr.println("/////////////////");
                           
                        }
                        else{
                            for(java.util.Map.Entry<String, ArrayList<String>> entry1 : PostShowed.getCommenti().entrySet()){
                                pr.println("Commenti:");
                                for(String s : entry1.getValue()){
                                pr.println(entry1.getKey()+" : "+s);}
                            }
                            pr.println("/////////////////");
                        }
                        
                        pr.println("end");
                        pr.flush();}

                }
            }

            if(richiesta.equals("delete")){
                try {
                   
                     id2=bf.readLine();
                 } catch (IOException e) {
                     e.printStackTrace();}
                     id = Integer.parseInt(id2);
                     

                    
                     int  z=instance.deletePost(username,id);
                   
                      if(z==2){
                        pr.println("Post inesistente oppure non sei l'autore del post che vuoi cancellare");
                        pr.flush();
                     }
                     if(z==1){
                        pr.println("Post con Id: "+id + " cancellato con successo!");
                        pr.flush();
                     }
            }

            if(richiesta.equals("rewin")){
                try {
                   
                     id2=bf.readLine();
                 } catch (IOException e) {
                     e.printStackTrace();}
                     id = Integer.parseInt(id2);
                     

                    
                     int  w=instance.rewinPost(username,id);
                   
                      if(w==1){
                        pr.println("Post inesistente oppure il post di cui vuoi fare il rewin non è nel tuo feed");
                        pr.flush();
                     }
                     if(w==2){
                        pr.println("Rewin del Post con Id: "+id + " eseguito con successo!");
                        pr.flush();
                     }
            }

            if(richiesta.equals("rate")){
                try {
                    
                    id2=bf.readLine();
                    richiesta2=bf.readLine();
                } catch (IOException e) {
                    e.printStackTrace();}
                   
                    id = Integer.parseInt(id2);
                    voto=Integer.parseInt(richiesta2);

                   int x = instance.ratePost(username, id, voto);

                   if(x==1){
                    pr.println("Il post con id: "+id+" non esiste");
                    pr.flush();
                   }
                   else if(x==2){
                    pr.println("Il post che vuoi votare non è nel tuo feed");
                    pr.flush();
                   }
                   else if(x==3){
                    pr.println("Non puoi votare un Post di cui sei l'autore");
                    pr.flush();
                   }
                   else if(x==4){
                    pr.println("Hai già votato questo post, non puoi votarlo ancora");
                    pr.flush();
                   }
                   else if(x==5){
                    pr.println("Hai votato correttamente il Post!");
                    pr.flush();
                   }
                    


            }

            if(richiesta.equals("comment")){
                try {
                    id2=bf.readLine();
                    richiesta2=bf.readLine();
                } catch (IOException e) {
                    e.printStackTrace();}
                   
                    id = Integer.parseInt(id2);
                   

                     int x = instance.addComment(username, id, richiesta2);

                    if(x==1){
                        pr.println("Il post con id: "+id+" non esiste");
                        pr.flush();
                       }
                       else if(x==2){
                        pr.println("Il post che vuoi commentare non è nel tuo feed");
                        pr.flush();
                       }
                       else if(x==3){
                        pr.println("Non puoi commentare un Post di cui sei l'autore");
                        pr.flush();
                       }
                       else if(x==4){
                        pr.println("Hai commentato correttamente il Post!");
                        pr.flush();
                       }

            }

            

            if(richiesta.equals("wallet")){

                try { 
                    richiesta2=bf.readLine();
                } catch (IOException e) {
                  e.printStackTrace();
                }
                if(richiesta2.equals("not"))

                {
                    HashMap<Double,Map<String,Double>> mapReturned=new HashMap<>();
                    mapReturned=instance.getWallet(username);
                    for(java.util.Map.Entry<Double, Map<String,Double>> entry : mapReturned.entrySet()){
                    pr.println("Ecco il valore in wincoin del tuo wallet: "+entry.getKey());
                    pr.println("Ecco lo storico delle transazioni:");
                        for(java.util.Map.Entry<String,Double> entry1 : entry.getValue().entrySet()){
                             pr.println("Data: "+entry1.getKey()+" Incremento: "+entry1.getValue());
                         }
                    }
                    pr.println("end");
                    pr.flush();
                   }

                   else{
                  
   
                   if(richiesta2.equals("btc")){
                       Double WalletBtc=instance.getWalletinBitcoin(username);
                       pr.println("Il tuo wallet in bitcoin è: "+WalletBtc);
                       pr.flush();
                   }
               }
          
           
               
              
            }

            if(richiesta.equals("logout")){
               
              
                pr.println(username+" scollegato");
                pr.flush();
               
                 if(connection!=null)
                 try {
                    connection.close();
                    pr.close();
                } catch (IOException e) {

                    e.printStackTrace();
                }
                 System.out.println("Client disconnesso");
                 Thread.currentThread().interrupt();
                 break;
                }
            

            }



        
    }
    
}
