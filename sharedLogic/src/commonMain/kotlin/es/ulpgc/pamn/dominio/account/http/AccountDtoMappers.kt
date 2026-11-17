package es.ulpgc.pamn.dominio.account.http

import es.ulpgc.pamn.dominio.account.Account
import es.ulpgc.pamn.dominio.account.NewAccount
import es.ulpgc.pamn.dominio.account.ServerFieldError

// Mapper: traduce entre los tipos de la app (account/) y los DTO de la red. Funciones puras.

internal fun NewAccount.toDto() = RegisterUserRequestDto(
    name = name,
    email = email,
    password = password,
    acceptedTerms = acceptedTerms,
)

internal fun UserResponseDto.toAccount() = Account(id = id, name = name, email = email, createdAt = createdAt)

internal fun ErrorResponseDto.toServerFieldErrors() = errors.map { ServerFieldError(field = it.field, code = it.code) }
