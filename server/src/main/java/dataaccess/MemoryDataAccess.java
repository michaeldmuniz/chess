package dataaccess;

import model.UserData;
import model.AuthData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MemoryDataAccess {

    private final Map<String, UserData> users = new HashMap<>();
    private final Map<String, AuthData> authTokens = new HashMap<>();

    // Clears all data (used by the /db endpoint)
    public void clear() throws DataAccessException {
        users.clear();
        authTokens.clear();
    }

    // Adds a new user (throws if username already exists)
    public void insertUser(UserData user) throws DataAccessException {
        if (users.containsKey(user.username())) {
            throw new DataAccessException("already taken");
        }
        users.put(user.username(), user);
    }

    // Gets a user by username
    public UserData getUser(String username) {
        return users.get(username);
    }

    // Creates a new auth token and stores it
    public AuthData createAuth(String username) {
        String token = UUID.randomUUID().toString();
        AuthData auth = new AuthData(token, username);
        authTokens.put(token, auth);
        return auth;
    }

    // Gets an auth token
    public AuthData getAuth(String token) {
        return authTokens.get(token);
    }

    // Deletes an auth token (for logout)
    public void deleteAuth(String token) {
        authTokens.remove(token);
    }
}
