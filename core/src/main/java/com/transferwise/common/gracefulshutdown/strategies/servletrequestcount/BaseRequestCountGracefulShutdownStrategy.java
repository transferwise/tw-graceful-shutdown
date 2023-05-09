package com.transferwise.common.gracefulshutdown.strategies.servletrequestcount;

import com.transferwise.common.gracefulshutdown.GracefulShutdownStrategy;
import com.transferwise.common.gracefulshutdown.config.RequestCountStrategyProperties;
import com.transferwise.common.gracefulshutdown.strategies.servletrequestcount.servletwrappers.HttpServletRequestWrapper;
import com.transferwise.common.gracefulshutdown.strategies.servletrequestcount.servletwrappers.HttpServletResponseWrapper;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class BaseRequestCountGracefulShutdownStrategy implements GracefulShutdownStrategy {

  private static final String ALREADY_FILTERED_KEY = BaseRequestCountGracefulShutdownStrategy.class.getName() + ".FILTERED";

  private static final int SERVICE_UNAVAILABLE = 503;
  protected static final AtomicLong currentRequestsCount = new AtomicLong();

  private volatile boolean stopAcceptingRequests = false;

  private volatile boolean stopCounting;

  @Autowired
  private RequestCountStrategyProperties requestCountStrategyProperties;

  public long getCurrentRequestsCount() {
    return currentRequestsCount.get();
  }

  @Override
  public void clientReactionTimePassed() {
    stopAcceptingRequests = true;
  }

  @Override
  public boolean canShutdown() {
    stopCounting = true;
    return currentRequestsCount.get() == 0;
  }

  protected void doFilterInternal0(HttpServletRequestWrapper request, HttpServletResponseWrapper response, Callable<Void> chainCaller)
      throws Exception {

    String requestUri = request.getRequestUri();

    boolean ignoredUri = requestCountStrategyProperties.getIgnoredUris().contains(requestUri);

    if (stopAcceptingRequests) {
      if (ignoredUri) {
        log.debug("Denying a request to '{}', because we are going to shut down.", requestUri);
      } else {
        log.info("Denying a request to '{}', because we are going to shut down.", requestUri);
      }
      response.sendError(SERVICE_UNAVAILABLE);
      return;
    }

    boolean shouldCount = !stopCounting && !ignoredUri;
    if (shouldCount) {
      currentRequestsCount.incrementAndGet();
    }
    if (stopCounting && !ignoredUri) {
      log.warn("Not counting request to '" + requestUri + "' from '" + request.getRemoteAddr() + ". Contact the caller's owners to fix their code.");
    }
    try {
      chainCaller.call();
    } finally {
      if (shouldCount) {
        currentRequestsCount.decrementAndGet();
      }
    }
  }

  public void doFilterInternal(HttpServletRequestWrapper request, HttpServletResponseWrapper response, Callable<Void> chainCaller) throws Exception {
    if (request.getAttribute(ALREADY_FILTERED_KEY) != null) {
      chainCaller.call();
    }

    try {
      request.setAttribute(ALREADY_FILTERED_KEY, Boolean.TRUE);
      doFilterInternal0(request, response, chainCaller);
    } finally {
      request.removeAttribute(ALREADY_FILTERED_KEY);
    }
  }
}
