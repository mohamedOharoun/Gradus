package es.ulpgc.pamn.dominio.submission

// Una entrega con foto ya guardada en el servidor (docs/tareas/ejercicio-foto.md). Solo lo que la app
// usa: su id y cuándo se hizo («2026-11-25T10:00:00.123456Z», como llega). Queda pendiente de revisión.
data class Submission(val id: String, val submittedAt: String)
