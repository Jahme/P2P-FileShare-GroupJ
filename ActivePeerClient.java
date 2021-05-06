/**
 * <h1> ActivePeerClient </h1>
 * This Java class is responsible for initiating active TCP socket connections to other clients on LAN.
 * 
 */
package Current;
import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ActivePeerClient extends Thread{ //or implements Runnable
    
    private Socket peerSocket;
    private String process;
    private String activeFile = null;
    
    //Constructor used general use. Called upon SYNC command from AppClient
    public ActivePeerClient(String hostName, int hostPort, String process){
        try{
            this.peerSocket = new Socket(hostName, hostPort);
            this.process = process;
        } catch(IOException e){
            System.out.println("IOException: " + e);
        }
    }
    
    //Constructor used for direct file use. Called upon PUT and GET command from AppClient
    public ActivePeerClient(String hostName, int hostPort, String process, String fileName){
        try{
            this.peerSocket = new Socket(hostName, hostPort);
            this.process = process;
            this.activeFile = fileName;
        } catch(IOException e){
            System.out.println("IOException: " + e);
        }
    }
    
    // Process of execution when starting this class object as new Thread
    @Override
    public void run(){
        try{ 
            //Initialize the data incoming stream for the socket
            InputStream incoming = this.peerSocket.getInputStream();
            DataInputStream receive = new DataInputStream(incoming);
            if(this.process.equals("GET")){ // Request a file
                File aFile = new File(activeFile); 
                getCmd(receive, activeFile); //Command to obtain file
            }
            else if(this.process.equals("PUT")){ // Send a file
                File aFile = new File(activeFile);
                putCmd(activeFile,aFile.length()); //Command to send file.
            }
            else if(this.process.equals("SYN")){ 
                syncFiles(receive); //Command to perform a sync between a pair of connected peers
            }
            //Necessary Termination
            receive.close();
            incoming.close();
            this.peerSocket.close();
            System.out.println("End of ActivePeerClient Thread");
        } catch (IOException e){
            System.out.println("IOException: " + e.getMessage());
        } catch (InterruptedException ex) {
            Logger.getLogger(ActivePeerClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    //Incoming File
    private void getCmd(DataInputStream in, String fileName){
        int bytesRead;
        File file = new File(fileName); //Name to store new file
        try {
            //Prepare OutputStream
            OutputStream os = this.peerSocket.getOutputStream();
            DataOutputStream dos = new DataOutputStream(os);
            //Send Command:FileName
            String cmdFile = "GET:" + fileName;
            dos.writeUTF(cmdFile);
            //Prepare file
            OutputStream output = new FileOutputStream(file);
            //Send command and fileName
            //Receive file
            long fileSize = in.readLong(); //Read file size
            byte[] buffer = new byte[1024];
            while(fileSize > 0 && (bytesRead = in.read(buffer, 0, (int)Math.min(buffer.length, fileSize))) != -1){ //Receive file for specified file size
                output.write(buffer, 0, bytesRead);
                fileSize -= bytesRead;
            }
            output.flush();
            output.close();
        } catch (FileNotFoundException ex) {
            System.out.println("FileNotFoundException: " + ex);
        } catch (IOException ex) {
            System.out.println("IO Exception: " + ex);
        }
    }
    
    //Outgoing File
    private void putCmd(String fileName, long fileSize){
        //Send command "PUT"
        try{
            //Prepare OutputStream
            OutputStream os = this.peerSocket.getOutputStream();
            DataOutputStream dos = new DataOutputStream(os);
            byte[] buffer = new byte[(int) fileSize];
            FileInputStream fis = new FileInputStream(fileName);
            BufferedInputStream bis = new BufferedInputStream(fis);
            DataInputStream dis = new DataInputStream(bis);
            dis.readFully(buffer, 0, buffer.length); //Load file into buffer
            //Send command and fileName
            String cmdFile = "PUT:" + fileName;
            dos.writeUTF(cmdFile);
            //Send file size
            dos.writeLong(fileSize);
            //Send file
            dos.write(buffer, 0, buffer.length); //
            dos.flush();
            dos.close();
        } catch (IOException e){
            System.out.println("IOException: " + e);
        }
    }
    
    //Sync Files
    private void syncFiles(DataInputStream in) throws InterruptedException{ //Corresponds with ActivePeerThread.sync() function
        //Load in directory
        File mainDir = new File(".\\files");
        File [] files = mainDir.listFiles();
        try{
            // Set up sending and receiving
            OutputStream os = this.peerSocket.getOutputStream();
            DataOutputStream dos = new DataOutputStream(os); //For sending communications to socket
            dos.writeUTF("SYN"); //Send sync command
            //Individual File Handler
            int sentinel = 0;
            for(File file: files){ //For each file in fileList
                String fileName = file.getName();
                long fileSize = file.length();
                long fileModified = file.lastModified();
                // Send fileName and check if connected peer has said file.
                dos.writeUTF(fileName);
                // Get file check response
                String fileCheck = in.readUTF();
                switch (fileCheck) {
                    case "HAS":
                        //Peer has same fileName. Compare and decide.
                        //Send timestamp. Comparison to-be-made on peer end.
                        dos.writeLong(fileModified);
                        String modCheck = in.readUTF();
                        //Determine whether to receive or send the most recent file version
                        if(modCheck.equals("NEW")){ //We have newer file. We shall send.
                            // Prepare file for sending
                            byte[] fileBuffer = new byte[(int) fileSize];
                            //FileInputStream fis = new FileInputStream(file);
                            //BufferedInputStream bis = new BufferedInputStream(fis);
                            DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
                            dis.readFully(fileBuffer, 0, fileBuffer.length); //Load in file to buffer
                            //Send file size
                            dos.writeLong(fileSize);
                            //Send file
                            dos.write(fileBuffer, 0, fileBuffer.length);
                            dis.close();
                        }
                        else if(modCheck.equals("OLD")){ //We have older file. We shall receive.
                            int bytesToRead;
                            //Prepare file
                            OutputStream output = new FileOutputStream(file); //Set up file to store
                            //Receive file
                            long incomingSize = in.readLong(); //Read file size
                            byte[] buffer = new byte[1024];
                            while(incomingSize > 0 && (bytesToRead = in.read(buffer, 0, (int)Math.min(buffer.length, incomingSize))) != -1){
                                output.write(buffer, 0, bytesToRead);
                                incomingSize -= bytesToRead;
                            }
                            output.close();
                        }
                        else{
                            System.out.println("ModCheck Error: ActivePeerClient.java syncFiles() function");
                        }   break;
                    case "NOT":
                        //Peer does not have same fileName. Send file.
                        // Prepare file for sending
                        byte[] fileBuffer = new byte[(int) fileSize];
                        //FileInputStream fis = new FileInputStream(file);
                        //BufferedInputStream bis = new BufferedInputStream(fis);
                        DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
                        dis.readFully(fileBuffer, 0, fileBuffer.length); //Load in file to buffer
                        //Send file size
                        dos.writeLong(fileSize);
                        //Send file
                        dos.write(fileBuffer, 0, fileBuffer.length);
                        dis.close();
                        break;
                    default:
                        System.out.println("FileCheck Error: ActivePeerClient.java syncFiles() function");
                        break;
                }
                sentinel++;
                System.out.println(sentinel);
                if(sentinel == files.length){
                    //Send NEX command
                    dos.writeUTF("NEX");
                    System.out.println("Sent NEX");
                    break; //Break for loop to avoid EoF
                }                               
                //End of file check switch case   
            } //End of file list processing [For-loop]
            //Check if local system does not have files that connected Peer has:
            int missingCount = in.readInt();
            System.out.println("Missing Count:" + missingCount);
            for(int m = 0; m < missingCount; m++){
                //Receive Files
                int bytesToRead;
                //Receive file name
                String fileName = in.readUTF();
                File newFile = new File(".\\files\\" + fileName);
                //Prepare file
                OutputStream output = new FileOutputStream(newFile); //Set up file to store
                //Receive file size and data
                long incomingSize = in.readLong(); //Read file size
                byte[] buffer = new byte[1024];
                while(incomingSize > 0 && (bytesToRead = in.read(buffer, 0, (int)Math.min(buffer.length, incomingSize))) != -1){
                    output.write(buffer, 0, bytesToRead);
                    incomingSize -= bytesToRead;
                    }
                output.close();
            }           
            dos.close(); //Close outbound communication  
        } catch( EOFException ioE){
            System.out.println("End of Files");
        } catch (IOException ioE){
            System.out.println("IOException: " + ioE.getMessage());
        }
        
        
    }
}
