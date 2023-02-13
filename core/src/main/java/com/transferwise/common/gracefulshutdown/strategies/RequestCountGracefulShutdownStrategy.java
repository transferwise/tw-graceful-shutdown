package com.transferwise.common.gracefulshutdown.strategies;

import com.transferwise.common.gracefulshutdown.GracefulShutdownStrategy;
import com.transferwise.common.gracefulshutdown.config.RequestCountStrategyProperties;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
public class RequestCountGracefulShutdownStrategy extends OncePerRequestFilter implements GracefulShutdownStrategy {

  private static final int SERVICE_UNAVAILABLE = 503;
  protected static final AtomicLong requestCount = new AtomicLong();

  private volatile boolean stopAcceptingRequests = false;

  private volatile boolean stopCounting;

  @Autowired
  private RequestCountStrategyProperties requestCountStrategyProperties;

  @Override
  public void applicationTerminating() {
    stopAcceptingRequests = true;
  }

  @Override
  public boolean canShutdown() {
    stopCounting = true;
    return requestCount.get() == 0;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String requestUri = request.getRequestURI();
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
      requestCount.incrementAndGet();
    }
    if (stopCounting && !ignoredUri) {
      log.warn("Not counting request to '" + requestUri + "' from '" + request.getRemoteAddr() + ". Contact the caller's owners to fix their code.");
    }
    try {
      filterChain.doFilter(request, response);
    } finally {
      if (shouldCount) {
        requestCount.decrementAndGet();
      }
    }
  }
}
