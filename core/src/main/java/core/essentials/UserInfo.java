package core.essentials;

import java.io.Serializable;

public class UserInfo implements Serializable {
    private final String login;
    private final String pass;

    public UserInfo(String login, String pass) {
        this.login = login;
        this.pass = pass;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return pass;
    }
}
