package com.transferwise.common.gracefulshutdown;

import com.transferwise.common.gracefulshutdown.strategies.ExecutorServiceGracefulShutdownStrategy;
import com.transferwise.common.gracefulshutdown.strategies.TaskSchedulersGracefulShutdownStrategy;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotated Beans will not be shutdown gracefully by service.
 * <p>
 * Affects: {@link ExecutorServiceGracefulShutdownStrategy} and {@link TaskSchedulersGracefulShutdownStrategy}
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface GracefulShutdownIgnore {

}
