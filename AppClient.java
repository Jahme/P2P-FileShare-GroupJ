/**
 * <h1> AppClient </h1>
 * This Java class is responsible for execution of the P2P File Sharing
 * application. 
 * <p>
 * Integrates the following classes in order to functional properly:
 * - PeerDiscoveryThread.java
 * - Broadcast.java
 * - Connect.java
 * - ActivePeer.java
 * - ActivePeerClient.java
 * - ActivePeerThread.java
 * 
 * @author James Chomchan Gojit
 * @version 0.5
 * @since 2021-05-05
 * 
 */
package Current;
import java.util.*;
import java.net.*;

public class AppClient {  
    
    /**
     * This is the main thread used to execute the program.
     * @param args Unused
     * @throws InterruptedException On Thread interruption
     * @throws UnknownHostException On inability to connect to some host.
     */
    public static void main(String [] args) throws InterruptedException, UnknownHostException{
        
        //Initializations
        //System Input
        Scanner scan = new Scanner(System.in);
        //Start Peer Discovery Thread for Broadcast
        Thread discoveryThread = new Thread(PeerDiscoveryThread.getInstance());
        discoveryThread.start();
        Thread.sleep(1000);
        //Configure Peers
        Connect network = new Connect();
        //Initial Connect to other Broadcasts
        
        //ActivePeer() Thread: Responsible for handling incoming connection requests.
        Thread activity = new Thread(new ActivePeer());
        activity.start();
        Thread.sleep(1000); //Static startup load
        //Miscellaneous Variables
        String localAddress = InetAddress.getLocalHost().getHostAddress(); //Store host address
        
        // Program Execution Superloop
        superloop: while(true){
            //Display Menu and wait for user selection.
            System.out.println("==== MENU ====\n1. Trigger Network Update\n2. PUT\n3. GET\n4. DIRECT SYNC\n5. FULL SYNC\n6. Exit");
            String select = scan.nextLine();
            String desiredPeer;
            switch(Integer.parseInt(select)){
                // NETWORK UPDATE: Sends connections to clients currently Broadcasting (Broadcast.java) on UDP and appends newly found peers to contact list.
                case 1: 
                    //Start Connect.java Thread
                    Thread updateNetwork = new Thread(network);
                    updateNetwork.start();
                    updateNetwork.join(2000); //Allow 2s (2000ms) for connections before coming back to main thread.
                    
                    //Display known peers
                    System.out.println(network.getPeers().toString());
                    break;
                
                //PUT: Notify and send a file to peer client.
                case 2:
                    System.out.println(network.getPeers().toString()); //Display list of known peers 
                    do{ //Retrieve user input for target peer.
                        System.out.println("Select from list of connected peers."); 
                        desiredPeer = scan.nextLine();    
                    } while(!network.getPeers().containsKey(desiredPeer) && !desiredPeer.equals(localAddress));
                    
                    //Start Client PUT Thread
                    new Thread(new ActivePeerClient(desiredPeer, 12345, "PUT", "put.txt")).start();
                    break;
                    
                //GET: Request and store a file from peer client.
                case 3:
                    System.out.println(network.getPeers().toString());
                    do{ //Retrieve user input for target peer.
                        System.out.println("Select from list of connected peers.");
                        desiredPeer = scan.nextLine();    
                    } while(!network.getPeers().containsKey(desiredPeer) && !desiredPeer.equals(localAddress));
                    
                    //Start Client GET thread
                    new Thread(new ActivePeerClient(desiredPeer, 12345, "GET", "get.txt")).start();
                    break;

                //DIRECT SYNC: Execute SYN with a specific known peer.
                case 4: 
                    System.out.println(network.getPeers().toString()); //Display list of known peers
                    do{ //Retrieve user input for which peer it would like to directly sync with.
                        System.out.println("Select from list of connected peers.");
                        desiredPeer = scan.nextLine();    
                    } while(!network.getPeers().containsKey(desiredPeer) && !desiredPeer.equals(localAddress));
                    System.out.println("Syncing with Peer - IP: " + desiredPeer); //Notification of SYNC start
                    
                    //Start Client SYNC thread.
                    Thread singleSync = new Thread(new ActivePeerClient(desiredPeer, 12345, "SYN"));
                    singleSync.start();  
                    singleSync.join(); //Wait for current client to finish SYNC
                    System.out.println("Finished Syncing"); //Console notification
                    break;
                    
                //FULL SYNC: Similar to option 4 - Execute SYN across all known peers
                case 5:
                    for(Map.Entry entry: network.getPeers().entrySet()){ //Iterate through full peer list
                        if(!entry.getKey().equals(localAddress)){ //Avoid connecting to localAddress
                            System.out.println(entry.getKey()); //DEBUG
                            
                            //Start Client SYNC thread.
                            Thread soloSync = new Thread(new ActivePeerClient(entry.getKey().toString(), 12345, "SYN"));
                            soloSync.start();
                            soloSync.join(); //Wait for current client to finish SYNC
                            
                            System.out.println("Finished current."); //DEBUG
                        }
                    }
                    System.out.println("Finished Syncing with All Known Peers"); //Notify end of SYN
                    break;                       
                
                // End Program and Default Endcase
                case 6:
                    System.out.println("Closing Broadcast");
                    PeerDiscoveryThread.getInstance().getBroadcast().closeBroadcast();
                    System.out.println("Exiting Program");
                    System.exit(0);
                default:
                    System.out.println("Incorrect selection: System will now close.");
                    System.out.println("Closing Broadcast");
                    PeerDiscoveryThread.getInstance().getBroadcast().closeBroadcast();
                    System.out.println("Exiting Program");
                    break superloop;
            }       
        }
    }
}
