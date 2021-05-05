/**
 * <h1> Connect </h1>
 * This Java class is responsible for having the Client detect UDP Broadcasts of other clients on the same LAN.
 * Used during "Trigger Network Update" feature through Main Client.
 * 
 */
package Current;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;

public class Connect implements Runnable{

    /**
     * HashMap of peers.
     * 
     * Updated whenever a new peer is found during thread execution.
     */
    private HashMap<String, Integer> peers;
    
    // Default Constructor of Class
    public Connect(){
        peers = new HashMap<>();
    }
    
    /**
     * Process of execution that is run when a Thread of this class is started.
     * Sends JOIN_NETWORK requests across all network interfaces to LAN. 
     */
    @Override
    public void run() {
        try {
            //Open a port for sending the JOIN_NETWORK request packet.
            DatagramSocket c = new DatagramSocket();
            c.setBroadcast(true);
 
            // Create connection packet
            byte[] sendData = "JOIN_NETWORK_CONNECT".getBytes();
            try {
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), 3275);
                c.send(sendPacket);
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
 
            // Broadcast the message over all the network interfaces
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                //Avoid sending packet into loopback interface, disabled interfaces, and virtual interfaces.
                if (networkInterface.isLoopback() || !networkInterface.isUp() || networkInterface.isVirtual()) {
                    continue; 
                }
                
                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast == null) {
                        continue;
                    }
                    //Send network join request
                    try {
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, 3275);
                        c.send(sendPacket);
                    } catch (IOException e) {
                        System.out.println("IOException: " + e.getMessage());
                    }
                    //System.out.println("Client>>> Request packet sent to: " + broadcast.getHostAddress() + "; Interface: " + networkInterface.getDisplayName());
                }
            }

            //Wait for a response
            int tries = 0;
            while(tries < 5){ //Attempts of connecting with broadcasts
                byte[] recvBuf = new byte[15000];
                DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
                c.receive(receivePacket);

                //Confirm Network Join handshake.
                String message = new String(receivePacket.getData()).trim();
                if (message.equals("JOIN_ACKNOWLEDGED")) {
                    String conn = receivePacket.getAddress().getHostAddress();
                    if(!peers.containsKey(conn)) peers.put(conn, receivePacket.getPort());
                }   
                tries += 1;
            } //End of while   
            //Close the Datagram Port
            System.out.println("End of Connect Thread");
            c.close();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } //End of try
    } //End of run 
    
    /**
     * Function used to return a HashMap of contacted Peers.
     * Key: String - HostAddress of known Peers in String format
     * Value: Integer - Port of known Peers.
     * 
     * @return HashMap of contacted peers.
     */
    public HashMap<String, Integer> getPeers(){
        return this.peers;
    }
} //End of clss
