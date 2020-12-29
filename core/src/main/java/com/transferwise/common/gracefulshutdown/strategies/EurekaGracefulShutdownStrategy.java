package com.transferwise.common.gracefulshutdown.strategies;

import com.transferwise.common.baseutils.ExceptionUtils;
import com.transferwise.common.gracefulshutdown.GracefulShutdownStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;

@Slf4j
public class EurekaGracefulShutdownStrategy implements GracefulShutdownStrategy {
    @Autowired(required = false)
    private EurekaRegistration eureka;

    @Override
    public boolean canShutdown() {
        return true;
    }

    @Override
    public void prepareForShutdown() {
        ExceptionUtils.doUnchecked(() -> {
            if (eureka != null) {
                eureka.getEurekaClient().shutdown();
            }
        });
    }
}
