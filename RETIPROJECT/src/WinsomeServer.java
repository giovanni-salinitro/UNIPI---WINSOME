


import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.text.SimpleDateFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;



import java.util.*;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;




public class WinsomeServer extends RemoteObject implements WinsomeServerInterface{
    
    //file utenti registrati
    private static File Registrati = new File("utentiRegistrati.json");
    //file dei followers
    private static File Followers = new File("followers.json");
    //file dei following
    private static File following =new File("following.json");
    //file dei post
    private static File Post=new File("post.json");
    //file dei wallets
    private static File Wallets=new File("wallets.json");
    //file dello storico delle transazioni
    private static File Transazioni = new File("transazioni.json");
    //file dei voti conteggia per ogni post
    private static File VotiConteggiati = new File ("votiConteggiati.json");
    //file dei commenti conteggiati per ogni post
    private static File CommentiConteggiati = new File ("commentiConteggiati.json");

    //struttura dati per capire di ogni post quali voti ho gia conteggiato
    //key -> idPost
    //value -> Lista di utenti che hanno votato il post con id=idPost che ho conteggiato
    private static ConcurrentHashMap<Integer,ArrayList<String>> votiConteggiati = new ConcurrentHashMap<Integer,ArrayList<String>>();

    //struttura dati per capire di ogni post quanti commenti ho gia conteggiato
    //key-> idPost
    //value -> Map:key ->username utente
    //             value ->lista di commenti conteggiati
    private static ConcurrentHashMap<Integer,ConcurrentHashMap<String,ArrayList<String>>> commentiConteggiati = new ConcurrentHashMap<Integer,ConcurrentHashMap<String,ArrayList<String>>>();


    //struttura dati che tiene uno storico delle transazioni per ogni utente
    private static ConcurrentHashMap<String,Map<String,Double>> transazioni = new ConcurrentHashMap<String,Map<String,Double>>();

    //struttura dati che tiene conto del portafoglio di ogni utente registrato
    private static ConcurrentHashMap<String,Double> wallets = new ConcurrentHashMap<String,Double>();


    //Struttura dati che contiene tutti i post dei vari utenti
    private static ConcurrentHashMap<String,ArrayList<Post>> post = new ConcurrentHashMap<String,ArrayList<Post>>();

   
    
    //Struttura dati che contiene tutti gli utenti che si sono registrati al social
    private static ArrayList<Utente> utentiRegistrati = new ArrayList<Utente>();

    //hashmap che mappa utente e corrispettivi utenti che hanno almeno un tag in comune con lui
    /*key -> username utente
     * values -> lista utenti con almeno un tag in comune con l'utente key
     */
    private static ConcurrentHashMap<String,ArrayList<String>> listUsers = new ConcurrentHashMap<>();

     //hashmap che mappa utente e corrispettivi utenti di cui è follower
    /*key -> username utente
     * values -> lista utenti di cui è follower
     */
    private static ConcurrentHashMap<String,ArrayList<String>> followingMap = new ConcurrentHashMap<>();


     //hashmap degli utenti e dei corrispettivi followers
    /*key -> username utente
     * values -> lista followers dell'utente
     */
    private  static ConcurrentHashMap<String,ArrayList<String>> followersMap = new ConcurrentHashMap<>();


     /* map dei client registrati al servizio RMICallback 
      * key -> WinsomeClientInterface
      value-> corrispettivo username
     */
    private static HashMap<WinsomeClientInterface,String> clients;

   //ReentrantReadWriteLock per le lock di read/write nei vari metodi implementati
    private ReentrantReadWriteLock ReadWriteLock=new ReentrantReadWriteLock();
    private Lock write = ReadWriteLock.writeLock();
    private Lock read= ReadWriteLock.readLock();

    
    //WinsomeServer ad ogni riavvio deve recuperare tutti i dati degli utenti precedenti.
    public WinsomeServer() throws RemoteException{
        super();
        clients = new HashMap<WinsomeClientInterface,String>( );
       recuperodati();
    }

    //metodo per rimuovere dall'HashMap dei client registrati al servizio di Callback un determinato client
    public synchronized void unregisterForCallback(WinsomeClientInterface Client,String username) throws RemoteException  {
        write.lock();
        if (clients.containsKey(Client))   
        {   
            clients.remove(Client, username);
            System.out.println("Client unregistered");}
            else { System.out.println("Unable to unregister client.");  }
        write.unlock();
    }

    //metodo per registare un determinato client al servizio di callback
    public synchronized void registerForCallback(WinsomeClientInterface ClientInterface,String username) throws RemoteException  {
        write.lock();
        if (!clients.containsKey(ClientInterface))
             { clients.put( ClientInterface,username);
             System.out.println("nuovo client registrato." );}
        write.unlock();
    }

    //metodo per aggiornare all'avvio la struttura dati locale del client per vedere la lista di follower che ha 
    public synchronized void updateFollow(String s) throws RemoteException{  
        read.lock();     
        for(java.util.Map.Entry<WinsomeClientInterface,String> entry : clients.entrySet()){
                WinsomeClientInterface client = entry.getKey();
               client.updateMapCallback(followersMap);
            }
        read.unlock();
}


