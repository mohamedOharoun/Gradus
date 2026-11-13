package es.ulpgc.pamn.dominio.backend.config;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseConfigTest {

    private static final Map<String, String> REQUIRED = Map.of(
            "GRADUS_DB_NAME", "gradus",
            "GRADUS_DB_USER", "usuario",
            "GRADUS_DB_PASSWORD", "secreta");

    @Test
    void usesLocalhostAndTheDefaultPortWhenTheyAreNotSet() {
        DatabaseConfig config = DatabaseConfig.fromEnvironment(REQUIRED);

        assertEquals("jdbc:postgresql://localhost:5432/gradus", config.jdbcUrl());
        assertEquals("usuario", config.user());
        assertEquals("secreta", config.password());
    }

    @Test
    void usesHostAndPortFromTheEnvironment() {
        Map<String, String> env = new HashMap<>(REQUIRED);
        env.put("GRADUS_DB_HOST", "db.local");
        env.put("GRADUS_DB_PORT", "55432");

        assertEquals("jdbc:postgresql://db.local:55432/gradus", DatabaseConfig.fromEnvironment(env).jdbcUrl());
    }

    @Test
    void failsFastNamingTheMissingVariable() {
        Map<String, String> env = Map.of("GRADUS_DB_NAME", "gradus", "GRADUS_DB_USER", "usuario");

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> DatabaseConfig.fromEnvironment(env));
        assertTrue(error.getMessage().contains("GRADUS_DB_PASSWORD"));
    }

    @Test
    void treatsABlankVariableAsMissing() {
        Map<String, String> env = new HashMap<>(REQUIRED);
        env.put("GRADUS_DB_USER", "  ");

        assertThrows(IllegalStateException.class, () -> DatabaseConfig.fromEnvironment(env));
    }

    @Test
    void rejectsAPortThatIsNotANumber() {
        Map<String, String> env = new HashMap<>(REQUIRED);
        env.put("GRADUS_DB_PORT", "cinco");

        assertThrows(IllegalStateException.class, () -> DatabaseConfig.fromEnvironment(env));
    }

    @Test
    void toStringNeverRevealsThePassword() {
        assertFalse(DatabaseConfig.fromEnvironment(REQUIRED).toString().contains("secreta"));
    }
}
