package com.transferwise.common.gracefulshutdown.strategies

import spock.lang.Specification

class RequestCountGracefulShutdownStrategySpec extends Specification {
    def "permission for shutdown is correctly given"() {
        given:
            RequestCountGracefulShutdownStrategy strategy = new RequestCountGracefulShutdownStrategy()
        when:
            strategy.requestCount.incrementAndGet()
        then:
            !strategy.canShutdown()
        when:
            strategy.requestCount.decrementAndGet()
        then:
            strategy.canShutdown()
    }
}
