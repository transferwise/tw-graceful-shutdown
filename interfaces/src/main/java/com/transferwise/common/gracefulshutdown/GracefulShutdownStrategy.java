package com.transferwise.common.gracefulshutdown;

@SuppressWarnings("unused")
public interface GracefulShutdownStrategy {

  /**
   * Called, when application is considered as started.
   */
  default void applicationStarted() {
  }

  /**
   * Return true, when application is ready to serve traffic.
   */
  default boolean isReady() {
    return true;
  }

  /**
   * Called when a signal for requesting a shutdown has received.
   */
  default void prepareForShutdown() {
  }

  /**
   * Application is not stopped, before this returns true.
   */
  boolean canShutdown();

  /**
   * Called, when decision has made to finally stop the application.
   */
  default void applicationTerminating() {
  }

  /**
   * Called right after client reaction time has passed from the start of the shutdown.
   */
  default void clientReactionTimePassed() {
  }

}
