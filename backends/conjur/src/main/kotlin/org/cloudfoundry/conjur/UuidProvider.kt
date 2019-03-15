package org.cloudfoundry.conjur

import java.util.UUID

interface UuidProvider {
    fun generateUuid(): UUID
}
