package com.transferwise.common.gracefulshutdown.strategies;

import com.transferwise.boot.gracefulshutdown.GracefulShutdownStrategy;
import com.transferwise.common.baseutils.ExceptionUtils;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class QuartzGracefulShutdownStrategy implements GracefulShutdownStrategy {
    @Autowired
    private Scheduler scheduler;

    @Override
    public void prepareForShutdown() {
        ExceptionUtils.doUnchecked(() -> {
            if (!scheduler.isInStandbyMode()) {
                log.info("Quartz scheduler was not in standby, setting to standby.");
                scheduler.standby();
            } else {
                log.info("Quartz scheduler is already on standby.");
            }
        });
    }

    @Override
    public boolean canShutdown() {
        return ExceptionUtils.doUnchecked(() -> {
            boolean hasRunningJobs = !scheduler.getCurrentlyExecutingJobs().isEmpty();
            if (hasRunningJobs) {
                log.info("Can't shut down, jobs are running: " + scheduler.getCurrentlyExecutingJobs() + ".");
                return false;
            }
            log.info("No running jobs, can shut down.");
            return true;
        });
    }
}
