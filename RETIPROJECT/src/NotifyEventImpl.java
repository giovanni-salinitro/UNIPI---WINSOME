

import java.rmi.*;
import java.rmi.server.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



public class NotifyEventImpl extends RemoteObject implements WinsomeClientInterface {

    private Map<String,ArrayList<String>> followersMapCallback = null;
    /* crea un nuovo callback client */
         public NotifyEventImpl( ) throws RemoteException{
         super( );
        this.followersMapCallback = new ConcurrentHashMap<>();  }

        public void updateMapCallback(ConcurrentHashMap<String,ArrayList<String>> followersMap) throws RemoteException{
            followersMapCallback=followersMap;
           
        }

    @Override
    public void notifyFollower(String follower,String followed) throws RemoteException {
        if(!followersMapCallback.containsKey(followed)){
            followersMapCallback.put(followed, new ArrayList<String>());
            if(!followersMapCallback.get(followed).contains(followed))
             followersMapCallback.get(followed).add(follower);
            System.out.println(followed + " ha il nuovo follower " + follower);
        }
        else{
            if(!followersMapCallback.get(followed).contains(follower)){
                followersMapCallback.get(followed).add(follower);
                System.out.println(followed + " ha il nuovo follower " + follower);
                }
            }
        }
       
    public void listFollowers(String username) throws RemoteException{
        String c;
            if(!followersMapCallback.containsKey(username) || followersMapCallback.get(username).isEmpty()){
                System.out.println("non hai nessun follower");
            }
            else{
                System.out.println("Ecco i tuoi followers\n");
                for(int i=0;i<followersMapCallback.get(username).size();i++){
                        c=followersMapCallback.get(username).get(i);
                        System.out.println(c+"\n");
                }
            }
    }     
    

    @Override
    public void notifyUnfollowed(String unfollower,String unfollowed) throws RemoteException {

      if(followersMapCallback.containsKey(unfollowed)){
        followersMapCallback.get(unfollower).remove(unfollowed);
      }
      System.out.println(unfollowed + " non ha piu' come follower " + unfollower);
    }

    
    
}