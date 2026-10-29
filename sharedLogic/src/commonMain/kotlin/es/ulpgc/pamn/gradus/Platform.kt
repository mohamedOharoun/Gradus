package es.ulpgc.pamn.gradus

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform