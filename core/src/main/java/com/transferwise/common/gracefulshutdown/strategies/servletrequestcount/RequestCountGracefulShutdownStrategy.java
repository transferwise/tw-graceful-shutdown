package com.transferwise.common.gracefulshutdown.strategies.servletrequestcount;

import com.transferwise.common.baseutils.ExceptionUtils;
import com.transferwise.common.gracefulshutdown.strategies.servletrequestcount.servletwrappers.JakartaHttpServletRequestWrapper;
import com.transferwise.common.gracefulshutdown.strategies.servletrequestcount.servletwrappers.JakartaHttpServletResponseWrapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RequestCountGracefulShutdownStrategy extends BaseRequestCountGracefulShutdownStrategy implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    try {
      doFilterInternal(new JakartaHttpServletRequestWrapper((HttpServletRequest) request),
          new JakartaHttpServletResponseWrapper((HttpServletResponse) response), () -> {
            chain.doFilter(request, response);
            return null;
          }
      );
    } catch (ServletException | IOException e) {
      throw e;
    } catch (Exception e) {
      throw ExceptionUtils.toUnchecked(e);
    }
  }
}