      /** Metodo usato per la Registrazione 
     * @param username Identificatore univoco del nome di un account. 
     * @param password Password del account.
     * @param tag Una lista di massimo 5 tag.
     * @return Ritorna un int (x) che indica:
     * x = 1 -> formato parametri registrazione errato
     * x = 2 -> username già in uso
     * x = 3 -> registrazione avvenuta con successo 
     */
    public synchronized int Registrazione(String username, String password, ArrayList<String> tag)throws RemoteException{
       write.lock();
        if(password == null ||   username == null ||   tag.size() > 5){
            write.unlock();
            return 1;
        }
    
        Utente y = new Utente(username,password,tag);
   
        for(Utente u : utentiRegistrati){
            if(u.username.equals(y.username)){
            write.unlock();
            return 2;}
         }
            
       
       //se sono qui la registrazione è andata a buon fine,quindi lo aggiungo nel mio file di utenti registrati
       //e lo inizializzo anche nelle mie map di followers,following;
       //lo aggiungo anche nel mio file dei wallet assegnandogli un wallet=0 e nel mio file transazioni(con ancora ovviamente 0 transazioni)
       utentiRegistrati.add(y);
       scritturaUtentiRegistrati();
       followersMap.putIfAbsent(y.username, new ArrayList<String>());
       scritturaFollowers();
       followingMap.putIfAbsent(y.username, new ArrayList<String>());
       scritturaFollowing();
       Double d=0.00;
       wallets.putIfAbsent(y.username, d);
       scritturaWallets();
       transazioni.putIfAbsent(y.username, new ConcurrentHashMap<String,Double>());
       scritturaTransazioni();
      
      write.unlock();
       return 3;
      

    }

     /*Funzione per la login;prende come parametri l username e password che vuole effettuare il login e il file degli utenti registati per controllare se è registrato
       torna un valore intero (x) che indica:
     * x=2 -> login effetuato con succeso
     * x=3 -> password errata
     * x=4 -> utente non esistente
     */
    public synchronized  int Login(String username,String password){
       read.lock();
       
        for(Utente u : utentiRegistrati){
                   
            if(u.username.equals(username) && u.password.equals(password)){
                  read.unlock();
                  return 2;}
             else if(u.username.equals(username) && !u.password.equals(password)) {
                read.unlock();
             return 3;}
          }


        read.unlock();
        return 4;
    }


   
  //metodo usato per la richiesta di list users
  //username non può essere null
    public synchronized ArrayList<String> listUsers(String username) throws NullPointerException{
      write.lock();

        Utente x=new Utente();
        for(Utente u : utentiRegistrati){
            if(u.username.equals(username))
             x=u;}
        for(Utente u : utentiRegistrati){
          if(!x.username.equals(u.username)) {
          for(int i=0;i<u.taglist.size();i++){
            if(x.taglist.contains(u.taglist.get(i))){
               if(!listUsers.containsKey(x.username)){
                listUsers.put(x.username, new ArrayList<String>());
                listUsers.get(x.username).add(u.username); }
               else{ listUsers.get(x.username).add(u.username);}
          }
        }
     }
    }
    write.unlock();
    return listUsers.get(x.username);
        
       
    }

    //metodo usato per la richiesta follow user
    //followed=utente followato
    //follower=utente che è diventato follower dell'username followed
    //ritorna 1 se follower segue gia followed
    //ritorna 2 se follower e followed sono uguali
    //ritorna 3 altrimenti
    public synchronized int followUser(String follower,String followed) throws NullPointerException{
      write.lock();
        if( follower.equals(followed)){
             write.unlock();
             return 2;
         }
        else if((followersMap.get(followed).contains(follower)) ){
            write.unlock();
            return 1;
        }
        else if( follower.equals(followed)){
            write.unlock();
            return 2;
        }
        else if(!(followersMap.get(followed).contains(follower)) && !follower.equals(followed)){
           
            followersMap.get(followed).add(follower);
           
            scritturaFollowers();

            for(java.util.Map.Entry<WinsomeClientInterface,String> entry : clients.entrySet()){
                    if(entry.getValue().equals(followed)){
                        WinsomeClientInterface client = entry.getKey();
                        try {
                            client.notifyFollower(follower, followed);
                        } catch (RemoteException e) {
                           
                            e.printStackTrace();
                        }
                    }
            }
         }
         if(!followingMap.get(follower).contains(followed)){
                followingMap.get(follower).add(followed);
                scritturaFollowing();}    
         write.unlock();
         return 3;
    }

