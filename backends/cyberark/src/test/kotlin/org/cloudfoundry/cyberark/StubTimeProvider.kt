package org.cloudfoundry.cyberark

import org.cloudfoundry.credhub.util.TimeProvider
import java.time.Instant
import java.time.temporal.TemporalAccessor
import java.util.Optional

class StubTimeProvider : TimeProvider {

    override fun getNow(): Optional<TemporalAccessor> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    lateinit var getInstantReturn: Instant
    override fun getInstant(): Instant {
        return getInstantReturn
    }

    override fun currentTimeMillis(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun sleep(sleepTimeInMillis: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
