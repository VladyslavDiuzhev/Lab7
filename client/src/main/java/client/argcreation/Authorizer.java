package client.argcreation;

import core.essentials.UserInfo;
import core.interact.ConsoleInteractor;
import core.interact.UserInteractor;

public abstract class Authorizer {

    public static UserInfo getUserInfo(UserInteractor interactor) {
        interactor.broadcastMessage("Введите логин: ", false);
        String login = interactor.getData();
        interactor.broadcastMessage("Введите пароль: ", false);
        String password = interactor.getSecureData();
        return new UserInfo(login, password);
    }
}
