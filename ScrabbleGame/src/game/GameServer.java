package game;
//Group: 	Pelican
//Names: 	Ming Jin, Kangping Xue, Yang Hong, Sharon Stratsianis
//Purpose: 	The game server functions and entry point

import com.google.gson.Gson;
import common.Hub;
import common.SendObject;
import common.User;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

public class GameServer  extends Hub {

    private GameStatus state;

    //Name:			main
    //Description:	entry point
    public static void main(String[] args){
        try {
            new GameServer();
            System.out.println(InetAddress.getLocalHost().getHostAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Name:			GameServer Constructor
    public GameServer() throws IOException {
        super(12345);

    }

    //Name:			messageReceived
    //Description:	receive a message from the client and process the request
    protected void messageReceived(int playerID, String message) {
        SendObject obj=new Gson().fromJson(message,SendObject.class);
        if(obj.type.equals("InviteMessage")){
            state=new GameStatus();
            InviteMessage inviteMessage=new Gson().fromJson(message,InviteMessage.class);
            state.currentPlayer=inviteMessage.id;

            state.players=new ArrayList<>();
            state.players.add(new User(inviteMessage.id));

            for(Integer player : inviteMessage.players){
                state.players.add(new User(player));
            }

            state.voteRlt=new ArrayList<>();
            sendToOne(inviteMessage.id,new InviteBackMessage(inviteMessage.id,state));
            for(Integer player : inviteMessage.players){
                sendToOne(player,new InviteBackMessage(inviteMessage.id,state));
            }
            setOnlinePlayers(state.players);
        }else if(obj.type.equals("GameStatus")){
            state=new Gson().fromJson(message,GameStatus.class);

            state.applyMessage(playerID);
            sendToAll(state);
        }

    }
}
