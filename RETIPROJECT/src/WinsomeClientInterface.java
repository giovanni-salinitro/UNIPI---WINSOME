

import java.rmi.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;





public interface WinsomeClientInterface extends Remote {
    public void notifyFollower(String follower,String followed) throws RemoteException;
    public void notifyUnfollowed(String unfollower,String unfollowed) throws RemoteException;
    public void listFollowers(String username) throws RemoteException;
    public void updateMapCallback(ConcurrentHashMap<String,ArrayList<String>> followersMap) throws RemoteException;
}