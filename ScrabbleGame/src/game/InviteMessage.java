package game;
//Group: 	Pelican
//Names: 	Ming Jin, Kangping Xue, Yang Hong, Sharon Stratsianis
//Purpose: 	Invite message extension of SendOjbect so it is JSON to be sent

import common.SendObject;

import java.util.ArrayList;

public class InviteMessage extends SendObject {

    public ArrayList<Integer> players;
    public int id;
    public int target;

    public InviteMessage(int id,ArrayList<Integer> players){
        super.type="InviteMessage";
        this.id=id;
        this.players=players;
    }

    public InviteMessage(int id,int target){
        super.type="InviteMessage";
        this.id=id;
        this.target=target;
    }
}
