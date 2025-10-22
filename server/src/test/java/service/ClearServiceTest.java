package service;

import dataaccess.MemoryDataAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class ClearServiceTest {

    private MemoryDataAccess dao;
    private ClearService service;

    @BeforeEach
    void setup() {
        dao = new MemoryDataAccess();
        service = new ClearService(dao);
    }

    @Test
    void positiveClear() {
        assertDoesNotThrow(() -> service.clear());
    }
}
