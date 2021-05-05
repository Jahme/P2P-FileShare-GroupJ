/**
 * <h1> ActivePeerThread </h1>
 * This Java class is responsible for handling ServerSided communication of TCP sockets.
 * Serves as an interface for ActivePeerClients to handle and process requests to a peer.
 * 
 */
package Current;
import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;

public class ActivePeerThread extends Thread{

    private Socket peer;
    
    public ActivePeerThread(Socket socket){
        this.peer = socket;
    }
    
    public Socket getSocket(){
        return this.peer;
    }
    
    //Thread of execution function. Upon constructing PeerDiscoveryThread, this is executed.
    @Override
    public void run() {
        try {
            // Obtain Command
            InputStream incoming = this.peer.getInputStream();
            DataInputStream receive = new DataInputStream(incoming);
            String commandReceived = receive.readUTF();
            System.out.println("Command Received -- " + commandReceived);
            // Command Handler
            if (commandReceived.substring(0, 3).equals("GET")){ //Get from server. Server will send file given file name.
                String fileName = commandReceived.substring(4);
                sendFile(fileName);
                incoming.close();
                receive.close();
            }
            else if(commandReceived.substring(0, 3).equals("PUT")){ //Put to server. Server will receive and store file given file name.
                String fileName = commandReceived.substring(4);
                receiveFile(receive, fileName);
                incoming.close();
                receive.close();
            }
            else if(commandReceived.equals("SYN")){
                sync(receive);
            }
            receive.close();
            incoming.close();
            this.peer.close();
        } catch (IOException ex) {
            System.out.println("IOException: " + ex.getMessage());
        }
        System.out.println("End of ActivePeerThread");
    } //End of run()
    
    //Get File Info
    
    //Send File (Used in "Get" Command)
    private void sendFile(String name){
        
        try{
            File fileToSend = new File(name);
            OutputStream os = this.peer.getOutputStream();
            DataOutputStream dos = new DataOutputStream(os);
            byte[] buffer = new byte[(int) fileToSend.length()];
            FileInputStream fis = new FileInputStream(name);
            BufferedInputStream bis = new BufferedInputStream(fis);
            DataInputStream dis = new DataInputStream(bis);
            dis.readFully(buffer, 0, buffer.length); //Load file into buffer
            dis.close();
            //Send command and fileName
            //String cmdFile = "PUT:" + fileName;
            //dos.writeUTF(cmdFile);
            //Send file size
            dos.writeLong(fileToSend.length());
            //Send file
            dos.write(buffer, 0, buffer.length); //Write file to outputstream from buffer
            dos.flush();
            dos.close();
        } catch (IOException e){
            System.out.println("IOException: " + e);
        }
        
    }//End of sendFile()
    
    //Receive File (Used in "Put" Command)
    private void receiveFile(DataInputStream in, String name){
        int bytesRead;
        System.out.println("Receiving File with Name: " + name);
        File file = new File("testReceiveNEW.txt"); //Replace with name
        try {
            OutputStream output = new FileOutputStream(file);
            long fileSize = in.readLong(); //Read file size
            byte[] buffer = new byte[1024];
            while(fileSize > 0 && (bytesRead = in.read(buffer, 0, (int)Math.min(buffer.length, fileSize))) != -1){
                output.write(buffer, 0, bytesRead);
                fileSize -= bytesRead;
            }
            output.close();
        } catch (FileNotFoundException ex) {
            System.out.println("FileNotFoundException: " + ex);
        } catch (IOException ex) {
            System.out.println("IO Exception: " + ex);
        }
    }//End of receiveFile()
    
