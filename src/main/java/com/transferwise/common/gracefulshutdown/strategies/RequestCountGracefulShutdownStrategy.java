package com.transferwise.common.gracefulshutdown.strategies;

import com.transferwise.common.gracefulshutdown.GracefulShutdownStrategy;
import com.transferwise.common.gracefulshutdown.config.RequestCountStrategyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class RequestCountGracefulShutdownStrategy extends OncePerRequestFilter implements GracefulShutdownStrategy {
    private static final int SERVICE_UNAVAILABLE = 503;
    protected static final AtomicLong requestCount = new AtomicLong();

    private boolean stopAcceptingRequests = false;

    private boolean stopCounting;

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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        if (stopAcceptingRequests) {
            log.info("Denying a request to '" + requestURI + "', because we are going to shut down.");
            response.sendError(SERVICE_UNAVAILABLE);
            return;
        }

        boolean shouldCount = !stopCounting && !requestCountStrategyProperties.getIgnoredUris().contains(requestURI);
        if (shouldCount) {
            requestCount.incrementAndGet();
        } else {
            if (!requestURI.contains("health")) {
                log.warn("Not counting request to '" + requestURI + "' from '" + request.getRemoteAddr() + ". Contact the caller's owners to fix their code.");
            }
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