    //metodo per la richiesta di unfollow
    //ritorna 1 se l'unfollower non segue gia unfollowed
    //ritorna 2 unfollowed e unfollower sono la stessa persona
    //ritorna 3 altrimenti
    public synchronized int unfollowUser(String unfollower,String unfollowed){
        write.lock();
         if(unfollower.equals(unfollowed)){
            write.unlock();
            return 2;
        }
        else if(!followersMap.get(unfollowed).contains(unfollower)){
            write.unlock();
            return 1;
        }
      
        if((followersMap.get(unfollowed).contains(unfollower))){
           
            followersMap.get(unfollowed).remove(unfollower);
           
            scritturaFollowers();

            for(java.util.Map.Entry<WinsomeClientInterface,String> entry : clients.entrySet()){
                    if(entry.getValue().equals(unfollowed)){
                        WinsomeClientInterface client = entry.getKey();
                        try {
                            client.notifyUnfollowed(unfollower, unfollowed);
                        } catch (RemoteException e) {
                           
                            e.printStackTrace();
                        }
                    }
            }
            }
         if(followingMap.get(unfollower).contains(unfollowed)){
                followingMap.get(unfollower).remove(unfollowed);
                scritturaFollowing();}
        write.unlock();
        return 3;

    }

    //metodo per la richiesta di list following
    //ritorna una lista delle persone seguite dall'utente=username
    public synchronized ArrayList<String> listFollowing(String username){
        read.lock();
        read.unlock();
            return followingMap.get(username);
       

    }
    
    //metodo nela creazione di un post
    //ritorna una lista di interi,in prima posizione ci sarà il risultato dell'operazione,in seconda,nel caso l'operazione va a buon fine,l'id del post creato
    public synchronized ArrayList<Integer> createPost(String username,String titolo,String contenuto){
        write.lock();
        ArrayList<Integer> Result = new ArrayList<Integer>() ;
        //caso in cui titolo è di lunghezza maggiore di 20
        if(titolo.length()>20){
            Result.add(1);
            write.unlock();
            return Result;
        } 
        //caso in cui il contenuto è maggiore di 500 caratteri
        if(contenuto.length()>500){
            Result.add(2);
            write.unlock();
            return Result;
        } 
         Post newpost = new Post(username, titolo, contenuto);
           
            //mi assicuro che l'Id del post sia univoco caricandomi in allPost tutti i post presenti nella rete
            //e scorrendomi allPost se il nuovo Post ha un id uguale lo aumento di uno e riinizio da capo il confronto fino a quando il mio
            //nuovo post avrà un Id univoco
            ArrayList<Post> allPost= new ArrayList<Post>();
                //carico tutti i post in allPost
                for(java.util.Map.Entry<String, ArrayList<Post>> entry : post.entrySet()){
                    allPost.addAll(entry.getValue());
                 }
                 for(int i=0;i<allPost.size();i++){
                    if(newpost.getId()==allPost.get(i).getId()){
                        newpost.setId(newpost.getId()+1);
                        i=0;
                    }
                  }
                //qui sono sicuro che il mio nuovo post ha Id univoco e quindi posso aggiungerlo alla mappa dei post
                if(post.containsKey(username))
                  post.get(username).add(newpost);
                else{
                    post.put(username, new ArrayList<Post>());
                    post.get(username).add(newpost);
                }
        
        //caso in cui ho creato correttamente il post
        scritturaPost();
        Result.add(3);
        Result.add(newpost.getId());
        write.unlock();
        return Result;
    }                    
                               
    //metodo per vedere il blog dell'utente=username
    //ritorna una lista dei post di cui l'utente è autore                  
    public synchronized ArrayList<Post> viewBlog(String username){
        read.lock();
        read.unlock();
        return post.get(username);
    }
    
    //metodo per vedere il feed dell'utente=username
    //ritorna la lista di post che stanno nel feed dell'utente
     public synchronized ArrayList<Post> showFeed(String username){
        write.lock();
        ArrayList<Post> userFeed = new ArrayList<Post>();
        ArrayList<String> followingUsers = new ArrayList<String>();
       followingUsers=followingMap.get(username);
       
       for(String followingUser : followingUsers){
        userFeed.addAll(post.get(followingUser));
       }
       write.unlock();
       return userFeed;
    }
                  
    //metodo per vedere il post con Id=id
    //ritorna il Post con Id desiderato          
    public synchronized Post showPost(int id){
        read.lock();
        Post PostShowed=null;
        for(java.util.Map.Entry<String, ArrayList<Post>> entry : post.entrySet()){
           for(Post p : entry.getValue()){
            if(p.getId()==id ){
                read.unlock();
                return p; }
           }
        }
        read.unlock();
        return PostShowed;
    }
    
    //metodo per cancellare un post
    //ritorna un 1 se l'operazione è andata a buon fine,2 altrimenti
    public synchronized int deletePost(String username,int id) {
        write.lock();
        ArrayList<Post> target;
        ArrayList<Post> target1;
        //ok mi segnala se l'autore del post che vuole elimarlo è effettivamente lui e se il post esiste
        boolean ok=false;
        
       //rimuovo il post
        for(java.util.Map.Entry<String, ArrayList<Post>> entry : post.entrySet()){
           target=entry.getValue();
       
           for(Iterator<Post> it=target.iterator();it.hasNext();){
            Post p=it.next();
            if(p.getId()==id && p.getAutore().equals(username)){
                it.remove();
                ok=true; }
           }
        }

         
         //se il post è stato rimosso,rimuovo i rewin di tale post
         for(java.util.Map.Entry<String, ArrayList<Post>> entry1 : post.entrySet()){
            target1=entry1.getValue();
            for(Iterator<Post> it1=target1.iterator();it1.hasNext();){
                Post p=it1.next();
             if(p.getId()==id && p.getAutore().equals(username) && p.rewinPosts==true && ok){
               it1.remove();
                  }
            }
        }
        //tolgo nella map dei votiConteggiati e dei commentiConteggiati il post

        if(ok){
            if(votiConteggiati.containsKey(id)){
                 votiConteggiati.remove(id);
                 scritturaVotiConteggiati();}
            if(commentiConteggiati.containsKey(id)){
                    commentiConteggiati.remove(id);
                    scritturaCommentiConteggiati();
             }
            scritturaPost();
            write.unlock();
            return 1;  
        } 
        else{
            write.unlock();
            return 2;}
    }

