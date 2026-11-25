package es.ulpgc.pamn.dominio.course.progress

import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.account.Session
import kotlin.time.Clock
import kotlin.time.Instant

// Ayudas comunes de los tests del almacén de progreso.

internal val START: Instant = Instant.parse("2026-11-25T10:00:00Z")

// Reloj falso (patrón Fake): el test decide qué hora es. La propiedad se llama time, no now,
// para no confundirla con la función now() de Clock.
internal class FakeClock(var time: Instant = START) : Clock {
    override fun now(): Instant = time
}

// Una sesión de prueba para el usuario dado. Su token es «t-<usuario>».
internal fun sessionOf(userId: String) = Session(
    token = "t-$userId",
    expiresAt = "2026-12-18T10:00:00Z",
    account = Account(userId, "Nombre $userId", "$userId@ulpgc.es", "2026-11-13T10:00:00Z"),
)

// Un almacén listo para probar: sesiones en memoria, reloj falso y el almacén que los usa.
internal class ProgressFixture {
    val sessions = InMemorySessionStore()
    val clock = FakeClock()
    val store = InMemoryProgressStore(sessions, clock)

    suspend fun logInAs(userId: String) = sessions.save(sessionOf(userId))

    // Inicia sesión como «ana», sin inscribirla en nada.
    suspend fun loggedInAsAna(): ProgressFixture {
        logInAs("ana")
        return this
    }

    // Inicia sesión como «ana» y la inscribe en los cursos dados.
    suspend fun enrolledIn(vararg courseIds: String): ProgressFixture {
        loggedInAsAna()
        courseIds.forEach { store.enroll(it) }
        return this
    }
}
