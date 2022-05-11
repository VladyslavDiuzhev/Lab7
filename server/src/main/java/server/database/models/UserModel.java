package server.database.models;

import java.util.Arrays;

public class UserModel extends Model{
    public UserModel(){
        this.name = "Users";
        this.fields = Arrays.asList(new FieldDB("id", "SERIAL PRIMARY KEY"),
                new FieldDB("login","varchar(256)"),
                new FieldDB("password","varchar(256)"));
    }
}