    //metodo per il rewin di un post
    //ritorna 1 se l post non esiste o non sta nel feed dell'utente che ha fatto la richiesta
    //ritorna 2 se l'operazione è andata a buon fine
    public synchronized int rewinPost(String username,int id){
        write.lock();
        //come prima cosa mi vado a prendere il Post con Id=id
        Post p=new Post();
        ArrayList<Post> target;
        boolean postexists=false;
        for(java.util.Map.Entry<String, ArrayList<Post>> entry : post.entrySet()){
            target=entry.getValue();
            for(Post q : target){
                
                if(q.getId()==id){
                    p=q;
                    postexists=true;
                }
            }
        }
        if(postexists==false){
            write.unlock();
            return 1;}
        //controllo che l'autore del post di cui l'utente vuole fare il rewin sia nel feed dell'utente
        else if(!followingMap.get(username).contains(p.getAutore())){
                write.unlock();
                return 1;
        }
        //qui le condizioni per il rewin del post sono verificate
        else{
            //metto rewinPosts a true indicandomi che è un post rewinato
            p.setrewinPosts(true);
            post.get(username).add(p);
            scritturaPost();
            write.unlock();
            return 2;
        }
    }

    //metodo per votare un post
    //ritorna 1 se il post non esiste,2 se il post non sta nel feed dell'utente che vuole votare
    //ritorna 3 se l'utente vuole votare un post di cui è autore
    //ritorna 4 se l'utente ha gia votato il post
    //ritorna 5 se l'operazione è andata a buon fine
    public synchronized int ratePost(String username,int id,int voto){
        write.lock();
         //come prima cosa mi vado a prendere il Post con Id=id
        Post p=new Post();
        ArrayList<Post> target;
        boolean postexists=false;
        for(java.util.Map.Entry<String, ArrayList<Post>> entry1 : post.entrySet()){
            target=entry1.getValue();
            for(Post q : target){
                //non permetto di votare un post rewinato
                if(q.getId()==id && q.rewinPosts==false){
                    p=q;
                    postexists=true;
                }
            }
        }
        //controllo che il post con Id=id esista
        if(postexists==false){
            write.unlock();
            return 1;}

         //controllo che l'utente che vuole assegnare il voto al Post non sia l'autore del Post stesso
         else if(username.equals(p.getAutore())){
            write.unlock();
            return 3;
        }
        //controllo che l'autore del post di cui l'utente vuole fare assegnare il voto sia nel feed dell'utente
        else if(!followingMap.get(username).contains(p.getAutore())){
            write.unlock();
                return 2;
        }
       
        //controllo che l'utente non abbia gia votato il post
        else if(p.getVoti().containsKey(username)){
            write.unlock();
            return 4;
        }
        //qui tutte le condizioni per cui l'utente può votare il post sono verificate,quindi aggiungo il voto al post
        else{
            for(java.util.Map.Entry<String, ArrayList<Post>> entry : post.entrySet()){
                for(Iterator<Post> it=entry.getValue().iterator();it.hasNext();){
                    Post s=it.next();
                    //non permetto di votare un post rewinato
                    if(s.getId()==id && s.rewinPosts==false){
                            
                            s.getVoti().put(username, voto);
                            scritturaPost();
                            write.unlock();
                            return 5;
                    }
                }
            }
        }
        write.unlock();
        return 0;
    }

