package es.ulpgc.pamn.dominio.backend.web;

import es.ulpgc.pamn.dominio.backend.application.DeleteAccountCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DeleteAccountRequestTest {

    @Test
    void becomesTheCommandOfTheUseCaseWithoutChangingTheData() {
        assertEquals(new DeleteAccountCommand(" Gradus2026! "), new DeleteAccountRequest(" Gradus2026! ").toCommand());
    }

    @Test
    void toStringNeverRevealsThePassword() {
        assertFalse(new DeleteAccountRequest("Gradus2026!").toString().contains("Gradus2026!"));
    }
}
