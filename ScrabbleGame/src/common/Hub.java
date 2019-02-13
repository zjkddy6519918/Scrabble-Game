package common;
//Group: 	Pelican
//Names: 	Ming Jin, Kangping Xue, Yang Hong, Sharon Stratsianis
//Purpose: 	Server common files

import com.google.gson.Gson;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

//Name:			Hub
//Description:	Maintains the client connections
public class Hub {
    

    private TreeMap<Integer, ConnectionToClient> playerConnections;
    

    private LinkedBlockingQueue<Message> incomingMessages;
    
    private ServerSocket serverSocket;  // Listens for connections.
    private Thread serverThread;        // Accepts connections on serverSocket
    volatile private boolean shutdown;  // Set to true when the Hub is not listening.
    
    private int nextClientID = 1;  // The id number that will be assigned to

    
    //Name:			Hub Constructor
    //Description:	Contruct the clients list and thread for listening
    public Hub(int port) throws IOException {
        playerConnections = new TreeMap<Integer, ConnectionToClient>();
        incomingMessages = new LinkedBlockingQueue<Message>();
        serverSocket = new ServerSocket(port);
        System.out.println("Listening for client connections on port " + port);
        serverThread = new ServerThread();//used to accept connections with clients
        serverThread.start();
        Thread readerThread = new Thread(){
            public void run() {
               while (true) {
                   try {
                       Message msg = incomingMessages.take();
                       messageReceived(msg.playerConnection, msg.message);
                   }
                   catch (Exception e) {
                       System.out.println("Exception while handling received message:");
                       e.printStackTrace();
                   }
               }
           }
        };
        readerThread.setDaemon(true);
        readerThread.start();
    }
    
    //Name:				messageReceived
    //Description:		receive message and send to all players
    protected void messageReceived(int playerID, String message) {
        sendToAll(new ForwardedMessage(playerID,message));
    }
    
    //Name:				playerConnected
    //Description:		connect a player
    protected void playerConnected(int playerID) {
    }
    
    //Name:				playerDisconnected
    //Description:		Disconnect a player
    protected void playerDisconnected(int playerID) {
    }
        
    //Name:				getPlayerList
    //Description:		get the list of players synchronized
    synchronized public ArrayList<User> getPlayerList() {
        ArrayList<User> players=new ArrayList<>(playerConnections.size());

        for (ConnectionToClient client : playerConnections.values()){
            if(!client.user.isGaming){
                players.add(client.user);
            }
        }

        return players;
    }

    //Name:				setOnlinePlayers
    //Description:		set the players who are going to play
    synchronized public void setOnlinePlayers(ArrayList<User> players) {
        for(User user:players){
            for (ConnectionToClient client : playerConnections.values()){
                if(client.user.id==user.id){
                    client.user.isGaming=true;
                }
            }
        }
    }
    
    //Name:				shutdownServerSocket
    //Description:		close the server listening for connections
    public void shutdownServerSocket() {
        if (serverThread == null)
            return;
        incomingMessages.clear();
        shutdown = true;
        try {
            serverSocket.close();
        }
        catch (IOException e) {
        }
        serverThread = null;
        serverSocket = null;
    }
    
    //Name:				sendToAll
    //Description:		send a message to all players synchronized
    synchronized public void sendToAll(SendObject message) {
        if (message == null)
            throw new IllegalArgumentException("Null cannot be sent as a message.");
        if ((message.type.equals("null")) )
            throw new IllegalArgumentException("Messages must implement the SendObject class.");
        for (ConnectionToClient pc : playerConnections.values())
            pc.send(message);
    }

    //Name:				sendToOne
    //Description:		sent a message to one client synchonized
    synchronized public boolean sendToOne(int recipientID, SendObject message) {
        if (message == null)
            throw new IllegalArgumentException("Null cannot be sent as a message.");
        if ((message.type.equals("null")))
            throw new IllegalArgumentException("Messages must implement the SendObject class.");
        ConnectionToClient pc = playerConnections.get(recipientID);
        if (pc == null)
            return false;
        else {
            pc.send(message);
            return true;
        }
    }

    //Name:				messageReceived
    //Description:		Receive a message from a client synchronized
    synchronized private void messageReceived(ConnectionToClient fromConnection, String message) {

        int sender = fromConnection.getPlayer();
        messageReceived(sender,message);
    }
    
    //Name:				acceptConnection
    //Description:		accept a connection from a client synchronized
    synchronized private void acceptConnection(ConnectionToClient newConnection) {
        int ID = newConnection.getPlayer();
        playerConnections.put(ID,newConnection);
        StatusMessage sm = new StatusMessage(ID,true,getPlayerList());
        sendToAll(sm);
        playerConnected(ID);
        System.out.println("Connection accepted from client number " + ID);
    }
    
    //Name:				clientDisconnected
    //Description:		disconnect a client from the server synchronized
    synchronized private void clientDisconnected(int playerID) {
        if (playerConnections.containsKey(playerID)) {
            playerConnections.remove(playerID);
            StatusMessage sm = new StatusMessage(playerID,false,getPlayerList());
            sendToAll(sm);
            playerDisconnected(playerID);
            System.out.println("Connection with client number " + playerID + " closed by DisconnectMessage from client.");
        }
    }
    
    //Name:				connectionToClientClosedWithError
    //Description:		close a connection with the client that happened in error.
    synchronized private void connectionToClientClosedWithError( ConnectionToClient playerConnection, String message ) {
        int ID = playerConnection.getPlayer();
        if (playerConnections.remove(ID) != null) {
            StatusMessage sm = new StatusMessage(ID,false,getPlayerList());
            sendToAll(sm);
        }
    }
    
