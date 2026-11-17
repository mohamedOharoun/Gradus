package es.ulpgc.pamn.dominio.backend.web;

import es.ulpgc.pamn.dominio.backend.application.LogInCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LogInRequestTest {

    @Test
    void becomesTheCommandOfTheUseCaseWithoutChangingTheData() {
        assertEquals(new LogInCommand(" Ana@ulpgc.es ", "Gradus2026!"),
                new LogInRequest(" Ana@ulpgc.es ", "Gradus2026!").toCommand());
    }

    @Test
    void toStringNeverRevealsThePassword() {
        assertFalse(new LogInRequest("ana@ulpgc.es", "Gradus2026!").toString().contains("Gradus2026!"));
    }
}
