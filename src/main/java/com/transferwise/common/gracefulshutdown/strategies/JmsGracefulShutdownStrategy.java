package com.transferwise.common.gracefulshutdown.strategies;

import com.transferwise.common.gracefulshutdown.GracefulShutdownStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jms.config.JmsListenerEndpointRegistry;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class JmsGracefulShutdownStrategy implements GracefulShutdownStrategy {
    private AtomicInteger runningContainersCount = new AtomicInteger();

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void prepareForShutdown() {
        runningContainersCount = new AtomicInteger();

        JmsListenerEndpointRegistry jmsListenerEndpointRegistry;
        try {
            jmsListenerEndpointRegistry = applicationContext.getBean(JmsListenerEndpointRegistry.class);
        } catch (BeansException e) {
            log.info("JmsListenerEndpointRegistry not found in application context. Skipping JMS graceful shutdown.");
            return;
        }

        runningContainersCount.set(jmsListenerEndpointRegistry.getListenerContainers().size());
        log.info("Gracefully stopping {} JMS MessageListenerContainers.", runningContainersCount.get());
        jmsListenerEndpointRegistry.getListenerContainers().forEach((container) -> container.stop(() -> runningContainersCount.decrementAndGet()));
    }

    @Override
    public boolean canShutdown() {
        int cnt = runningContainersCount.get();
        if (cnt > 0) {
            log.info("Can't shut down, {} JMS listeners are running.", cnt);
            return false;
        }
        log.info("All JMS listeners have finished.");
        return true;
    }
}