    //Name:				Message class
    private class Message {
        ConnectionToClient playerConnection;
        String message;
    }
    
    //Name:				ServerThread Class
    private class ServerThread extends Thread {  // Listens for connection requests from clients.
        public void run() {
            try {
                while ( ! shutdown ) {
                    Socket connection = serverSocket.accept();
                    if (shutdown) {
                        System.out.println("Listener socket has shut down.");
                        break;
                    }
                    new ConnectionToClient(incomingMessages,connection);
                }
            }
            catch (Exception e) {
                if (shutdown)
                    System.out.println("Listener socket has shut down.");
                else
                    System.out.println("Listener socket has been shut down by error: " + e);
            }
        }
    }
    
    //Name:			ConnectionToClient Class  
    private class ConnectionToClient { // Handles communication with one client.

        private User user;
        private BlockingQueue<Message> incomingMessages;
        private LinkedBlockingQueue<SendObject> outgoingMessages;
        private Socket connection;
        private DataInputStream in;
        private DataOutputStream out;
        private volatile boolean closed;  // Set to true when connection is closing normally.
        private Thread sendThread; // Handles setup, then handles outgoing messages.
        private volatile Thread receiveThread; // Created only after connection is open.
        
        //Name:			ConnectionToClient Constructor
        ConnectionToClient(BlockingQueue<Message> receivedMessageQueue, Socket connection)  {
            this.connection = connection;
            incomingMessages = receivedMessageQueue;
            outgoingMessages = new LinkedBlockingQueue<SendObject>();
            sendThread =  new SendThread();
            sendThread.start();
        }
        
        //Name:			getPlayer
        //Description:	get a Player        
        int getPlayer() {
            return user.id;
        }
  
        //Name:			close
        //Description:	close a thread
        void close() {
            closed = true;
            sendThread.interrupt();
            if (receiveThread != null)
                receiveThread.interrupt();
            try {
                connection.close();
            }
            catch (IOException e) {
            }
        }
        
        //Name:			send
        //Description:	send a single message to the queue 
        void send(SendObject obj) { // Just drop message into message output queue.
            if (obj.type.equals("DisconnectMessage")) {
                // A signal to close the connection;
                // discard other waiting messages, if any.
                outgoingMessages.clear();
            }
            outgoingMessages.add(obj);
        }
        
        //Name:			closeWithError
        //Description:	close a client that errored
        private void closedWithError(String message) {
            connectionToClientClosedWithError(this, message);
            close();
        }

        //Name:			sendThread
        //Description:	Handles sending Threads
        private class SendThread extends Thread {
            public void run() {
                try {
                    out = new DataOutputStream(connection.getOutputStream());
                    in = new DataInputStream(connection.getInputStream());
                    String handle = in.readUTF(); // first input must be "Hello Hub"
                    if(!handle.startsWith("NewUser")){
                        throw new Exception("Incorrect hello string received from client.");
                    }

                    user=new User(handle.split("#")[1]);
                    synchronized(Hub.this) {
                        user.id=nextClientID++;
                    }
                    out.writeInt(user.id);  // send playerID to the client.
                    out.flush();
                    acceptConnection(ConnectionToClient.this);
                    receiveThread = new ReceiveThread();
                    receiveThread.start();
                }
                catch (Exception e) {
                    try {
                        closed = true;
                        connection.close();
                    }
                    catch (Exception e1) {
                    }
                    System.out.println("\nError while setting up connection: " + e);
                    e.printStackTrace();
                    return;
                }
                try {
                    while ( ! closed ) {  // Get messages from outgoingMessages queue and send them.
                        try {
                            SendObject message = outgoingMessages.take();
                            out.writeUTF(message.toJsonString());
                            out.flush();
                            if (message.type.equals("DisconnectMessage")) // A signal to close the connection.
                                close();
                        }
                        catch (InterruptedException e) {
                            // should mean that connection is closing
                        }
                    }    
                }
                catch (IOException e) {
                    if (! closed) {
                        closedWithError("Error while sending data to client.");
                        System.out.println("Hub send thread terminated by IOException: " + e);
                    }
                }
                catch (Exception e) {
                    if (! closed) {
                        closedWithError("Internal Error: Unexpected exception in output thread: " + e);
                        System.out.println("\nUnexpected error shuts down hub's send thread:");
                        e.printStackTrace();
                    }
                }
            }
        }
        
        //Name:			ReceiveThread Class
        //Description	Handles receiving threads
        private class ReceiveThread extends Thread {
            public void run() {
                try {
                    while ( ! closed ) {
                        try {
                            String content=in.readUTF();
                            SendObject message = new Gson().fromJson(content,SendObject.class);
                            Message msg = new Message();
                            msg.playerConnection = ConnectionToClient.this;
                            msg.message = content;
                            if ( ! (message.type.equals("DisconnectMessage")) )
                                incomingMessages.put(msg);
                            else {
                                closed = true;
                                outgoingMessages.clear();
                                out.writeUTF(new DisconnectMessage("*goodbye*").toJsonString());
                                out.flush();
                                clientDisconnected(user.id);
                                close();
                            }
                        }
                        catch (InterruptedException e) {
                            // should mean that connection is closing
                        }
                    }
                }
                catch (IOException e) {
                    if (! closed) {
                        closedWithError("Error while reading data from client.");
                        System.out.println("Hub receive thread terminated by IOException: " + e);
                    }
                }
                catch (Exception e) {
                    if ( ! closed ) {
                        closedWithError("Internal Error: Unexpected exception in input thread: " + e);
                        System.out.println("\nUnexpected error shuts down hub's receive thread:");
                        e.printStackTrace();
                    }
                }
            }
        }
        
    }  // end nested class ConnectionToClient

    
}