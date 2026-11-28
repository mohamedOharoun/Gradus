package es.ulpgc.pamn.dominio.backend.application.course;

/** Desenlaces de publicar una versión nueva (§4.3): los de un curso nuevo, más 404, 403 y 409. */
public sealed interface PublishVersionResult permits Publication.Published, Publication.TooLarge,
        Publication.Unreadable, Publication.Rejected, Publication.CourseNotFound, Publication.NotAuthor,
        Publication.IdenticalToLatest {
}
