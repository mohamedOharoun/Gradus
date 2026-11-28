package es.ulpgc.pamn.dominio.data.progress

import androidx.room.TypeConverter
import kotlin.time.Instant

// Room guarda cada Instant como nanosegundos desde 1970 en un Long (columna INTEGER).
// - La ida y vuelta es exacta, con los nanosegundos.
// - Conserva el orden: un Instant anterior da un Long menor, así que MIN en SQL da la fecha más antigua.
// - Un Long solo cabe entre 1677-09-21T00:12:43.145224192Z (Long.MIN_VALUE) y
//   2262-04-11T23:47:16.854775807Z (Long.MAX_VALUE). Fuera de ese rango lanza IllegalArgumentException
//   en vez de guardar un número desbordado.
class InstantConverter {

    @TypeConverter
    fun toEpochNanos(instant: Instant): Long {
        val seconds = instant.epochSeconds
        val nanos = instant.nanosecondsOfSecond.toLong()
        return try {
            // Con segundos negativos y nanos > 0 se toma un segundo prestado: así el producto no se sale
            // del Long cerca del mínimo aunque el resultado final sí quepa.
            if (seconds < 0 && nanos > 0) {
                Math.addExact(Math.multiplyExact(seconds + 1, NANOS_PER_SECOND), nanos - NANOS_PER_SECOND)
            } else {
                Math.addExact(Math.multiplyExact(seconds, NANOS_PER_SECOND), nanos)
            }
        } catch (overflow: ArithmeticException) {
            throw IllegalArgumentException("$instant no cabe en nanosegundos desde 1970 (1677–2262)", overflow)
        }
    }

    // floorDiv y floorMod (no / y %) para que antes de 1970 los nanos queden entre 0 y 999 999 999.
    @TypeConverter
    fun fromEpochNanos(nanos: Long): Instant =
        Instant.fromEpochSeconds(
            Math.floorDiv(nanos, NANOS_PER_SECOND),
            Math.floorMod(nanos, NANOS_PER_SECOND),
        )

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000L
    }
}
