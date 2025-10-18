package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;

/**
 * Handles clearing all data from the database.
 */
public class ClearService {

    private final DataAccess dataAccess;

    public ClearService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    /**
     * Clears all users, auth tokens, and games.
     */
    public void clear() throws DataAccessException {
        dataAccess.clear();
    }
}
