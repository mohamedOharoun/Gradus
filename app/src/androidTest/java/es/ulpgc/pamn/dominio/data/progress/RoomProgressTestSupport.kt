package es.ulpgc.pamn.dominio.data.progress

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.InMemorySessionStore
import es.ulpgc.pamn.dominio.account.Session
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.rules.ExternalResource
import kotlin.time.Clock
import kotlin.time.Instant

// Ayudas comunes de los tests de RoomProgressStore (7b).

internal const val ANA = "ana"
internal const val BETO = "beto"
internal const val GRAPHS = "graphs"
internal const val TREES = "trees"
internal val START: Instant = Instant.parse("2026-11-25T10:00:00Z")

// Reloj de prueba (patrón Fake): el test decide qué hora es.
class TestClock(var time: Instant = START) : Clock {
    override fun now(): Instant = time
}

// Una sesión de prueba para el usuario dado. Su token es «t-<usuario>».
internal fun sessionOf(userId: String) = Session(
    token = "t-$userId",
    expiresAt = "2026-12-18T10:00:00Z",
    account = Account(userId, "Nombre $userId", "$userId@ulpgc.es", "2026-11-13T10:00:00Z"),
)

// Regla de JUnit (TestRule): antes de cada test abre una base en memoria y crea el almacén con
// sesiones en memoria y el reloj de prueba; después de cada test cierra la base. Es pública porque
// JUnit la recibe de una propiedad pública (@get:Rule).
class RoomProgressRule : ExternalResource() {
    val sessions = InMemorySessionStore()
    val clock = TestClock()
    lateinit var database: ProgressDatabase
        private set
    lateinit var store: RoomProgressStore
        private set
    val dao: ProgressDao get() = database.progressDao()

    override fun before() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ProgressDatabase::class.java,
        ).build()
        store = RoomProgressStore(database.progressDao(), sessions, clock)
    }

    override fun after() = database.close()

    suspend fun logInAs(userId: String) = sessions.save(sessionOf(userId))

    // Inicia sesión como «ana» y la inscribe en los cursos dados.
    suspend fun enrolledAsAna(vararg courseIds: String) {
        logInAs(ANA)
        courseIds.forEach { store.enroll(it) }
    }

    // Cuántas filas tiene la tabla (de todos los usuarios y cursos).
    fun rowCount(table: String): Int =
        database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use {
            it.moveToFirst()
            it.getInt(0)
        }
}

// Escucha el Flow en segundo plano y deja cada valor en un Channel. Room emite desde su propio
// hilo: receive() espera a que llegue el siguiente valor, sin delay ni sleep. backgroundScope se
// cancela solo al terminar el test.
internal fun <T> TestScope.listenTo(flow: Flow<T>): ReceiveChannel<T> {
    val seen = Channel<T>(Channel.UNLIMITED)
    backgroundScope.launch { flow.collect { seen.send(it) } }
    return seen
}

// La excepción que lanza el bloque, o null si no lanza.
internal suspend fun failureOf(block: suspend () -> Unit): Throwable? =
    runCatching { block() }.exceptionOrNull()

// Exactamente la IllegalStateException del puerto: ni una subclase ni la SQLiteConstraintException
// de la clave foránea.
internal fun assertIsThePortFailure(failure: Throwable?) =
    assertEquals("llegó $failure", IllegalStateException::class.java, failure?.javaClass)
