package service;

import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import model.UserData;
import model.AuthData;

/**
 * Handles user registration.
 * Validates input, saves the user, and creates an authentication token.
 */
public class RegisterService {

    private final MemoryDataAccess dao;

    public RegisterService(MemoryDataAccess dao) {
        this.dao = dao;
    }

    /**
     * Registers a new user.
     *
     * @param request the user information (username, password, email)
     * @return an AuthData containing the authToken and username
     * @throws DataAccessException if input is bad or user already exists
     */
    public AuthData register(UserData request) throws DataAccessException {
        // 1️⃣ Validate input
        if (request.username() == null || request.username().isEmpty() ||
                request.password() == null || request.password().isEmpty() ||
                request.email() == null || request.email().isEmpty()) {
            throw new DataAccessException("bad request");
        }

        // 2️⃣ Check if the username already exists
        if (dao.getUser(request.username()) != null) {
            throw new DataAccessException("already taken");
        }

        // 3️⃣ Add user to database
        dao.insertUser(request);

        // 4️⃣ Create and return auth token
        return dao.createAuth(request.username());
    }
}
