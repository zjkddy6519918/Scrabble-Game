package common;
//Group: 	Pelican
//Names: 	Ming Jin, Kangping Xue, Yang Hong, Sharon Stratsianis
//Purpose: 	Stores the message that lets the server or client know it wants to disconnect

final class DisconnectMessage extends SendObject{


    final public String message;
    

    public DisconnectMessage(String message) {
        super.type="DisconnectMessage";

        this.message = message;
    }
    
}