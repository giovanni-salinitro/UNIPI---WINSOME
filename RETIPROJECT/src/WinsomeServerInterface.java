
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;



public interface WinsomeServerInterface extends Remote{
    public int Registrazione(String username, String password, ArrayList<String> tags)throws RemoteException;
    public void registerForCallback(WinsomeClientInterface ClientInterface,String username) throws RemoteException;
    public  void unregisterForCallback(WinsomeClientInterface Client,String username) throws RemoteException;
   
}
