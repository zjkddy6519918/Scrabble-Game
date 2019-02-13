package game;
//Group: 	Pelican
//Names: 	Ming Jin, Kangping Xue, Yang Hong, Sharon Stratsianis
//Purpose: 	Invite back message extension of SendOjbect so it is JSON to be sent

import common.SendObject;

public class InviteBackMessage extends SendObject {
    public GameStatus state;

    public int id;

    public InviteBackMessage(int id,GameStatus state){
        super.type="InviteBackMessage";
        this.id=id;
        this.state=state;
    }
}
