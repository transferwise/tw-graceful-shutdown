package com.transferwise.common.gracefulshutdown


import com.transferwise.common.gracefulshutdown.strategies.EurekaGracefulShutdownStrategy
import com.transferwise.common.gracefulshutdown.strategies.GracefulShutdownHealthIndicator
import com.transferwise.common.gracefulshutdown.strategies.RequestCountGracefulShutdownStrategy
import com.transferwise.common.gracefulshutdown.test.TestApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.Status
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

@ActiveProfiles(["test"])
@SpringBootTest(classes = [TestApplication], properties = ["tw-graceful-shutdown.clientsReactionTimeMs=1", "tw-graceful-shutdown.shutdownTimeoutMs=1"])
class GracefulShutdownerIntSpec extends Specification {
    @Autowired
    GracefulShutdowner gracefulShutdowner

    @Autowired
    GracefulShutdownHealthIndicator healthIndicator

    def "works"() {
        expect:
            gracefulShutdowner.running

            gracefulShutdowner.strategies.find({ it.class == GracefulShutdownHealthIndicator })
            gracefulShutdowner.strategies.find({ it.class == RequestCountGracefulShutdownStrategy })
            gracefulShutdowner.strategies.find({ it.class == EurekaGracefulShutdownStrategy })

            healthIndicator.health().status == Status.UP
    }
}
