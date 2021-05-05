/**
 * <h1> ActivePeer </h1>
 * ServerSocket Thread that handles active connection requests from ActivePeerClients and passes their work to another Thread.
 * 
 */
package Current;
import java.io.*;
import java.util.*;
import java.net.*;

public class ActivePeer implements Runnable{

    private ServerSocket peerConnect;
    private HashMap<String, Integer> connectedPeers; //Will be a fingertable
    
    public ActivePeer(){
        try{
            this.peerConnect = new ServerSocket(12345); //Open connections to other peers.
            this.connectedPeers = new HashMap<>();
        } catch (IOException e) {
            System.out.println(e);
        }       
    }
     
    @Override
    public void run() {
        try{
            while(true){
                Socket newPeer = peerConnect.accept(); //Waiting for new connections
                String newHost = newPeer.getInetAddress().getHostName();
                int newPort = newPeer.getLocalPort();
                System.out.println("HostName: " + newHost + "\nPort: " + newPort);
                //Fetch details about new peer
                new ActivePeerThread(newPeer).start(); //Run Thread run() method
                connectedPeers.put(newHost, newPort); //Store hostname and associated port of socket into hashtable
            }
        }
        catch(IOException e){
            System.out.println(e);
        }
    }
    
}
