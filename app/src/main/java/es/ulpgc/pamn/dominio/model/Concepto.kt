package es.ulpgc.pamn.dominio.model

enum class Estado { DOMINADO, FRONTERA, BLOQUEADO }

data class ConceptoUi(val id: String, val titulo: String, val estado: Estado, val progreso: Float)