    //metodo per aggiungere un commento a un post
    //ritorna 1 se il post non esiste,2 se il post non sta nel feed dell'utente
    //ritorna 3 se l'utente che vuole aggiungere il commento è l'autore stesso del post
    //ritorna 4 se l'operazione è andata a buon fine
    public synchronized int addComment(String username,int id,String commento){
        write.lock();
          //come prima cosa mi vado a prendere il Post con Id=id
          Post p=new Post();
          ArrayList<Post> target;
          boolean postexists=false;
          for(java.util.Map.Entry<String, ArrayList<Post>> entry1 : post.entrySet()){
              target=entry1.getValue();
              for(Post q : target){
                //non permetto di commentare post rewinati
                  if(q.getId()==id && q.rewinPosts==false){
                      p=q;
                      postexists=true;
                  }
              }
          }
          //controllo che il post con Id=id esista
          if(postexists==false){
            write.unlock();
              return 1;}
         //controllo che l'utente che vuole aggiungere un commento al Post non sia l'autore del Post stesso
         else if(username.equals(p.getAutore())){
            write.unlock();
            return 3;
         }
               //controllo che l'autore del post di cui l'utente vuole aggiungere un commento sia nel feed dell'utente
          else if(!followingMap.get(username).contains(p.getAutore())){
            write.unlock();
            return 2;
          }
        
         //qui tutte le condizioni per cui l'utente può commentare il post sono verificate,quindi aggiungo il commento al post
         else{
            for(java.util.Map.Entry<String, ArrayList<Post>> entry : post.entrySet()){
                for(Iterator<Post> it=entry.getValue().iterator();it.hasNext();){
                    Post s=it.next();
                    
                    if(s.getId()==id){
                            if(!s.getCommenti().containsKey(username)){
                            s.getCommenti().put(username, new ArrayList<String>());
                            s.getCommenti().get(username).add(commento);
                            scritturaPost();
                            return 4;}
                            else{
                                s.getCommenti().get(username).add(commento);
                                scritturaPost();
                                write.unlock();
                                return 4;
                            }
                           
                    }
                }
            }
        }
        write.unlock();
        return 0;

    }

    //metodo per la richiesta di wallet
    //ritorna una map che ha come chiave il valore del wallet,come value la map con lo storico delle transazione
    public synchronized HashMap<Double,Map<String,Double>> getWallet(String username){
        write.lock();
        Map<String,Double> listaTransazioni=new HashMap<>();
        HashMap<Double,Map<String,Double>> mapReturned=new HashMap<>();
        Double valoreWallet=wallets.get(username);
        listaTransazioni=transazioni.get(username);
        mapReturned.put(valoreWallet, listaTransazioni);
        write.unlock();
        return mapReturned;
    }

