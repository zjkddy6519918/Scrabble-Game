package common;
//Group: 	Pelican
//Names: 	Ming Jin, Kangping Xue, Yang Hong, Sharon Stratsianis
//Purpose: 	Object that will get sent in JSON

import com.google.gson.Gson;

public class SendObject {
    public String type;

    public SendObject(){
        type="null";
    }

    public String toJsonString(){
        return new Gson().toJson(this);
    }
}
