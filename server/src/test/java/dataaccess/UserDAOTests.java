package dataaccess;

import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UserDAOTests {

    private MySQLDataAccess dao;

    @BeforeEach
    public void setup() throws DataAccessException {
        dao = new MySQLDataAccess();
        dao.clear();
    }

    @Test
    public void createUserPositive() throws DataAccessException {
        var user = new UserData("bob", "password123", "bob@example.com");
        dao.createUser(user);

        var result = dao.getUser("bob");
        assertNotNull(result, "User should exist in database after creation");
        assertEquals("bob", result.username());
        assertEquals("bob@example.com", result.email());
    }

    @Test
    public void createUserNegativeDuplicateUsername() throws DataAccessException {
        var user1 = new UserData("alice", "pw", "a@x.com");
        dao.createUser(user1);

        var user2 = new UserData("alice", "pw2", "a2@x.com");

        assertThrows(DataAccessException.class, () -> dao.createUser(user2));
    }
}
