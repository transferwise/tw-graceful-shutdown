package com.transferwise.common.gracefulshutdown.strategies.servletrequestcount.servletwrappers;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

// To be removed after we drop support for Spring Boot 2
public class JakartaHttpServletResponseWrapper implements HttpServletResponseWrapper {

  private final HttpServletResponse delegate;

  public JakartaHttpServletResponseWrapper(HttpServletResponse delegate) {
    this.delegate = delegate;
  }

  @Override
  public void sendError(int sc) throws IOException {
    delegate.sendError(sc);
  }
}
