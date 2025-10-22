package service;

import dataaccess.MemoryDataAccess;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RegisterServiceTest {

    private MemoryDataAccess dao;
    private RegisterService service;

    @BeforeEach
    void setup() {
        dao = new MemoryDataAccess();
        service = new RegisterService(dao);
    }

    @Test
    void positiveRegister() throws Exception {
        var request = new UserData("user1", "password", "email@example.com");
        var result = service.register(request);

        assertEquals("user1", result.username());
        assertNotNull(result.authToken());
    }

    @Test
    void negativeRegister_duplicateUser() throws Exception {
        var request = new UserData("dupeUser", "password", "email@example.com");
        service.register(request);

        // Second registration with same username should fail
        assertThrows(Exception.class, () -> service.register(request));
    }
}
