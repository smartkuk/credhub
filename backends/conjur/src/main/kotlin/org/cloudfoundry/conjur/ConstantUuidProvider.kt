package org.cloudfoundry.conjur

import java.util.UUID

class ConstantUuidProvider : UuidProvider {
    override fun generateUuid(): UUID {
        return UUID.fromString("00000000-0000-0000-0000-000000000000")
    }
}
