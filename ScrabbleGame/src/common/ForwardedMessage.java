package common;
//Group: 	Pelican
//Names: 	Ming Jin, Kangping Xue, Yang Hong, Sharon Stratsianis
//Purpose: 	Stores the message and client details for a message that needs to sent
//			to other clients

public class ForwardedMessage extends SendObject{
    
    public final String message;  // Original message from a client.
    public final int senderID;    // The ID of the client who sent that message.


    public ForwardedMessage(int senderID, String message) {
        super.type="ForwardedMessage";

        this.senderID = senderID;
        this.message = message;
    }

}