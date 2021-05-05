/**
 * <h1> PeerDiscoveryThread </h1>
 * This Singleton class is used to initialize and refer to the UDP Broadcast thread of the client.
 * 
 */
package Current;

public class PeerDiscoveryThread implements Runnable{
    
    //Broadcast Object
    private Broadcast listener = new Broadcast();
    
    //Thread of execution function. Upon initiating PeerDiscoveryThread, this is executed.
    @Override
    public void run() {
        Thread openToNetwork = new Thread(listener);
        openToNetwork.start();
    }
    
    //Public function used for accessing this Thread.
    public static PeerDiscoveryThread getInstance(){
        return DiscoveryThreadHolder.INSTANCE;
    }
    
    //Holds DiscoveryThread
    private static class DiscoveryThreadHolder{
        private static final PeerDiscoveryThread INSTANCE = new PeerDiscoveryThread();
    }
    
    //Public function used for accessing the Broadcast object.
    public Broadcast getBroadcast(){
        return this.listener;
    }
    
}
