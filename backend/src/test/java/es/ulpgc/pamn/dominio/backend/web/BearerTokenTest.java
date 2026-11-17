package es.ulpgc.pamn.dominio.backend.web;

import es.ulpgc.pamn.dominio.backend.domain.session.SessionToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BearerTokenTest {

    @Test
    void readsTheTokenOfAnAuthorizationHeader() {
        assertEquals(Optional.of(new SessionToken("nAeCoBRT-BFCfCPGmC-h8HPCB5RnYxr5uR7VcMloq0k")),
                BearerToken.from("Bearer nAeCoBRT-BFCfCPGmC-h8HPCB5RnYxr5uR7VcMloq0k"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"bearer abc", "BEARER abc", "Bearer   abc", "Bearer abc "})
    void theSchemeIsCaseInsensitiveAndExtraSpacesAreIgnored(String header) {
        // RFC 7235: el nombre del esquema no distingue mayúsculas.
        assertEquals(Optional.of(new SessionToken("abc")), BearerToken.from(header));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "Bearer",
            "Bearer ",
            "abc",                     // sin esquema
            "Basic dXNlcjpwYXNz",      // otro esquema
            "Bearer abc def",          // dos tokens
            "Bearer <script>"          // caracteres que no puede tener un token
    })
    void ignoresAnythingThatIsNotABearerToken(String header) {
        assertEquals(Optional.empty(), BearerToken.from(header));
    }
}
