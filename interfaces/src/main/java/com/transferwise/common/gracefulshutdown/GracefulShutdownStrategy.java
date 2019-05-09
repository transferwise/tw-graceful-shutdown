package com.transferwise.common.gracefulshutdown;

public interface GracefulShutdownStrategy {
    default void applicationStarted() {
    }

    default void prepareForShutdown() {
    }

    boolean canShutdown();

    default void applicationTerminating() {
    }
}
