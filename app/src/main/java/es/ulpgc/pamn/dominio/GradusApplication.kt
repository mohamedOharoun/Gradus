package es.ulpgc.pamn.dominio

import android.app.Application
import es.ulpgc.pamn.dominio.data.AppContainer

// Se crea antes que cualquier Activity (y también si Android arranca el proceso sin Activity, como
// hará el Worker de la tarea 13). Le da el Context al composition root.
class GradusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
    }
}
