
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.rmi.server.*;
import java.util.*;


public class ClientMain {

    private static final String portObjectRemote="portObjectRemote";
	private static final String HostName="HostName";
    private static final String RegistryName="RegistryName";
    private static final String TCPport="TCPport";
    

    //porta oggetto remoto per RMI
    public static int portObjectRemoteCallback ;
    // Nome dell'host a cui connettersi.
    public static String hostname;
    //nome della remote reference
    public static String name;
	// Porta TCP associata alla socket.
	public static  int portTCP;
    //Porta UDP per multicast
    public static int PortMulticast;
    static String myUsername;
    static WinsomeClientInterface callbackObj;
   
   

    public static void main(String[] args) throws Exception {

        //lettura parametri del file di configurazione
        try {
            
      File config=new File("./src/config/client.properties");
      Properties properties = new Properties();
	  FileInputStream fis = new FileInputStream(config);
	  properties.load(fis);
	  fis.close();
        portObjectRemoteCallback=Integer.parseInt(properties.getProperty(portObjectRemote));
        hostname=properties.getProperty(HostName);
        name=properties.getProperty(RegistryName);
        portTCP=Integer.parseInt(properties.getProperty(TCPport));
    
     } catch (NumberFormatException e) {
        System.out.println("formato dati config errato");
    }catch(IllegalArgumentException e1){ System.out.println("formato dati config errato"); }

        



        //setup per comunicazione RMI
        Registry registry = LocateRegistry.getRegistry(portObjectRemoteCallback);
        WinsomeServerInterface server =(WinsomeServerInterface) registry.lookup(name);
        callbackObj = new NotifyEventImpl();
        WinsomeClientInterface stub = (WinsomeClientInterface) UnicastRemoteObject.exportObject(callbackObj, 0);
       
        //creo la socket per connettermi al server passandogli l'hostname e la portaTCP
       Socket socket = new Socket(hostname, portTCP);
       InputStreamReader in = new InputStreamReader(socket.getInputStream()); 
       BufferedReader bf = new BufferedReader(in);
       Thread notificaWallet=null;
       System.out.println("Benvenuto su WinsomeServer:per digitare la lista di comandi con la sintassi corretta da usare digita:");
       System.out.println("help");
       System.out.println("Ricorda che per usare i comandi devi prima effettuare il login!");
       System.out.println("Se non sei registrato,effettua prima la registrazione!");
       
      

      
      
         while(true){
            
           
            Scanner scanner = new Scanner(System.in);
            String message = scanner.nextLine();
            String delims = "[ <>]+";
            String[] tokens = message.split(delims); 
            PrintWriter pr = new PrintWriter(socket.getOutputStream());
          

           if(tokens[0].equals("help")){
            System.out.println("Ecco la lista dei comandi con la corretta sintassi");
            System.out.println("REGISTRAZIONE(massimo puoi inserire 5 tag): register username password tag1 tag2 tag3 tag4 tag5");
            System.out.println("LOGIN: login username password");
            System.out.println("EFFETTUARE IL LOGOUT: logout");
            System.out.println("VEDERE LISTA FOLLOWERS: list followers");
            System.out.println("VEDERE LISTA FOLLOWING: list following");
            System.out.println("VEDERE LISTA USERS: list users");
            System.out.println("FOLLOWARE UN UTENTE: follow username");
            System.out.println("UNFOLLOWARE UN UTENTE: unfollow username");
            System.out.println("CREARE UN POST(le parentesi <> sono obbligatorie): post <titolo> <contenuto>");
            System.out.println("VEDERE IL PROPRIO BLOG: blog");
            System.out.println("VEDERE IL PROPRIO FEED: show feed");
            System.out.println("VEDERE UN POST SPECIFICO: show post IdPost");
            System.out.println("CANCELLARE UN POST: delete IdPost");
            System.out.println("FARE IL REWIN DI UN POST: rewin IdPost");
            System.out.println("VOTARE UN POST(il voto può essere 1 o -1): rate IdPost Voto");
            System.out.println("AGGIUNGERE UN COMMENTO A UN POST(le parentesi <> sono obbligatorie): comment <IdPost> <commento>");
            System.out.println("VEDERE IL PORPRIO PORTAFOGLIO: wallet");
            System.out.println("VEDERE IL PORPRIO PORTAFOGLIO IN BITCOIN: wallet btc\n");

           }
      
            if(tokens[0].equals("register")){
                registrazione(tokens);
            } 
            
            if(tokens[0].equals("login")){

               
                 
             if(tokens.length<3)
             System.out.println("Per fare il login digitare login username password");
             else{
              try {
                  
                
                  pr.println(tokens[0]);
                  pr.flush();
                  pr.println(tokens[1]);
                  myUsername=tokens[1];
                  pr.flush();
                  pr.println(tokens[2]);
                  pr.flush();
                //leggo l'indirizzo di multicast
                  String indirizzo=bf.readLine();
                 
                  //leggo la porta per la comunicazione multicast
                  String portaUDP=bf.readLine();
                  PortMulticast=Integer.parseInt(portaUDP);
                  //avvio il mio MultiCastWorker con i riferimenti che mi ha fornito il server
                  notificaWallet=new Thread(new MultiCastWorker(indirizzo,PortMulticast));
                  notificaWallet.start();

               //leggo il messaggio di risposta del server
               String messaggio = bf.readLine();
               System.out.println(messaggio);

               //iscrivo l utente loggato al servizio di callback dei follower
               if(messaggio.equals(tokens[1] +" "+" loggato con successo!")){
                try {
                     server.registerForCallback(stub,myUsername);

                } catch (Exception e) { System.err.println("Client exception:"+ e.getMessage( )); }
               }

              } catch (IOException e) {
                  System.err.println("Errore nella creazione della socket!");
                  System.exit(1);
              }
            }
            
            }



            //caso in cui si digita il comando list followers
            if((tokens[0].equals("list")) && (tokens[1].equals("followers"))){
                callbackObj.listFollowers(myUsername);
            }

            //caso in cui si digita list following
            if((tokens[0].equals("list")) && (tokens[1].equals("following"))){
                pr.println(tokens[0]);
                pr.flush();
                pr.println(tokens[1]);
                pr.flush();
                String messaggio = bf.readLine();
                while(!messaggio.equals("end")){
                    System.out.println(messaggio);
                    messaggio=bf.readLine();
                }
            }

            //caso in cui l'utente digita il comando list users
            if((tokens[0].equals("list")) && (tokens[1].equals("users"))){
                pr.println(tokens[0]);
                pr.flush();
                pr.println(tokens[1]);
                pr.flush();
                String messaggio = bf.readLine();
                while(!messaggio.equals("end")){
                    System.out.println(messaggio);
                    messaggio=bf.readLine();
                }
            }

            //caso in cui digito il comando follow <username>
            if(tokens[0].equals("follow")){
                pr.println(tokens[0]);
                pr.flush();
                pr.println(tokens[1]);
                pr.flush();

                String messaggio = bf.readLine();
                System.out.println(messaggio);
            }

            //caso in cui digito il comando unfollow <username>
            if(tokens[0].equals("unfollow")){
                pr.println(tokens[0]);
                pr.flush();
                pr.println(tokens[1]);
                pr.flush();

                String messaggio = bf.readLine();
                System.out.println(messaggio);
            }

            //caso in cui digito il comando post <titolo> <contenuto>
            if(tokens[0].equals("post")){
                String delims2="[<>]+";
                String[] tokens2 = message.split(delims2);

              if(tokens2.length<4)
               System.out.println("Titolo e/o contenuto del post non possono essere vuoti");

               else{
                pr.println(tokens[0].toString());
                pr.flush();
                pr.println(tokens2[1].toString());
                pr.flush();
                pr.println(tokens2[3].toString());
                pr.flush();
                String messaggio = bf.readLine();
                System.out.println(messaggio);
               }
               
                

            }

            //caso in cui digito il comando blog
            if(tokens[0].equals("blog")){
                pr.println(tokens[0]);
                pr.flush();

                String messaggio = bf.readLine();
                while(!messaggio.equals("end")){
                    System.out.println(messaggio);
                    messaggio=bf.readLine();
                }
            }
            
            //caso in cui digito il comando show feed
            if((tokens[0].equals("show")) && (tokens[1].equals("feed"))){
                pr.println(tokens[0]);
                pr.flush();
                pr.println(tokens[1]);
                pr.flush();
                String messaggio = bf.readLine();
                while(!messaggio.equals("end")){
                    System.out.println(messaggio);
                    messaggio=bf.readLine();
                }
            }

            //caso in cui digito il comando show post IdPost
            if((tokens[0].equals("show")) && (tokens[1].equals("post"))){
                if(tokens.length<3 || tokens.length>4)
                System.out.println("Per vedere il post digitare: show post idPost");
                else{
                  
                    pr.println(tokens[0]);
                    pr.flush();
                    pr.println(tokens[1]);
                    pr.flush();
                    pr.println(tokens[2]);
                    pr.flush();

                    String messaggio = bf.readLine();
                   
                 while(!messaggio.equals("end")){
                    System.out.println(messaggio);
                    messaggio=bf.readLine();
                }
                }
            }

            //caso in cui digito il comando delete IdPost
            if(tokens[0].equals("delete")){
                if(tokens.length<2 || tokens.length>2)
                System.out.println("Per rimuove il post digitare: delete idPost");
                else{
                    pr.println(tokens[0]);
                    pr.flush();
                    pr.println(tokens[1]);
                    pr.flush();

                    String messaggio = bf.readLine();
                    System.out.println(messaggio);

                }
            }

            //caso in cui digito il comando rewin IdPost
            if(tokens[0].equals("rewin")){
                if(tokens.length<2 || tokens.length>2)
                System.out.println("Per il rewin di un  post digitare: rewin idPost");
                else{
                    pr.println(tokens[0]);
                    pr.flush();
                    pr.println(tokens[1]);
                    pr.flush();

                    String messaggio = bf.readLine();
                    System.out.println(messaggio);

                }
            }

            //caso in cui digito il comando rate IdPost Voto
            if(tokens[0].equals("rate")){
                if(tokens.length<3 || tokens.length>3)
                System.out.println("Per votare un post digitare rate idPost Voto");
                if(!tokens[2].equals("1") || !tokens[2].equals("-1")){
                    System.out.println("Per votare digitare 1 o -1");
                }
                else{
                    pr.println(tokens[0]);
                    pr.flush();
                    pr.println(tokens[1]);
                    pr.flush();
                    pr.println(tokens[2]);
                    pr.flush();

                    
                    String messaggio = bf.readLine();
                    System.out.println(messaggio);
                }
            }

            //caso in cui digito il comando comment <IdPost> <commento>
            if(tokens[0].equals("comment")){
                String delims2="[<>]+";
                String[] tokens2 = message.split(delims2);

               if(tokens2.length<4 || tokens2.length>4)
               System.out.println("Per commentare un post digitare comment <idPost> <commento> , idPost e commento non possono essere vuoti");

               else{
                pr.println(tokens[0].toString());
                pr.flush();
                pr.println(tokens2[1].toString());
                pr.flush();
                pr.println(tokens2[3].toString());
                pr.flush();
                String messaggio = bf.readLine();
                System.out.println(messaggio);
               }
            }

            //caso in cui digito il comando wallet
            if(tokens[0].equals("wallet") && tokens.length<2){
               
                
                pr.println(tokens[0]);
                pr.flush();
                pr.println("not");
                pr.flush();;
                String messaggio = bf.readLine();
                   
                while(!messaggio.equals("end")){
                   System.out.println(messaggio);
                   messaggio=bf.readLine();
                 }
                
            }

            //caso in cui digito il comando wallet btc
            if(tokens[0].equals("wallet") && tokens.length>=2){
                pr.println(tokens[0]);
                pr.flush();
                pr.println(tokens[1]);
                pr.flush();
                String messaggio = bf.readLine();
                System.out.println(messaggio);

            }



            //invio la richiesta di logout
            if(tokens[0].equals("logout")){
                
                pr.println(tokens[0]);
                pr.flush();
                String messaggio = bf.readLine();
                System.out.println(messaggio);
                server.unregisterForCallback(stub,myUsername);
                in.close();
                bf.close();
                scanner.close();
                notificaWallet.interrupt();
                
                  //chiudo la connessione con il server
            try {
                if(socket!=null)
                 socket.close();
               
                 System.exit(1);

                break;
            } 
            catch (IOException e) {
               System.out.println("Errore nella chiusura della connessione del client");
               System.exit(1);
            }
            
            }
        }
    }
      








//metodo registrazione tramire RMI
public static boolean registrazione(String[] tokens) throws NotBoundException{
   
         Utente z = new Utente();
       
        if(tokens.length<3 || tokens.length>8){
            System.out.println("Parametri d'ingresso per la registrazione sbagliati");
            return false;}
               else{
                z.username = tokens[1];
                z.password = tokens[2];
              
                   for(int d=3;d<tokens.length;d++){
                       z.taglist.add(tokens[d]);
                   }
   
               }

               
               try{
                Registry r = LocateRegistry.getRegistry(portObjectRemoteCallback);
                WinsomeServerInterface c = (WinsomeServerInterface) r.lookup(name);
                int x = c.Registrazione(z.username,z.password,z.taglist);
               
                   if(x == 3){
                    System.out.println("Registrazione avvenuta con successo");
                    return true;
                   }
                   else if(x == 2){
                    System.out.println("username gia in uso");
                     return false;
                   }
                   else if(x == 1){
                    System.out.println("formato registrazione errato");
                     return false;
                   }
            }catch(RemoteException e){
             System.out.println("eccezione");
             return false;
            }

            return true;
}
}

