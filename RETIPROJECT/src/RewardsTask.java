

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

public class RewardsTask implements Runnable {
    private int intervallo;
    private int MultiCastport;
    private static MulticastSocket multicastSock=null;
    private String address;
    private int ricompensaAutore;
    private int ricompensaCuratore;
    WinsomeServer instance;

    public RewardsTask(String address,int MultiCastport,int intervallo,int ricompensaAutore,int ricompensaCuratore,WinsomeServer instance) throws IOException,NullPointerException{
            this.address=address;
            this.MultiCastport=MultiCastport;
            this.intervallo=intervallo;
            this.ricompensaAutore=ricompensaAutore;
            this.ricompensaCuratore=ricompensaCuratore;
            this.instance=instance;
            multicastSock= new MulticastSocket(MultiCastport);


     }


    @Override
    public void run() {
       
       while(!Thread.currentThread().isInterrupted()){
        try { Thread.sleep(intervallo); }
			catch (InterruptedException shutdown)
			{
				multicastSock.close();
				return;
			}

           InetAddress group=null;
            try {
                group = InetAddress.getByName(address);
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            }


            instance.CalcoloRicompense(ricompensaAutore, ricompensaCuratore);
            System.out.println("Calcolo Ricompense svolto");
            String msg = "Il tuo wallet potrebbe essere stato aggiornato";
            DatagramPacket packet = new DatagramPacket(msg.getBytes(), msg.length(),group,MultiCastport);
            try {
                multicastSock.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
           
        
      }
    
   }
}
