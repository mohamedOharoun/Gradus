package es.ulpgc.pamn.dominio.backend.web;

import es.ulpgc.pamn.dominio.backend.application.ChangePasswordCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ChangePasswordRequestTest {

    @Test
    void becomesTheCommandOfTheUseCaseWithoutChangingTheData() {
        assertEquals(new ChangePasswordCommand(" Gradus2026! ", "Nueva2026?"),
                new ChangePasswordRequest(" Gradus2026! ", "Nueva2026?").toCommand());
    }

    @Test
    void toStringNeverRevealsThePasswords() {
        String text = new ChangePasswordRequest("Gradus2026!", "Nueva2026?").toString();

        assertFalse(text.contains("Gradus2026!"));
        assertFalse(text.contains("Nueva2026?"));
    }
}
