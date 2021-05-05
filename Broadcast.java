/**
 * <h1> Broadcast </h1>
 * This Java class is responsible for making the client available to other clients on the same LAN.
 * Accomplished via UDP broadcasting, with request from Clients using "Trigger Network Update" feature through Main Client and sending them responses.
 * 
 */

package Current;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;

public class Broadcast implements Runnable{
    
    private HashMap<String, Integer> contacted = new HashMap<>(); //List of contacted peers.
    private DatagramSocket socket; //Broadcast Socket that makes Peer Available to Network
    
    @Override
    public void run() {
        try{
            //Establish open socket to listen to all UDP traffic for this port
            socket = new DatagramSocket(3275, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);
            
            while(true){ //Poll for connection packets sent by other peers on LAN.              
                //Receive Packet
                byte[] recvBuf = new byte[15000];
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                socket.receive(packet); //Blocking receive
                //Process Packet Information i.e. Address & Port
                InetAddress packetAddress = packet.getAddress();                
                int packetPort = packet.getPort();
                String packetHost = packetAddress.getHostAddress();
                //Append peer to list of attempted contacts
                this.contacted.put(packetAddress.toString(), packetPort); 
                
                
                //See if the packet holds the JOIN_NETWORK command
                String message = new String(packet.getData()).trim();
                if(message.equals("JOIN_NETWORK_CONNECT")){
                    //Send response acknowledging connection
                    byte[] sendData = "JOIN_ACKNOWLEDGED".getBytes();
                    
                    //Send a response
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packetAddress, packetPort);
                    socket.send(sendPacket);
                    
                    //Add contacted to HashMap of known peers.
                    contacted.put(packetHost, packetPort);
                }
                
                //LEAVE HANDLER: Executed when a peer decides to leave the network.
                //NOT FULLY IMPLEMENTED
                else if(message.equals("LEAVE_NETWORK")){ 
                    if(contacted.containsKey(packetHost)) contacted.remove(packetHost);
                    
                    //Send a response
                    byte[] sendData = "LEAVE_ACKNOWLEDGED".getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packetAddress, packetPort);
                    socket.send(sendPacket);
                }
            }
        } catch (IOException e){
            System.out.println(e.getMessage());
        }
    }
    
    /**
     * Function used to return a HashMap of contacted Peers.
     * Key: String - HostAddress of known Peers in String format
     * Value: Integer - Port of known Peers.
     * 
     * @return HashMap of contacted peers.
     */
    public HashMap<String, Integer> getContacted(){
        return this.contacted;
    }
    
    /**
     * Function used to close the Broadcast. Initiated when program exits.
     * 
     */
    public void closeBroadcast(){
        this.socket.close();
    }
}
