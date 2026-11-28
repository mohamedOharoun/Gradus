package es.ulpgc.pamn.dominio.backend.infrastructure.persistence;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;

/**
 * Ayuda de test: un {@link DataSource} de mentira (hecho con {@link Proxy}, como en {@code TransactionsTest})
 * que hace fallar el test si alguien le pide una conexión. Sirve para comprobar que algo responde sin
 * tocar la base de datos.
 */
final class NoConnectionDataSource {

    private NoConnectionDataSource() {
    }

    static DataSource create() {
        return (DataSource) Proxy.newProxyInstance(NoConnectionDataSource.class.getClassLoader(),
                new Class<?>[]{DataSource.class}, (proxy, method, args) -> {
                    throw new AssertionError("No se esperaba usar la base de datos: " + method.getName());
                });
    }
}
