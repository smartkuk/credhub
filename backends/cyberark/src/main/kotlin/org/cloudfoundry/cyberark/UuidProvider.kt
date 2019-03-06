package org.cloudfoundry.cyberark

import java.util.UUID

interface UuidProvider {
    fun generateUuid(): UUID
}
