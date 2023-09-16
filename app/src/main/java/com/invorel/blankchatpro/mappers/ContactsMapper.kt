package com.invorel.blankchatpro.mappers

import com.invorel.blankchatpro.local.tables.Contacts
import com.invorel.blankchatpro.state.Contact

fun Contacts.toPojo() =
  Contact(
    id = id,
    name = name,
    number = number,
    photo = photo,
  )

fun Iterable<Contacts>.toPojos() = this.map { it.toPojo() }

fun Contact.toModel() =
  Contacts(
    id = id,
    name = name,
    number = number,
    photo = photo,
  )

fun Iterable<Contact>.toModels() = this.map { it.toModel() }
