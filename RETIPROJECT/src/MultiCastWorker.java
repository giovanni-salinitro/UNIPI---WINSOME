

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;



public class MultiCastWorker implements Runnable {
    String address;
    int PortMulticast;

    public MultiCastWorker(String address,int PortMulticast) throws IOException,InterruptedException{
        this.address=address;
        this.PortMulticast=PortMulticast;
    }

    @Override
    public void run()  {
     
      try {
       
        MulticastSocket multicastSock = new MulticastSocket(PortMulticast);
       InetSocketAddress group=new InetSocketAddress(InetAddress.getByName(address),PortMulticast);
       
       NetworkInterface netIf=NetworkInterface.getByName(address);
       
       multicastSock.joinGroup(group,netIf );
      
       byte[] buffer=new byte[512];
       DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
       while(!Thread.currentThread().isInterrupted()){
       multicastSock.receive(packet);
       System.out.println(new String(buffer));
       }
       multicastSock.leaveGroup(group, netIf);
       multicastSock.close();
       

      } catch (IOException e) {
       e.printStackTrace(); }
    
    
 } 
    
}
