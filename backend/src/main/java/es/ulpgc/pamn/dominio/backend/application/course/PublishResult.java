package es.ulpgc.pamn.dominio.backend.application.course;

/** Desenlaces de publicar un curso nuevo (§4.3): 201, 413, 400 o 422. */
public sealed interface PublishResult permits Publication.Published, Publication.TooLarge, Publication.Unreadable,
        Publication.Rejected {
}
