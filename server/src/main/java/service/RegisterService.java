package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.UserData;
import model.AuthData;

public class RegisterService {

    private final DataAccess dao;

    public RegisterService(DataAccess dao) {
        this.dao = dao;
    }

    public AuthData register(UserData request) throws DataAccessException {
        if (request.username() == null || request.username().isEmpty() ||
                request.password() == null || request.password().isEmpty() ||
                request.email() == null || request.email().isEmpty()) {
            throw new DataAccessException("bad request");
        }

        if (dao.getUser(request.username()) != null) {
            throw new DataAccessException("already taken");
        }

        dao.createUser(request);

        String token = java.util.UUID.randomUUID().toString();
        AuthData auth = new AuthData(token, request.username());
        dao.createAuth(auth);
        return auth;
    }

}
