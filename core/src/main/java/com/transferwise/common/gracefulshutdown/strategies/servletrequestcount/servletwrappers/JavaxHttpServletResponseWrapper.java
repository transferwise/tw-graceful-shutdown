package com.transferwise.common.gracefulshutdown.strategies.servletrequestcount.servletwrappers;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

// To be removed after we drop support for Spring Boot 2
public class JavaxHttpServletResponseWrapper implements HttpServletResponseWrapper {

  private final HttpServletResponse delegate;

  public JavaxHttpServletResponseWrapper(HttpServletResponse delegate) {
    this.delegate = delegate;
  }

  @Override
  public void sendError(int sc) throws IOException {
    delegate.sendError(sc);
  }
}
