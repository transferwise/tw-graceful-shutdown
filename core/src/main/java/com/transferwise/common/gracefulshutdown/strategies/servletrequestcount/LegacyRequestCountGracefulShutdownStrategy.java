package com.transferwise.common.gracefulshutdown.strategies.servletrequestcount;

import com.transferwise.common.baseutils.ExceptionUtils;
import com.transferwise.common.gracefulshutdown.strategies.servletrequestcount.servletwrappers.JavaxHttpServletRequestWrapper;
import com.transferwise.common.gracefulshutdown.strategies.servletrequestcount.servletwrappers.JavaxHttpServletResponseWrapper;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

// To be removed after we drop support for Spring Boot 2
@Slf4j
public class LegacyRequestCountGracefulShutdownStrategy extends BaseRequestCountGracefulShutdownStrategy implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    try {
      doFilterInternal(new JavaxHttpServletRequestWrapper((HttpServletRequest) request),
          new JavaxHttpServletResponseWrapper((HttpServletResponse) response), () -> {
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
