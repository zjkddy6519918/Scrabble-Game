package common;
//Group: 	Pelican
//Names: 	Ming Jin, Kangping Xue, Yang Hong, Sharon Stratsianis
//Purpose: 	Stores the status of the players an extension of SendObject

import java.util.ArrayList;

final class StatusMessage extends SendObject{


    public final int playerID;


    public final boolean connecting;

    public  ArrayList<User> players;
    
    public StatusMessage(int playerID, boolean connecting, ArrayList<User> players) {
        super.type="StatusMessage";
        this.playerID = playerID;
        this.connecting = connecting;
        this.players = players;
    }
    
}