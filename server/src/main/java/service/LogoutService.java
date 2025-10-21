package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;

public class LogoutService {

    private final DataAccess dao;

    public LogoutService(DataAccess dao) {
        this.dao = dao;
    }

    public void logout(String authToken) throws DataAccessException {
        var auth = dao.getAuth(authToken);
        if (auth == null) {
            throw new DataAccessException("unauthorized");
        }

        dao.deleteAuth(authToken);
    }
}
