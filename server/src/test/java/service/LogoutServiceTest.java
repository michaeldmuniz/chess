package service;

import dataaccess.MemoryDataAccess;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LogoutServiceTest {

    private MemoryDataAccess dao;
    private RegisterService registerService;
    private LoginService loginService;
    private LogoutService logoutService;

    private String validAuthToken;

    @BeforeEach
    void setup() throws Exception {
        dao = new MemoryDataAccess();
        registerService = new RegisterService(dao);
        loginService = new LoginService(dao);
        logoutService = new LogoutService(dao);

        var user = new UserData("logoutUser", "password", "email@example.com");
        registerService.register(user);

        var loginResult = loginService.login("logoutUser", "password");
        validAuthToken = loginResult.authToken();
    }

    @Test
    void positiveLogout() throws Exception {
        assertDoesNotThrow(() -> logoutService.logout(validAuthToken));

        assertNull(dao.getAuth(validAuthToken));
    }

    @Test
    void negativeLogout_invalidToken() {
        assertThrows(Exception.class, () -> logoutService.logout("invalid_token"));
    }
}
