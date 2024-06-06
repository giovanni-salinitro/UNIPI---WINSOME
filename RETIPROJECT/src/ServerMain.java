


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;
import java.util.concurrent.*;

public class ServerMain {

    private static final String portObjectRemote="portObjectRemote";
    private static final String SERVERSOCKETTIMEOUT="SERVERSOCKETTIMEOUT";
    private static final String UDPport="UDPport"; 
    private static final String RegistryName="RegistryName";
    private static final String MULTICASTaddress="MULTICASTaddress";
    private static final String TCPport="TCPport";
    private static final String RewardsInterval="RewardsInterval";
    private static final String RicompensaAutore="RicompensaAutore";
    private static final String RicompensaCuratore="RicompensaCuratore";

    //porta oggetto remoto per RMI
    public static int portObjectRemoteCallback ;
    //porta tcp per la serversocket
    public static int portTCP;
    //timeout della serversocket
    public static int timeoutSocket;
    //porta UDP per il multicast
    public static int Multicastport;
    //indirizzo MultiCast
     public static String addressMultiCast ;
     //nome della remote reference
     public static String name;
     //intervallo di tempo per il calcolo delle ricompense
     public static int intervalloReward;
     //percentuale ricompensa per l'autore
     public static int ricompensaAutore;
     //percentuale ricompensa curatore
     public static int ricompensaCuratore;

    //Socket per l'interazione tra client e server
    public static ServerSocket serversocket = null;
   
    
   
    
/**
 * @param args
 * @throws IOException
 */
public static void main(String args[]) throws IOException{

      //lettura parametri del file di configurazione
      try {
            
        File config=new File("./src/config/server.properties");
        Properties properties = new Properties();
        FileInputStream fis = new FileInputStream(config);
        properties.load(fis);
        fis.close();
            portTCP=Integer.parseInt(properties.getProperty(TCPport));
           // System.out.println(portTCP);
            timeoutSocket=Integer.parseInt(properties.getProperty(SERVERSOCKETTIMEOUT));
            //System.out.println(timeoutSocket);
            Multicastport=Integer.parseInt(properties.getProperty(UDPport));
            //System.out.println(Multicastport);
            name=properties.getProperty(RegistryName);
            //System.out.println(name);
            addressMultiCast=properties.getProperty(MULTICASTaddress);
            //System.out.println(addressMultiCast);
            portObjectRemoteCallback=Integer.parseInt(properties.getProperty(portObjectRemote));
            //System.out.println(portObjectRemoteCallback);
            intervalloReward=Integer.parseInt(properties.getProperty(RewardsInterval));
            //System.out.println(intervalloReward);
            ricompensaAutore=Integer.parseInt(properties.getProperty(RicompensaAutore));
            //System.out.println(ricompensaAutore);
            ricompensaCuratore=Integer.parseInt(properties.getProperty(RicompensaCuratore));
            //System.out.println(ricompensaCuratore);
  
       } catch (NumberFormatException e) {
          System.out.println("formato dati config errato");
      }catch(IllegalArgumentException e1){ System.out.println("formato dati config errato"); }

    WinsomeServer instance = new WinsomeServer();

    //creo i threadpool che mi gestisce i vari client
    ThreadPoolExecutor executor= (ThreadPoolExecutor) Executors.newCachedThreadPool(); 

    //attivo il thread per il calcolo delle ricompense
   Thread rewards=new Thread(new RewardsTask(addressMultiCast, Multicastport, intervalloReward,ricompensaAutore,ricompensaCuratore,instance));
    rewards.start();

   
    

      try {
       
        WinsomeServerInterface stub=(WinsomeServerInterface) UnicastRemoteObject.exportObject (instance,0);
        LocateRegistry.createRegistry(portObjectRemoteCallback);
        Registry registry=LocateRegistry.getRegistry(portObjectRemoteCallback);
        registry.bind (name, stub);
    } catch (Exception e) {
        System.out.println("Eccezione"+e);
    }
 

     //inizializzo la socket per la connessione alla determinata porta e col determinato Timeout
     try {
        serversocket = new ServerSocket(portTCP);
        serversocket.setSoTimeout(timeoutSocket);
       } catch (Exception e) {
             System.err.println("Errore nell'inizializzazione della ServerSocket!");
			System.exit(1);
       }

      
        while(true){
        try {
            
           
           
            //mi metto in attessa di una connessione del client
            Socket connection = serversocket.accept();
           System.out.println("Client connesso");

           //appena un client si connette al server creo un task che mi gestirà le sue varie richieste a cui passo
           //l'istanza di WinsomeServer,la socket,l'indirizzo di multicast e la porta di multicast;il task passerà quest'ultime due
           //al client affinchè esso possa mettersi in ascolto sulla MultiCastSocket 
           executor.execute(new ClientHendler(instance,connection,addressMultiCast,Multicastport));
       
            

        } catch (SocketTimeoutException e) {
            try {
                executor.awaitTermination(timeoutSocket, TimeUnit.MILLISECONDS);
                executor.shutdown();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            System.out.println("Timeout connessione!,il server entra in Shutdown Mode");
            rewards.interrupt();
            System.exit(1);
            break;
        }

      } 

   
}



}



 


