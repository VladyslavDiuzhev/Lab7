package server.commands;

import core.essentials.UserInfo;
import core.essentials.Vehicle;
import core.interact.Message;
import server.commands.interfaces.Command;

import java.util.Objects;
import java.util.Stack;

public class Authorize implements Command {
    private UserInfo userInfo;
    public Authorize(UserInfo userInfo){
        this.userInfo = userInfo;
    }

    @Override
    public Message execute(Stack<Vehicle> stack) {
        if (Objects.equals(userInfo.getLogin(), "login") && Objects.equals(userInfo.getPassword(), "password")){
            return new Message("Авторизация пройдена!", true);
        } else{
            return new Message("Авторизация не пройдена!", true);
        }

    }
}
