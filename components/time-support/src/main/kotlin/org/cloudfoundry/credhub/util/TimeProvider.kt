package org.cloudfoundry.credhub.util

import java.time.Instant
import java.time.temporal.TemporalAccessor
import java.util.Optional

interface TimeProvider {

    fun getNow(): Optional<TemporalAccessor>

    fun getInstant(): Instant

    fun currentTimeMillis(): Long

    @Throws(InterruptedException::class)
    fun sleep(sleepTimeInMillis: Long)
}
