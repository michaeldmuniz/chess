package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import model.AuthData;
import model.UserData;
import org.mindrot.jbcrypt.BCrypt;


public class LoginService {

    private final DataAccess dao;

    public LoginService(DataAccess dao) {
        this.dao = dao;
    }

    public AuthData login(String username, String password) throws DataAccessException {
        UserData user = dao.getUser(username);

        if (user == null) {
            throw new DataAccessException("unauthorized");
        }

        if (!BCrypt.checkpw(password, user.password())) {
            throw new DataAccessException("unauthorized");
        }

        AuthData auth = new AuthData(java.util.UUID.randomUUID().toString(), username);
        dao.createAuth(auth);

        return auth;
    }

}