    //metodo per la richiesta di wallet btc
    //faccio una http get request a random.org facendomi randomizzare un numero decimale compreso tra [0,1] e lo moltiplico al valore del wallet per effettuare il cambio
    //ritorna il double con la conversione in btc eseguita
    public synchronized Double getWalletinBitcoin(String username){
        write.lock();
       java.net.URL url;
    try {
        //faccio la richiesta a Random.org per generarmi un numero decimale tra 0 e 1 (con due cifre decimali)
        url = new java.net.URL("https://www.random.org/decimal-fractions/?num=1&dec=2&col=1&format=html&rnd=new");
       HttpURLConnection conn=(HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
       // conn.connect();

        //Check se la connessione è stata fatta
        int responseCode = conn.getResponseCode();

        //200 OK
        if(responseCode!=200){
            write.unlock();
            throw new RuntimeException("HttpResponseCode: "+ responseCode);
        }else{

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            String target=null;

			while ((inputLine = in.readLine()) != null) {
				if(inputLine.equals("<p>Here are your random numbers:</p>")){
                    target=in.readLine();
                }
			}
			in.close();

            String delims2="[<>]+";
            String[] tokens = target.split(delims2);
           
            //qui ho il numero decimale che mi ha randomizzato RANDOM.ORG
            Double cambio=Double.parseDouble(tokens[2]);
            Double wallet=wallets.get(username);
            cambio=wallet*cambio;
            write.unlock();
            return cambio;
        }
        
    } catch (MalformedURLException e) {
        System.out.println("Eccezione Url");
        e.printStackTrace(); }
        catch(IOException e1){e1.printStackTrace();}
    return 0.0;

    }

     /** Metodo per il calcolo delle ricompense
     * @param RicompensaAutore = % di guadagno che spetta all'autore
     * @param RicompensaCuratore = % di guadagno che spetta al curatore
     */
    public synchronized void CalcoloRicompense(int RicompensaAutore,int RicompensaCuratore){
        write.lock();
        //guadagno totale
        double Guadagno;

        ArrayList<Post> target;
       
        
        for(java.util.Map.Entry<String, ArrayList<Post>> entry : post.entrySet()){
            target=entry.getValue();
            for(Post p : target){
                //non faccio calcolo delle ricompense su post che sono stati rewinati
             if(!p.rewinPosts){
                 //in curatori mi salverò ogni volta i curatori del post
                 ArrayList<String> curatori=new ArrayList<String>();
                  //valore del guadagno dei voti
                Double GuadagnoVoti=0.0;
                //valore dei voti del post
                int countVoti=0;
               //numero di commenti che un utente ha fatto
                int Cp=0;
                //valore del guadagno dei commenti del Post
                Double GuadagnoCommenti=0.0;
                for(java.util.Map.Entry<String, Integer> entry1 : p.voti.entrySet()){
                    //caso in cui i voti del post p non sono stati ancora conteggiati
                    if(!votiConteggiati.containsKey(p.getId())){
                         //se il voto è positivo mi salvo il nome del curatore
                        if(entry1.getValue()==1){
                            if(!curatori.contains(entry1.getKey()))
                            curatori.add(entry1.getKey());
                        }
                    countVoti=countVoti+entry1.getValue();
                    //lo segno nella mia map dei voti conteggiati
                    votiConteggiati.put(p.getId(), new ArrayList<String>());
                    votiConteggiati.get(p.getId()).add(entry1.getKey());
                    }
                    //caso in cui ho gia conteggiato dei voti del Post p,quindi vado a vedere quali ancora devo conteggiare
                    else if(!votiConteggiati.get(p.getId()).contains(entry1.getKey())){

                        if(entry1.getValue()==1){
                            //se il voto è positivo mi salvo il nome del curatore
                            if(!curatori.contains(entry1.getKey()))
                            curatori.add(entry1.getKey());
                        }

                            countVoti=countVoti+entry1.getValue();
                             //lo segno nella mia map dei voti conteggiati
                            votiConteggiati.get(p.getId()).add(entry1.getKey());
                    }
                }
                
                
                if(countVoti!=0){
                countVoti++;
                GuadagnoVoti=Math.log(countVoti);
                GuadagnoVoti=GuadagnoVoti/(p.iterated);
                //evito il griefing dei voti
                    if(GuadagnoVoti<0) GuadagnoVoti=0.0;
                }
                //fino a qui ho calcolato il guadagno dei voti del Post p
                //ora calcolo il guadagno dei commenti
                for(java.util.Map.Entry<String, ArrayList<String>> entry2 : p.commenti.entrySet()){
                    Double countCommenti=0.0;
                    Cp= entry2.getValue().size();
                    Double denominatore=Math.exp(-(Cp-1));
                    denominatore++;
                     //qui sono nel caso in cui non ho ancora conteggiato nessun commento del Post p
                    if(!commentiConteggiati.containsKey(p.getId())){
                       
                        countCommenti=((2/denominatore)+1);
                        
                        //inserisco nella mia lista di curatori l'utente di cui sto valutando i commenti
                        if(!curatori.contains(entry2.getKey())){
                            curatori.add(entry2.getKey());
                        }
                         ConcurrentHashMap<String,ArrayList<String>> map=new ConcurrentHashMap<String,ArrayList<String>>();
                        commentiConteggiati.put(p.getId(), map);
                        commentiConteggiati.get(p.getId()).put(entry2.getKey(), entry2.getValue());
                    }
                    //caso in cui ho conteggiato gia dei commenti del Post p
                    else{
                        //caso in cui non ho ancora conteggiato nessun commento dell'utente=entry2.getKey()
                        if(!commentiConteggiati.get(p.getId()).containsKey(entry2.getKey())){
                           
                            if(!curatori.contains(entry2.getKey())){
                                curatori.add(entry2.getKey());
                            }
                            countCommenti=((2/denominatore)+1);
                           
                            commentiConteggiati.get(p.getId()).put(entry2.getKey(),entry2.getValue());
                        }
                        else{
                            //caso in cui ho conteggiato dei commenti dell'utente=entry2.geKey()
                            
                            for(String s : entry2.getValue()){
                               
                                //caso in cui il commento che sto valutando non è ancora stato conteggiato
                                if(!commentiConteggiati.get(p.getId()).get(entry2.getKey()).contains(s)){
                                    commentiConteggiati.get(p.getId()).get(entry2.getKey()).add(s);
                                    if(!curatori.contains(entry2.getKey())){
                                        curatori.add(entry2.getKey());
                                    }
                                  
                                }
                            }
                            if(curatori.contains(entry2.getKey()))
                            countCommenti=((2/denominatore)+1);
                            
                        }
                        
                    }
                    GuadagnoCommenti=GuadagnoCommenti+countCommenti;
                }
                
                if(GuadagnoCommenti!=0){
                    GuadagnoCommenti=Math.log(GuadagnoCommenti)/p.iterated;
                    
                }
               
                Guadagno=GuadagnoVoti+GuadagnoCommenti;
                if(Guadagno!=0){
                //calcolo il guadagno dell'autore,aggiorno il suo wallet e aggiungo la transazione
                double ricompensaAutore=(Guadagno*(RicompensaAutore))/100;
                //approssimo le ricompense a due cifre decimali
                ricompensaAutore=Math.round(ricompensaAutore*(100))/100.00;
               
                //calcolo la ricompensa curatore e la divido per il numero di curatori
                double ricompensaCuratore=(Guadagno*(RicompensaCuratore))/(100*(curatori.size()));
                ricompensaCuratore=Math.round(ricompensaCuratore*(100))/100.00;
               //aggiorno i wallet dell'autore
                double vecchiowallet=wallets.get(p.autore);
                double newwallet=Double.sum(vecchiowallet, ricompensaAutore);
                wallets.remove(p.getAutore());
                wallets.put(p.autore, newwallet);
                //prendo la data per metterla nella map delle transazioni
                Date date = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a");
                String formattedDate = sdf.format(date);
                transazioni.get(p.autore).put(formattedDate,ricompensaAutore);

                //per ogni curatore,aggiorno il suo wallet e aggiungo la transazione
                for(String c : curatori){
                    Date date1 = new Date();
                    SimpleDateFormat sdf1 = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a");
                    String formattedDate1 = sdf1.format(date1);
                    double oldwallet=wallets.get(c);
                    double newwallet1=Double.sum(oldwallet,ricompensaCuratore);
                    wallets.remove(c);
                    wallets.put(c, newwallet1);
                    transazioni.get(c).put(formattedDate1,ricompensaCuratore);

                }
                //aggiorno i miei file.json
                 scritturaVotiConteggiati();
                scritturaCommentiConteggiati();
                scritturaWallets();
                scritturaTransazioni();
                //incremento il numero di iterazioni che ha avuto il post
                p.iterated++;
                scritturaPost();}
                else{p.iterated++;}
               
            }
            }


        }
        write.unlock();
    }
         
        
    //opreazione per ricostruire lo stato del sistema
    public synchronized void recuperodati(){
      
        letturaUtentiRegistrati();
        letturaFollowers();
        letturaFollowing();
        letturaPost();
        letturaWallets();
       letturaVotiConteggiati();
       letturaCommentiConteggiati();
       letturaTransazioni();
        
    }


    //lettura del file post.json
    public synchronized void letturaPost(){

        if(Post.exists()){
            try {
                FileReader fileReader = new FileReader(Post);
                Type type = new TypeToken< ConcurrentHashMap<String,ArrayList<Post>>>(){}.getType();
                Gson gson = new Gson();

                post = gson.fromJson(fileReader, type);
                fileReader.close();
              
            } catch (FileNotFoundException e) {
                System.err.println("Errore nella creazione del FileReader Object");
            } catch (IOException e) {System.err.println("Errore nella chiusura del file"); }
        } else{System.out.println("File dei post ancora inesistente");}


    }

    //lettura del file utentiRegistrati.json
   public synchronized void letturaUtentiRegistrati(){

    if(Registrati.exists()){
        try {
            FileReader fileReader = new FileReader(Registrati);
            Type type = new TypeToken<ArrayList<Utente>>(){}.getType();
            Gson gson = new Gson();
            utentiRegistrati = gson.fromJson(fileReader, type);
            fileReader.close();
        } catch (FileNotFoundException e) {
            System.err.println("Errore nella creazione del FileReader Object");
        } catch (IOException e) {System.err.println("Errore nella chiusura del file"); }
    } else{System.out.println("File degli uenti registrati ancora inesistente");}
   }

   //lettura del file wallets.json
   public synchronized void letturaWallets(){
    
    if(Wallets.exists()){
        try {
            FileReader fileReader = new FileReader(Wallets);
            Type type = new TypeToken< ConcurrentHashMap<String,Double>>(){}.getType();
            Gson gson = new Gson();
           wallets = gson.fromJson(fileReader, type);
            fileReader.close();
        } catch (FileNotFoundException e) {
            System.err.println("Errore nella creazione del FileReader Object");
        } catch (IOException e) {System.err.println("Errore nella chiusura del file"); }
    } else{System.out.println("File dei wallets ancora inesistente");}
   }

   //lettura del file transazioni.json
   public synchronized void letturaTransazioni(){
    
    if(Transazioni.exists()){
        try {
            FileReader fileReader = new FileReader(Transazioni);
            Type type = new TypeToken< ConcurrentHashMap<String,Map<String,Double>>>(){}.getType();
            Gson gson = new Gson();
           transazioni = gson.fromJson(fileReader, type);
            fileReader.close();
        } catch (FileNotFoundException e) {
            System.err.println("Errore nella creazione del FileReader Object");
        } catch (IOException e) {System.err.println("Errore nella chiusura del file"); }
    } else{System.out.println("File delle transazioni ancora inesistente");}
   }

   //lettura del file votiConteggiati.json
   public synchronized void letturaVotiConteggiati(){
    
    if(VotiConteggiati.exists()){
        try {
            FileReader fileReader = new FileReader(VotiConteggiati);
            Type type = new TypeToken< ConcurrentHashMap<Integer,ArrayList<String>>>(){}.getType();
            Gson gson = new Gson();
           votiConteggiati = gson.fromJson(fileReader, type);
            fileReader.close();
        } catch (FileNotFoundException e) {
            System.err.println("Errore nella creazione del FileReader Object");
        } catch (IOException e) {System.err.println("Errore nella chiusura del file"); }
    } else{System.out.println("File dei voti conteggiati ancora inesistente");}
   }

   //lettura del file commentiConteggiati
   public synchronized void letturaCommentiConteggiati(){
    
    if(CommentiConteggiati.exists()){
        try {
            FileReader fileReader = new FileReader(CommentiConteggiati);
            Type type = new TypeToken< ConcurrentHashMap<Integer,ConcurrentHashMap<String,ArrayList<String>>>>(){}.getType();
            Gson gson = new Gson();
           commentiConteggiati = gson.fromJson(fileReader, type);
            fileReader.close();
        } catch (FileNotFoundException e) {
            System.err.println("Errore nella creazione del FileReader Object");
        } catch (IOException e) {System.err.println("Errore nella chiusura del file"); }
    } else{System.out.println("File dei voti conteggiati ancora inesistente");}
   }


   //lettura del file following.json
   public synchronized void letturaFollowing(){
    
    if(following.exists()){
        try {
            FileReader fileReader = new FileReader(following);
            Type type = new TypeToken< ConcurrentHashMap<String,ArrayList<String>>>(){}.getType();
            Gson gson = new Gson();
            followingMap = gson.fromJson(fileReader, type);
            fileReader.close();
        } catch (FileNotFoundException e) {
            System.err.println("Errore nella creazione del FileReader Object");
        } catch (IOException e) {System.err.println("Errore nella chiusura del file"); }
    } else{System.out.println("File degli uenti registrati ancora inesistente");}
   }



   //lettura del file followers.json
public synchronized void letturaFollowers(){
    
    if(Followers.exists()){
        try {
            FileReader fileReader = new FileReader(Followers);
            Type type = new TypeToken< ConcurrentHashMap<String,ArrayList<String>>>(){}.getType();
            Gson gson = new Gson();
            followersMap = gson.fromJson(fileReader, type);
            fileReader.close();
        } catch (FileNotFoundException e) {
            System.err.println("Errore nella creazione del FileReader Object");
        } catch (IOException e) {System.err.println("Errore nella chiusura del file"); }
    } else{System.out.println("File degli uenti registrati ancora inesistente");}
   }


   //scrittura nel file followers.json
   public synchronized static void scritturaFollowers(){
    if(!followersMap.isEmpty()){
        try {
            FileWriter fileWriter = new FileWriter(Followers);
            Gson gson = new Gson();
            gson.toJson( followersMap,fileWriter);
            fileWriter.close();
        } catch (IOException e) {
            System.err.println("Errore nella scrittura del file");
        }
    }else if(!Followers.exists()){System.out.println("ancora nessun utente ha un follower");}
    
   }

   //scrittura nel file following.json
   public synchronized void scritturaFollowing(){
    if(!followingMap.isEmpty()){
        try {
            FileWriter fileWriter = new FileWriter(following);
            Gson gson = new Gson();
            gson.toJson( followingMap,fileWriter);
            fileWriter.close();
        } catch (IOException e) {
            System.err.println("Errore nella scrittura del file");
        }
    }else if(!following.exists()){System.out.println("ancora nessun utente ha degli utenti di cui è follower");}

   }

   //scrittura nel file utentiRegistrati.json
   public synchronized void scritturaUtentiRegistrati(){
   
    if(!utentiRegistrati.isEmpty()){
        try {
            FileWriter fileWriter = new FileWriter(Registrati);
            Gson gson = new Gson();
            gson.toJson(utentiRegistrati,fileWriter);
            fileWriter.close();
        } catch (IOException e) {
            System.err.println("Errore nella scrittura del file");
        }
    }else if(!Registrati.exists()){System.out.println("ancora nessun utente registrato");}

   }

   //scrittura nel file wallets.json
   public synchronized void scritturaWallets(){
    if(!wallets.isEmpty()){
        try {
            FileWriter fileWriter = new FileWriter(Wallets);
            Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
             gson.toJson(wallets,fileWriter);
            fileWriter.close();
        } catch (IOException e) {
            System.err.println("Errore nella scrittura del file");
        }
    }else if(!Wallets.exists()){System.out.println("ancora nessun utente ha un wallet");}


   }

   //scrittura nel file transazioni.json
   public synchronized void scritturaTransazioni(){
    if(!transazioni.isEmpty()){
        try {
            FileWriter fileWriter = new FileWriter(Transazioni);
            Gson gson = new Gson();
            gson.toJson(transazioni,fileWriter);
            fileWriter.close();
        } catch (IOException e) {
            System.err.println("Errore nella scrittura del file");
        }
    }else if(!Transazioni.exists()){System.out.println("ancora nessun utente ha avuto transazioni");}


   }

   //scrittura nel file votiConteggiati.json
   public synchronized void scritturaVotiConteggiati(){
    if(!votiConteggiati.isEmpty()){
        try {
            FileWriter fileWriter = new FileWriter(VotiConteggiati);
            Gson gson = new Gson();
            gson.toJson(votiConteggiati,fileWriter);
            fileWriter.close();
        } catch (IOException e) {
            System.err.println("Errore nella scrittura del file");
        }
    }else if(!VotiConteggiati.exists()){System.out.println("ancora nessun utente è stato conteggiato");}


   }

   //scrittura nel file commentiConteggiati.json
   public synchronized void scritturaCommentiConteggiati(){
    if(!commentiConteggiati.isEmpty()){
        try {
            FileWriter fileWriter = new FileWriter(CommentiConteggiati);
            Gson gson = new Gson();
            gson.toJson(commentiConteggiati,fileWriter);
            fileWriter.close();
        } catch (IOException e) {
            System.err.println("Errore nella scrittura del file");
        }
    }else if(!VotiConteggiati.exists()){System.out.println("ancora nessun utente è stato conteggiato");}


   }

   //scrittura nel file post.json
   public synchronized void scritturaPost(){
    if(!post.isEmpty()){
        try {
            FileWriter fileWriter = new FileWriter(Post);
            Gson gson = new Gson();
            gson.toJson(post,fileWriter);
            fileWriter.close();
        } catch (IOException e) {
            System.err.println("Errore nella scrittura del file");
        }
    }else if(!Post.exists()){System.out.println("ancora nessun post pubblicato");}
   }

       
}

