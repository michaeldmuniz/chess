package service;

import dataaccess.MemoryDataAccess;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LoginServiceTest {

    private MemoryDataAccess dao;
    private RegisterService registerService;
    private LoginService loginService;

    @BeforeEach
    void setup() throws Exception {
        dao = new MemoryDataAccess();
        registerService = new RegisterService(dao);
        loginService = new LoginService(dao);

        var user = new UserData("testUser", "password", "email@example.com");
        registerService.register(user);
    }

    @Test
    void positiveLogin() throws Exception {
        var result = loginService.login("testUser", "password");

        assertEquals("testUser", result.username());
        assertNotNull(result.authToken());
    }

    @Test
    void negativeLoginWrongPassword() {
        assertThrows(Exception.class, () -> loginService.login("testUser", "wrongPassword"));
    }
}
