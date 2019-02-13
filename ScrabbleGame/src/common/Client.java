package common;
//Group: 		Pelican
//Names: 		Ming Jin, Kangping Xue, Yang Hong, Sharon Stratsianis
//Purpose: 		provides the client socket communication interface with the server
//Description: 	This file contains both the abstract Client class and the private class 
//				ConnectionToHub

import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

//Name: 		Client
//Description: 	Connected Players
//				Need to provide the hostName, Port, and userName
abstract public class Client {

    public ArrayList<User> connectedPlayers;

    public User currenyUser;

    private final ConnectionToHub connection;


    public Client(String hubHostName, int hubPort,String name) throws IOException {
        connectedPlayers=new ArrayList<>(0);
        currenyUser=new User(name);
        connection = new ConnectionToHub(hubHostName, hubPort);
    }

    //Name: 		messageReceived
    //Description: 	Receives a message from the server   
    abstract protected void messageReceived(String message);

    //Name:			playerConnected
    //Description:	Connects a player to the list of players, by their id    
    protected void playerConnected(int newPlayerID) { }

    //Name:			playerDisconnected
    //Description:	Disconnects a player to the list of players, by their id
    protected void playerDisconnected(int departingPlayerID) { }
    
    //Name: 		connectionClosedByError
    //Description:	Handles errored connections
    protected void connectionClosedByError(String message) { }
    
    //Name: 		serverShutdown
    //Description:	Server Shutdown
    protected void serverShutdown(String message) { }

    //Name:			disconnect
    //Descripton:	Disconnects from server
    public void disconnect() {
        if (!connection.closed)
            connection.send(new DisconnectMessage("Goodbye Hub"));

    }
    //Name:			send
    //Description:	sends a message Object to the server
    public void send(SendObject message) {
        if (message == null)
            throw new IllegalArgumentException("Null cannot be sent as a message.");
        if (! (message instanceof SendObject))
            throw new IllegalArgumentException("Messages must implement the SendObject class.");
        if (connection.closed)
            throw new IllegalStateException("Message cannot be sent because the connection is closed.");
        connection.send(message);
    }
    
    //Name:			ConnectionToHub
    //Description:	Private class handling the connection to the server
    private  class ConnectionToHub {

        private final Socket socket;               // The socket that is connected to the Hub.
        private final DataInputStream in;        // A stream for sending messages to the Hub.
        private final DataOutputStream out;      // A stream for receiving messages from the Hub.
        private final SendThread sendThread;       // The thread that sends messages to the Hub.
        private final ReceiveThread receiveThread; // The thread that receives messages from the Hub.

        private final LinkedBlockingQueue<SendObject> outgoingMessages;  // Queue of messages waiting to be transmitted.

        private volatile boolean closed;     // This is set to true when the connection is closing.
                                             // For one thing, this will prevent errors from being
                                             // reported when exceptions are generated because the
                                             // connection is being closed in the normal way.
        
        //Name:			ConnectionToHub Constructor
        //Description:	Provides setup for a connection and connects to the server
        //		        with a host and port.
        ConnectionToHub(String host, int port) throws IOException {
            outgoingMessages = new LinkedBlockingQueue<SendObject>();
            socket = new Socket(host,port);
            out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF("NewUser#"+currenyUser.name);
            out.flush();
            in = new DataInputStream(socket.getInputStream());
            try {
                currenyUser.id=in.readInt();
            }
            catch (Exception e){
                throw new IOException("Illegal response from server.");
            }

            sendThread = new SendThread();
            receiveThread = new ReceiveThread();
            sendThread.start();
            receiveThread.start();
        }

        //Name:			close
        //Description:	Closes a thread
        void close() {
            closed = true;
            sendThread.interrupt();
            receiveThread.interrupt();
            try {
                socket.close();
            }
            catch (IOException e) {
            }
        }
        
        //Name: 		send
        //Description:	sends a message object to the server
        void send(SendObject message) {
            outgoingMessages.add(message);
        }
        
        //Name:			closedByError
        //Description:	close a connection nicely that has had an error
        synchronized void closedByError(String message) {
            if (! closed ) {
                connectionClosedByError(message);
                close();
            }
        }
        
        //Name:			SendThread
        //Description:	private class handling sending threads
        private class SendThread extends Thread {
            public void run() {
                System.out.println("Client send thread started.");
                try {
                    while ( ! closed ) {
                        SendObject message = outgoingMessages.take();
                        out.writeUTF(message.toJsonString());
                        out.flush();
                        if (message.type.equals("DisconnectMessage")) {
                            close();
                        }
                    }
                }
                catch (IOException e) {
                    if ( ! closed ) {
                        closedByError("IO error occurred while trying to send message.");
                        System.out.println("Client send thread terminated by IOException: " + e);
                    }
                }
                catch (Exception e) {
                    if ( ! closed ) {
                        closedByError("Unexpected internal error in send thread: " + e);
                        System.out.println("\nUnexpected error shuts down client send thread:");
                        e.printStackTrace();
                    }
                }
                finally {
                    System.out.println("Client send thread terminated.");
                }
            }
        }
        
        //Name:			ReceiveThread
        //Description:	private class handling receiving threads
        private class ReceiveThread extends Thread {
            public void run() {
                System.out.println("Client receive thread started.");
                try {
                    while ( ! closed ) {
                        String content=in.readUTF();
                        SendObject obj=new Gson().fromJson(content,SendObject.class);
                        if (obj.type.equals("DisconnectMessage")) {
                            close();
                            serverShutdown(new Gson().fromJson(content,DisconnectMessage.class).message);
                        }
                        else if (obj.type.equals("StatusMessage")) {
                            StatusMessage msg = new Gson().fromJson(content,StatusMessage.class);
                            connectedPlayers = msg.players;
                            if (msg.connecting)
                                playerConnected(msg.playerID);
                            else
                                playerDisconnected(msg.playerID);
                        }
                        else
                            messageReceived(content);
                    }
                }
                catch (IOException e) {
                    if ( ! closed ) {
                        closedByError("IO error occurred while waiting to receive  message.");
                        System.out.println("Client receive thread terminated by IOException: " + e);
                    }
                }
                catch (Exception e) {
                    if ( ! closed ) {
                        closedByError("Unexpected internal error in receive thread: " + e);
                        System.out.println("\nUnexpected error shuts down client receive thread:");
                        e.printStackTrace();
                    }
                }
                finally {
                    System.out.println("Client receive thread terminated.");
                }
            }
        }
        
    } // end nested class ConnectionToHub

}