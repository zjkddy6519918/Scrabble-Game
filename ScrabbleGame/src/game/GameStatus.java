package game;
//Group: 	Pelican
//Names: 	Ming Jin, Kangping Xue, Yang Hong, Sharon Stratsianis
//Purpose: 	Stores the GameStatus to be sent in JSON is an extension of SendObject

import common.SendObject;
import common.User;

import java.util.ArrayList;

/**
 * store the game status
 */
public class GameStatus extends SendObject {

    public char[][] board=new char[20][20];
    public int currentPlayer;
    public boolean gameEndedInTie;
    public boolean pass; //pass or not?
    public int passNum;
    public String cmd;
    public String voteStr;
    public int voteUser;



    public ArrayList<User> players;
    public ArrayList<Integer> voteRlt;
    public int currentVoteRlt;

    //Name:			GameStatus constructor
    //Description:	initialization of the game status
    public GameStatus(){
        super.type="GameStatus";
        currentPlayer=-1;
        gameEndedInTie=false;
        cmd="null";
        voteStr="";
        voteUser=-1;
        currentVoteRlt=-1;
        players=new ArrayList<>();
    }

    //Name:			checkDone
    //Description:	check if the board is full
    public boolean checkDone(){
        int cnt=0;
        for(int x=0;x<20;x++){
            for(int y=0;y<20;y++){
                if(board[x][y]==0){
                    cnt++;
                }
            }
        }
        return cnt==400;
    }

    //Name:			applyMessage
    //Description:	apply the message of the status of the game.
    public void applyMessage(int sender){
        gameEndedInTie=checkDone();
        switch (cmd){
            case "next":
            	if (passNum == players.size()) {
            		gameEndedInTie = true;
            		cmd="null";
            		break;
            	}
                if(sender==currentPlayer){
                    for(int i=0;i<players.size();i++){
                        if(players.get(i).id==currentPlayer){
                            if(i==players.size()-1){
                                currentPlayer=players.get(0).id;
                            }else{
                                currentPlayer=players.get(i+1).id;
                            }
                            break;
                        }
                    }
                    cmd="null";
                }
                break;
            case "voteresult":
                voteUser=sender;
                voteRlt.add(currentVoteRlt);
                break;
            case "votedone":
                int cnt=0;
                for(Integer i : voteRlt){
                    if(i==0){
                        cnt++;
                    }
                }
                if(cnt==players.size()){
                    currentVoteRlt=0;
                    for(User user : players){
                        if(currentPlayer==user.id){
                            user.score+=voteStr.length();
                        }
                    }
                }else{
                    currentVoteRlt=1;
                }
                voteRlt=new ArrayList<>();
                break;
            default:
                break;
        }


    }

}
