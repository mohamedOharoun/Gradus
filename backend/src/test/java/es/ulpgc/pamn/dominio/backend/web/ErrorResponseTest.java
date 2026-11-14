package es.ulpgc.pamn.dominio.backend.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.ulpgc.pamn.dominio.backend.domain.validation.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorResponseTest {

    @Test
    void everyErrorIsSentWithItsFieldCodeAndMessage() throws JsonProcessingException {
        ErrorResponse response = ErrorResponse.of("email", ErrorCode.EMAIL_TAKEN, "Ya existe.");

        assertEquals(
                "{\"errors\":[{\"field\":\"email\",\"code\":\"EMAIL_TAKEN\",\"message\":\"Ya existe.\"}]}",
                new ObjectMapper().writeValueAsString(response));
    }
}
