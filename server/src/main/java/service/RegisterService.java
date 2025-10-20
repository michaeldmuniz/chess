package service;

import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import model.UserData;
import model.AuthData;

public class RegisterService {

    private final MemoryDataAccess dao;

    public RegisterService(MemoryDataAccess dao) {
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

        return dao.createAuth(request.username());
    }
}
