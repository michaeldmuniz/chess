package dataaccess;

import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthDAOTests {

    private MySQLDataAccess dao;

    @BeforeEach
    public void setup() throws DataAccessException {
        dao = new MySQLDataAccess();
        dao.clear();

        dao.createUser(new UserData("testUser", "password", "test@example.com"));
    }

    @Test
    public void createAuthPositive() throws DataAccessException {
        AuthData auth = new AuthData("token123", "testUser");
        dao.createAuth(auth);

        AuthData found = dao.getAuth("token123");

        assertNotNull(found, "Auth should be found after being created");
        assertEquals("testUser", found.username(), "Username should match the one used to create it");
    }

    @Test
    public void createAuthNegative() throws DataAccessException {
        AuthData auth = new AuthData("token123", "testUser");
        dao.createAuth(auth);

        assertThrows(DataAccessException.class, () -> {
            dao.createAuth(auth);
        }, "Should not allow duplicate auth tokens");
    }

    @Test
    public void getAuthNegative() throws DataAccessException {
        AuthData missing = dao.getAuth("fakeToken");
        assertNull(missing, "Should return null for non-existent token");
    }
}
