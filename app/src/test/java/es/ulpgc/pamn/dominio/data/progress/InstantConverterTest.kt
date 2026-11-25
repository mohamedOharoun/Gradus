package es.ulpgc.pamn.dominio.data.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

// Cómo se guardan las fechas en Room: nanosegundos desde 1970 en un Long. La ida y vuelta es exacta,
// el orden se conserva (MIN en SQL da la más antigua) y fuera del rango de un Long se lanza.
class InstantConverterTest {

    private val converter = InstantConverter()

    private fun roundTrip(instant: Instant) = converter.fromEpochNanos(converter.toEpochNanos(instant))

    @Test
    fun the_round_trip_returns_the_same_instant_with_its_nanoseconds() {
        val instant = Instant.parse("2026-11-25T10:15:30.123456789Z")

        assertEquals(instant, roundTrip(instant))
    }

    @Test
    fun the_epoch_is_zero() {
        assertEquals(0L, converter.toEpochNanos(Instant.parse("1970-01-01T00:00:00Z")))
        assertEquals(Instant.parse("1970-01-01T00:00:00Z"), converter.fromEpochNanos(0L))
    }

    @Test
    fun an_earlier_instant_gives_a_smaller_number_even_by_one_nanosecond() {
        val earlier = Instant.parse("2026-11-25T10:15:30.123456789Z")
        val later = Instant.parse("2026-11-25T10:15:30.123456790Z")

        assertTrue(converter.toEpochNanos(earlier) < converter.toEpochNanos(later))
    }

    @Test
    fun the_order_is_kept_across_the_epoch() {
        val before = Instant.parse("1969-12-31T23:59:59.999999999Z")
        val epoch = Instant.parse("1970-01-01T00:00:00Z")
        val after = Instant.parse("1970-01-01T00:00:00.000000001Z")

        assertEquals(-1L, converter.toEpochNanos(before))
        assertTrue(converter.toEpochNanos(before) < converter.toEpochNanos(epoch))
        assertTrue(converter.toEpochNanos(epoch) < converter.toEpochNanos(after))
    }

    @Test
    fun an_instant_before_1970_with_nanoseconds_comes_back_exact() {
        val instant = Instant.parse("1903-12-17T10:35:00.000000123Z")

        assertEquals(instant, roundTrip(instant))
    }

    @Test
    fun a_whole_second_before_1970_comes_back_exact() {
        val instant = Instant.parse("1969-12-31T23:59:59Z")

        assertEquals(-1_000_000_000L, converter.toEpochNanos(instant))
        assertEquals(instant, roundTrip(instant))
    }

    @Test
    fun the_earliest_instant_is_long_min_value_and_comes_back() {
        assertEquals(Long.MIN_VALUE, converter.toEpochNanos(EARLIEST))
        assertEquals(EARLIEST, converter.fromEpochNanos(Long.MIN_VALUE))
    }

    @Test
    fun the_latest_instant_is_long_max_value_and_comes_back() {
        assertEquals(Long.MAX_VALUE, converter.toEpochNanos(LATEST))
        assertEquals(LATEST, converter.fromEpochNanos(Long.MAX_VALUE))
    }

    @Test
    fun one_nanosecond_before_the_earliest_instant_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            converter.toEpochNanos(Instant.parse("1677-09-21T00:12:43.145224191Z"))
        }
    }

    @Test
    fun one_nanosecond_after_the_latest_instant_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            converter.toEpochNanos(Instant.parse("2262-04-11T23:47:16.854775808Z"))
        }
    }

    @Test
    fun instants_far_outside_the_range_throw() {
        assertThrows(IllegalArgumentException::class.java) { converter.toEpochNanos(Instant.DISTANT_PAST) }
        assertThrows(IllegalArgumentException::class.java) { converter.toEpochNanos(Instant.DISTANT_FUTURE) }
    }

    private companion object {
        val EARLIEST = Instant.parse("1677-09-21T00:12:43.145224192Z")
        val LATEST = Instant.parse("2262-04-11T23:47:16.854775807Z")
    }
}
