package es.ulpgc.pamn.dominio.backend.web.progress;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.ulpgc.pamn.dominio.backend.application.Authenticate;
import es.ulpgc.pamn.dominio.backend.application.SessionRepository;
import es.ulpgc.pamn.dominio.backend.application.UserRepository;
import es.ulpgc.pamn.dominio.backend.application.progress.EnrollmentRepository;
import es.ulpgc.pamn.dominio.backend.application.progress.SyncProgress;
import es.ulpgc.pamn.dominio.backend.application.progress.UnenrollCourse;
import es.ulpgc.pamn.dominio.backend.domain.Email;
import es.ulpgc.pamn.dominio.backend.domain.PasswordHash;
import es.ulpgc.pamn.dominio.backend.domain.TermsAcceptance;
import es.ulpgc.pamn.dominio.backend.domain.User;
import es.ulpgc.pamn.dominio.backend.domain.UserName;
import es.ulpgc.pamn.dominio.backend.domain.progress.Enrollment;
import es.ulpgc.pamn.dominio.backend.domain.progress.MasteredModule;
import es.ulpgc.pamn.dominio.backend.domain.progress.ProgressMerge;
import es.ulpgc.pamn.dominio.backend.domain.session.Session;
import es.ulpgc.pamn.dominio.backend.domain.session.SessionPolicy;
import es.ulpgc.pamn.dominio.backend.domain.session.SessionToken;
import es.ulpgc.pamn.dominio.backend.domain.session.SessionTokenHash;
import es.ulpgc.pamn.dominio.backend.web.RequireSession;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressControllerTest {

    private static final Instant NOW = Instant.parse("2026-11-28T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final User ANA = new User(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            new UserName("Ana"), new Email("ana@ulpgc.es"), new PasswordHash("fake$hash"),
            NOW, new TermsAcceptance(NOW));
    private static final User LUIS = new User(
            UUID.fromString("00000000-0000-0000-0000-000000000002"),
            new UserName("Luis"), new Email("luis@ulpgc.es"), new PasswordHash("fake$hash"),
            NOW, new TermsAcceptance(NOW));

    private static final SessionToken ANA_TOKEN = new SessionToken("token-de-ana");
    private static final UUID COURSE_A = UUID.fromString("0b8f1e2a-3c4d-4e5f-8a9b-0c1d2e3f4a5b");
    private static final UUID COURSE_B = UUID.fromString("1c9a2f3b-4d5e-4f6a-9b0c-1d2e3f4a5b6c");
    private static final UUID COURSE_UNKNOWN = UUID.fromString("99999999-9999-4999-8999-999999999999");

    private FakeEnrollmentRepository enrollments;
    private ProgressController controller;
    private Javalin app;
    private HttpClient client;

    @BeforeEach
    void setUp() {
        enrollments = new FakeEnrollmentRepository();
        FakeUserRepository users = new FakeUserRepository();
        FakeSessionRepository sessions = new FakeSessionRepository();
        users.add(ANA);
        users.add(LUIS);
        sessions.add(SessionPolicy.STANDARD.start(
                UUID.fromString("00000000-0000-0000-0000-00000000000a"), ANA.id(), ANA_TOKEN.hash(), NOW));

        RequireSession requireSession = new RequireSession(
                new Authenticate(sessions, users, SessionPolicy.STANDARD, CLOCK));
        controller = new ProgressController(
                new ProgressRequestReader(),
                new SyncProgress(enrollments, ids -> ids, CLOCK),
                new UnenrollCourse(enrollments));

        app = Javalin.create(config -> {
            config.routes.post("/api/progress/sync", requireSession.around(controller::sync));
            config.routes.delete("/api/progress/enrollments/{courseId}", requireSession.around(controller::unenroll));
        }).start(0);
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
        if (app != null) {
            app.stop();
        }
    }

    @Test
    void constructorRejectsNullDependencies() {
        ProgressRequestReader reader = new ProgressRequestReader();
        SyncProgress sync = new SyncProgress(enrollments, ids -> ids, CLOCK);
        UnenrollCourse unenroll = new UnenrollCourse(enrollments);

        assertThrows(NullPointerException.class, () -> new ProgressController(null, sync, unenroll));
        assertThrows(NullPointerException.class, () -> new ProgressController(reader, null, unenroll));
        assertThrows(NullPointerException.class, () -> new ProgressController(reader, sync, null));
    }

    @Test
    void unenrollReturns401WithoutAuthorizationHeader() throws Exception {
        enrollments.mergeAll(ANA.id(), List.of(enrollment(COURSE_A, "mod-1")));

        HttpResponse<String> response = delete("/api/progress/enrollments/" + COURSE_A, null);

        assertEquals(401, response.statusCode());
        assertEquals(Optional.of("Bearer"), response.headers().firstValue("WWW-Authenticate"));
        JsonNode errors = MAPPER.readTree(response.body()).get("errors");
        assertEquals(1, errors.size());
        assertEquals("authorization", errors.get(0).get("field").asText());
        assertEquals("UNAUTHENTICATED", errors.get(0).get("code").asText());
        assertTrue(enrollments.deleteCalls.isEmpty());
        assertEquals(1, enrollments.findAll(ANA.id()).size());
    }

    @Test
    void unenrollReturns401WithInvalidToken() throws Exception {
        enrollments.mergeAll(ANA.id(), List.of(enrollment(COURSE_A, "mod-1")));

        HttpResponse<String> response = delete("/api/progress/enrollments/" + COURSE_A, "Bearer token-invalido");

        assertEquals(401, response.statusCode());
        assertEquals(Optional.of("Bearer"), response.headers().firstValue("WWW-Authenticate"));
        JsonNode errors = MAPPER.readTree(response.body()).get("errors");
        assertEquals(1, errors.size());
        assertEquals("UNAUTHENTICATED", errors.get(0).get("code").asText());
        assertTrue(enrollments.deleteCalls.isEmpty());
        assertEquals(1, enrollments.findAll(ANA.id()).size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-uuid", "1-2-3-4-5"})
    void unenrollReturns404WhenCourseIdIsNotACanonicalUuid(String rawCourseId) throws Exception {
        enrollments.mergeAll(ANA.id(), List.of(enrollment(COURSE_A, "mod-1")));

        HttpResponse<String> response = delete("/api/progress/enrollments/" + rawCourseId, bearer(ANA_TOKEN));

        assertEquals(404, response.statusCode());
        JsonNode errors = MAPPER.readTree(response.body()).get("errors");
        assertEquals(1, errors.size());
        assertEquals("courseId", errors.get(0).get("field").asText());
        assertEquals("COURSE_NOT_FOUND", errors.get(0).get("code").asText());
        assertFalse(errors.get(0).get("message").asText().isBlank());
        assertTrue(enrollments.deleteCalls.isEmpty());
        assertEquals(1, enrollments.findAll(ANA.id()).size());
    }

    @Test
    void unenrollReturns204WithEmptyBodyAndRemovesEnrollmentAndModules() throws Exception {
        enrollments.mergeAll(ANA.id(), List.of(enrollment(COURSE_A, "mod-1", "mod-2")));

        HttpResponse<String> response = delete("/api/progress/enrollments/" + COURSE_A, bearer(ANA_TOKEN));

        assertEquals(204, response.statusCode());
        assertEquals("", response.body());
        assertTrue(enrollments.findAll(ANA.id()).isEmpty());
    }

    @Test
    void unenrollAcceptsUppercaseUuid() throws Exception {
        enrollments.mergeAll(ANA.id(), List.of(enrollment(COURSE_A, "mod-1")));

        HttpResponse<String> response = delete(
                "/api/progress/enrollments/" + COURSE_A.toString().toUpperCase(), bearer(ANA_TOKEN));

        assertEquals(204, response.statusCode());
        assertEquals("", response.body());
        assertTrue(enrollments.findAll(ANA.id()).isEmpty());
    }

    @Test
    void unenrollIsIdempotentWhenRepeatedOrWhenCourseIsNotEnrolled() throws Exception {
        enrollments.mergeAll(ANA.id(), List.of(enrollment(COURSE_A, "mod-1")));

        HttpResponse<String> first = delete("/api/progress/enrollments/" + COURSE_A, bearer(ANA_TOKEN));
        HttpResponse<String> second = delete("/api/progress/enrollments/" + COURSE_A, bearer(ANA_TOKEN));
        HttpResponse<String> unknown = delete("/api/progress/enrollments/" + COURSE_UNKNOWN, bearer(ANA_TOKEN));

        assertEquals(204, first.statusCode());
        assertEquals("", first.body());
        assertEquals(204, second.statusCode());
        assertEquals("", second.body());
        assertEquals(204, unknown.statusCode());
        assertEquals("", unknown.body());
        assertTrue(enrollments.findAll(ANA.id()).isEmpty());
    }

    @Test
    void unenrollOnlyDeletesRequestedCourseForAuthenticatedUser() throws Exception {
        Enrollment anaCourseA = enrollment(COURSE_A, "mod-a1");
        Enrollment anaCourseB = enrollment(COURSE_B, "mod-b1");
        Enrollment luisCourseA = enrollment(COURSE_A, "mod-a1");
        enrollments.mergeAll(ANA.id(), List.of(anaCourseA, anaCourseB));
        enrollments.mergeAll(LUIS.id(), List.of(luisCourseA));

        HttpResponse<String> response = delete("/api/progress/enrollments/" + COURSE_A, bearer(ANA_TOKEN));

        assertEquals(204, response.statusCode());
        assertEquals(List.of(anaCourseB), enrollments.findAll(ANA.id()));
        assertEquals(List.of(luisCourseA), enrollments.findAll(LUIS.id()));
    }

    @Test
    void syncStillResponds200WithTheSameControllerInstance() throws Exception {
        String body = """
                {
                  "enrollments": [
                    {
                      "courseId": "%s",
                      "enrolledAt": "2026-11-28T09:00:00Z",
                      "mastered": [
                        { "moduleId": "mod-1", "masteredAt": "2026-11-28T09:30:00Z" }
                      ]
                    }
                  ]
                }
                """.formatted(COURSE_A);

        HttpResponse<String> response = post("/api/progress/sync", bearer(ANA_TOKEN), body);

        assertEquals(200, response.statusCode());
        JsonNode json = MAPPER.readTree(response.body());
        assertEquals(1, json.get("enrollments").size());
        assertEquals(COURSE_A.toString(), json.get("enrollments").get(0).get("courseId").asText());
        assertEquals(1, json.get("enrollments").get(0).get("mastered").size());
        assertEquals(0, json.get("skipped").size());
    }

    private HttpResponse<String> delete(String path, String authorization) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path)).DELETE();
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String authorization, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Authorization", authorization)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + app.port() + path);
    }

    private static String bearer(SessionToken token) {
        return "Bearer " + token.value();
    }

    private static Enrollment enrollment(UUID courseId, String... moduleIds) {
        List<MasteredModule> modules = java.util.Arrays.stream(moduleIds)
                .map(id -> new MasteredModule(id, NOW))
                .toList();
        return new Enrollment(courseId, NOW, modules);
    }

    private static final class FakeEnrollmentRepository implements EnrollmentRepository {

        record DeleteCall(UUID userId, UUID courseId) {
        }

        private final Map<UUID, List<Enrollment>> byUser = new HashMap<>();
        private final List<DeleteCall> deleteCalls = new ArrayList<>();

        @Override
        public void mergeAll(UUID userId, List<Enrollment> incoming) {
            byUser.merge(userId, List.copyOf(incoming), ProgressMerge::merge);
        }

        @Override
        public List<Enrollment> findAll(UUID userId) {
            return byUser.getOrDefault(userId, List.of());
        }

        @Override
        public void delete(UUID userId, UUID courseId) {
            deleteCalls.add(new DeleteCall(userId, courseId));
            byUser.computeIfPresent(userId, (id, current) -> {
                List<Enrollment> remaining = current.stream()
                        .filter(enrollment -> !enrollment.courseId().equals(courseId))
                        .toList();
                return remaining.isEmpty() ? null : remaining;
            });
        }
    }

    private static final class FakeUserRepository implements UserRepository {

        private final Map<UUID, User> byId = new HashMap<>();

        @Override
        public boolean add(User user) {
            return byId.putIfAbsent(user.id(), user) == null;
        }

        @Override
        public Optional<User> findByEmail(Email email) {
            return byId.values().stream().filter(user -> user.email().equals(email)).findFirst();
        }

        @Override
        public Optional<User> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public void update(User user) {
            byId.replace(user.id(), user);
        }

        @Override
        public boolean removeUnlessAuthor(UUID id) {
            return byId.remove(id) != null;
        }
    }

    private static final class FakeSessionRepository implements SessionRepository {

        private final Map<SessionTokenHash, Session> byHash = new HashMap<>();

        @Override
        public void add(Session session) {
            byHash.put(session.tokenHash(), session);
        }

        @Override
        public Optional<Session> findByTokenHash(SessionTokenHash tokenHash) {
            return Optional.ofNullable(byHash.get(tokenHash));
        }

        @Override
        public void update(Session session) {
            byHash.replace(session.tokenHash(), session);
        }

        @Override
        public void remove(SessionTokenHash tokenHash) {
            byHash.remove(tokenHash);
        }

        @Override
        public void removeOthersOf(UUID userId, SessionTokenHash keep) {
            byHash.values().removeIf(session -> session.userId().equals(userId) && !session.tokenHash().equals(keep));
        }
    }
}
