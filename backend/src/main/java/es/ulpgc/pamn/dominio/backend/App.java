package es.ulpgc.pamn.dominio.backend;

import es.ulpgc.pamn.dominio.backend.application.Authenticate;
import es.ulpgc.pamn.dominio.backend.application.LogIn;
import es.ulpgc.pamn.dominio.backend.application.LogOut;
import es.ulpgc.pamn.dominio.backend.application.PasswordHasher;
import es.ulpgc.pamn.dominio.backend.application.RegisterUser;
import es.ulpgc.pamn.dominio.backend.application.SessionRepository;
import es.ulpgc.pamn.dominio.backend.application.UserRepository;
import es.ulpgc.pamn.dominio.backend.application.course.CourseInspector;
import es.ulpgc.pamn.dominio.backend.application.course.CourseReader;
import es.ulpgc.pamn.dominio.backend.application.course.CourseRepository;
import es.ulpgc.pamn.dominio.backend.application.course.FindCourse;
import es.ulpgc.pamn.dominio.backend.application.course.SearchCourses;
import es.ulpgc.pamn.dominio.backend.application.course.PublishCourse;
import es.ulpgc.pamn.dominio.backend.application.course.PublishCourseVersion;
import es.ulpgc.pamn.dominio.backend.application.course.ValidateCourse;
import es.ulpgc.pamn.dominio.backend.application.progress.SyncProgress;
import es.ulpgc.pamn.dominio.backend.config.DatabaseConfig;
import es.ulpgc.pamn.dominio.backend.config.HttpConfig;
import es.ulpgc.pamn.dominio.backend.domain.session.SessionPolicy;
import es.ulpgc.pamn.dominio.backend.domain.course.CourseContent;
import es.ulpgc.pamn.dominio.backend.domain.course.rules.CourseValidator;
import es.ulpgc.pamn.dominio.backend.domain.session.SessionToken;
import es.ulpgc.pamn.dominio.backend.infrastructure.course.CourseSchema;
import es.ulpgc.pamn.dominio.backend.infrastructure.course.JacksonCourseReader;
import es.ulpgc.pamn.dominio.backend.infrastructure.persistence.JdbcCourseExistence;
import es.ulpgc.pamn.dominio.backend.infrastructure.persistence.JdbcCourseRepository;
import es.ulpgc.pamn.dominio.backend.infrastructure.persistence.JdbcEnrollmentRepository;
import es.ulpgc.pamn.dominio.backend.infrastructure.persistence.JdbcSessionRepository;
import es.ulpgc.pamn.dominio.backend.infrastructure.persistence.JdbcUserRepository;
import es.ulpgc.pamn.dominio.backend.infrastructure.security.Pbkdf2PasswordHasher;
import com.fasterxml.jackson.core.JacksonException;
import es.ulpgc.pamn.dominio.backend.web.ExceptionHandlers;
import es.ulpgc.pamn.dominio.backend.web.RequireSession;
import es.ulpgc.pamn.dominio.backend.web.SessionController;
import es.ulpgc.pamn.dominio.backend.web.UserController;
import es.ulpgc.pamn.dominio.backend.web.course.CourseCatalogController;
import es.ulpgc.pamn.dominio.backend.web.course.CoursePublicationController;
import es.ulpgc.pamn.dominio.backend.web.progress.ProgressController;
import es.ulpgc.pamn.dominio.backend.web.progress.ProgressRequestReader;
import io.javalin.Javalin;

import javax.sql.DataSource;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;

/** Composition root: el único sitio que conoce las implementaciones concretas y las conecta. */
public final class App {

    private App() {
    }

    public static void main(String[] args) {
        Map<String, String> env = System.getenv();
        HttpConfig http = HttpConfig.fromEnvironment(env);
        DataSource dataSource = DatabaseConfig.fromEnvironment(env).dataSource();

        // Una sola instancia de cada pieza compartida: el hasher calcula su hash señuelo al crearse.
        UserRepository userRepository = new JdbcUserRepository(dataSource);
        SessionRepository sessionRepository = new JdbcSessionRepository(dataSource);
        PasswordHasher hasher = Pbkdf2PasswordHasher.withDefaults();
        Clock clock = Clock.systemUTC();

        UserController users = new UserController(
                new RegisterUser(userRepository, hasher, clock, UUID::randomUUID));
        SessionController sessions = new SessionController(
                new LogIn(userRepository, sessionRepository, hasher, SessionPolicy.STANDARD, clock,
                        UUID::randomUUID, SessionToken::generate),
                new LogOut(sessionRepository));
        RequireSession requireSession = new RequireSession(
                new Authenticate(sessionRepository, userRepository, SessionPolicy.STANDARD, clock));

        // Cursos: el schema se compila una sola vez y el lector y el validador se comparten.
        CourseReader courseReader = new JacksonCourseReader(CourseSchema.load());
        CourseInspector courseInspector = new CourseInspector(courseReader, CourseValidator.standard());
        CourseRepository courseRepository = new JdbcCourseRepository(dataSource);
        CoursePublicationController coursePublication = new CoursePublicationController(
                new ValidateCourse(courseInspector),
                new PublishCourse(courseInspector, courseRepository, clock, UUID::randomUUID),
                new PublishCourseVersion(courseInspector, courseReader, courseRepository, clock));
        CourseCatalogController courseCatalog = new CourseCatalogController(
                new FindCourse(courseRepository), new SearchCourses(courseRepository, clock));

        // Progreso: SyncProgress se crea una sola vez (lee el reloj en cada llamada); el lector es inmutable.
        ProgressController progress = new ProgressController(new ProgressRequestReader(),
                new SyncProgress(new JdbcEnrollmentRepository(dataSource), new JdbcCourseExistence(dataSource), clock));

        Javalin.create(config -> {
            config.jetty.port = http.port();
            // 2 MiB (§4.7). Javalin trae 1 000 000 bytes por defecto: sin esto, rechazaría los cursos más grandes.
            config.http.maxRequestSize = CourseContent.MAX_SIZE_BYTES;
            config.routes.post("/api/users", users::register);
            config.routes.post("/api/sessions", sessions::logIn);
            // Rutas protegidas: sin un token válido responden 401 sin llegar al controlador.
            config.routes.get("/api/users/me", requireSession.around(users::me));
            config.routes.delete("/api/sessions/current", requireSession.around(sessions::logOut));
            config.routes.post("/api/courses/validate", requireSession.around(coursePublication::validate));
            config.routes.post("/api/courses", requireSession.around(coursePublication::publish));
            config.routes.post("/api/courses/{id}/versions", requireSession.around(coursePublication::publishVersion));
            config.routes.post("/api/progress/sync", requireSession.around(progress::sync));
            // Consulta pública: buscar y descargar cursos no necesita sesión.
            config.routes.get("/api/courses", courseCatalog::search);
            config.routes.get("/api/courses/{id}", courseCatalog::latest);
            config.routes.get("/api/courses/{id}/versions/{version}", courseCatalog::version);
            config.routes.exception(JacksonException.class, ExceptionHandlers::invalidJson);
        }).start();
    }
}
