package common;
//Group: 	Pelican
//Names: 	Ming Jin, Kangping Xue, Yang Hong, Sharon Stratsianis
//Purpose: 	Stores the user details of the client/player

public class User {
    public int id;
    public String name;
    public boolean voteRlt;
    public boolean isGaming;
    public int score;

    public User(String name){
        this.name=name;
        id=-1;
        voteRlt=false;
        isGaming=false;
        score=0;
    }

    public User(int id){
        name="";
        this.id=id;
        voteRlt=false;
        isGaming=false;
        score=0;
    }
}