    private void sync(DataInputStream in){ //Corresponds with ActivePeerClient.sync() function
        //Load in directory (?)
        //File mainDir = new File(".\\files");
        //File [] files = mainDir.listFiles();
        try{
            // Set up sending and receiving
            OutputStream os = this.peer.getOutputStream();
            DataOutputStream dos = new DataOutputStream(os); //For sending communications to socket
            //LOAD Directory
            File filesDir = new File(".\\files");
            ArrayList<String> filesAL = new ArrayList<String>(Arrays.asList(filesDir.list()));
            System.out.println(filesAL.toString());
            while(true){ //IMPORTANT: NEED TO IMPLEMENT BREAK FROM WHILE LOOP WHEN ALL FILES FINISH BEING SYNCED
                //Receive File Name OR NEX command
                String readIn = in.readUTF();
                if(readIn.equals("NEX")) break; //Break from while
                String fileName = ".\\files\\" + readIn;
                
                //Load Local Directory 
                //File [] fileDir = new File(".\\files").listFiles(); //Now loaded before while
                //Check if we have file
                File fileToCheck = new File(fileName);
                System.out.println("FileName: " + fileName);
                if(fileToCheck.exists()){ //File exists. Notify and compare
                    filesAL.remove(fileToCheck.getName()); //Remove file from list
                    //Notify
                    System.out.println("File Exists");
                    dos.writeUTF("HAS");
                    //Obtain last modified date from peer.
                    long fileMod = in.readLong(); //File modification from peer
                    if(fileToCheck.lastModified() < fileMod){ //We have older file. Notify and receive
                        dos.writeUTF("NEW");
                        int bytesToRead;
                        //Prepare file
                        OutputStream output = new FileOutputStream(fileToCheck); //Set up file to store
                        //Receive file
                        long incomingSize = in.readLong(); //Read file size
                        byte[] buffer = new byte[1024];
                        while(incomingSize > 0 && (bytesToRead = in.read(buffer, 0, (int)Math.min(buffer.length, incomingSize))) != -1){
                            output.write(buffer, 0, bytesToRead);
                            incomingSize -= bytesToRead;
                        }
                        output.close();
                    } else{ //We have newer file. Notify and send
                        dos.writeUTF("OLD");
                        // Prepare file for sending
                        byte[] fileBuffer = new byte[(int) fileToCheck.length()];
                        //FileInputStream fis = new FileInputStream(fileToCheck);
                        //BufferedInputStream bis = new BufferedInputStream(fis);
                        DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(fileToCheck)));
                        dis.readFully(fileBuffer, 0, fileBuffer.length); //Load in file to buffer
                        //Send file size
                        dos.writeLong(fileToCheck.length());
                        //Send file
                        dos.write(fileBuffer, 0, fileBuffer.length);
                        dis.close();
                    }
                } else { //File does not exist. Notify Prepare to receive.
                    System.out.println("File Does Not Exist");
                    //Notify
                    dos.writeUTF("NOT");
                    int bytesToRead;
                    //Prepare file
                    OutputStream output = new FileOutputStream(fileToCheck); //Set up file to store
                    //Receive file
                    long incomingSize = in.readLong(); //Read file size
                    byte[] buffer = new byte[1024];
                    while(incomingSize > 0 && (bytesToRead = in.read(buffer, 0, (int)Math.min(buffer.length, incomingSize))) != -1){
                        output.write(buffer, 0, bytesToRead);
                        incomingSize -= bytesToRead;
                    }
                    output.close();
                }
            } //End of while
            
            //Handle files that we have that connecting client does not have
            System.out.println("Missing Files:" + filesAL);
            dos.writeInt(filesAL.size()); //Send number of missing files
            for(String missingfileName: filesAL){
                //Send all missing files back to client
                File missingFile = new File(".\\files\\" + missingfileName);
                if(missingFile.exists()){ //Local exist check
                        // Prepare file for sending
                        byte[] mfileBuffer = new byte[(int) missingFile.length()];
                        //FileInputStream fis = new FileInputStream(fileToCheck);
                        //BufferedInputStream bis = new BufferedInputStream(fis);
                        DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(missingFile)));
                        dis.readFully(mfileBuffer, 0, mfileBuffer.length); //Load in file to buffer
                        //Send file name
                        dos.writeUTF(missingfileName);
                        //Send file size
                        dos.writeLong(missingFile.length());
                        //Send file
                        dos.write(mfileBuffer, 0, mfileBuffer.length);
                        dis.close();
                }
            }
            dos.close(); //Close stream
        } catch (IOException e){
            //System.out.println("IOException ActivePeerThread: " + e.getMessage());
        }
    }
    
    //Helper Functions
    //compareFile(): DEPRECATED
    private int compareFile(String fileName, String remoteAttr){
        String localPath = fileName;
        Path localFile = Paths.get(localPath);
        try{
            FileTime localAttr = Files.getLastModifiedTime(localFile);
            return localAttr.toString().compareTo(remoteAttr);
        } catch(IOException e){
            System.out.println("IOException: " + e.getMessage());
        }
        return 0; //Default return 
    }
}
