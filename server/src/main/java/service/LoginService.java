package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import model.AuthData;
import model.UserData;

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

        if (!user.password().equals(password)) {
            throw new DataAccessException("unauthorized");
        }

        MemoryDataAccess memoryDAO = (MemoryDataAccess) dao;
        AuthData auth = memoryDAO.createAuth(username);


        return auth;
    }
}
