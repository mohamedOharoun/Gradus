package es.ulpgc.pamn.dominio.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

// En los tests instrumentados GradusApplication ya ha llamado a AppContainer.init.
// Esto crea el RoomDatabase real, pero Room no abre el fichero hasta la primera consulta
// (build() solo configura), así que el test no toca ningún dato.
@RunWith(AndroidJUnit4::class)
class AppContainerTest {

    // Lo que la arquitectura pide conservar al mergear: si fueran dos objetos, inscribirse no
    // abriría nada.
    @Test
    fun progress_store_and_enrollment_store_are_the_same_object() {
        assertSame(AppContainer.progressStore, AppContainer.enrollmentStore)
    }
}
