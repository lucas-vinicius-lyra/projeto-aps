package br.ufpb.jwpersonal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import org.junit.jupiter.api.Test;

class SmokeTest {

    @Test
    void junitFunciona() {
        assertEquals(4, 2 + 2);
    }

    @Test
    void driverSqliteConectaEmMemoria() {
        assertDoesNotThrow(() -> {
            try (Connection c = DriverManager.getConnection("jdbc:sqlite::memory:")) {
                c.createStatement().execute("SELECT 1");
            }
        });
    }
}
