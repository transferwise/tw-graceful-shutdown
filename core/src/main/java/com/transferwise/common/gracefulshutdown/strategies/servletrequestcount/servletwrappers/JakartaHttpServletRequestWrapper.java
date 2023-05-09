package com.transferwise.common.gracefulshutdown.strategies.servletrequestcount.servletwrappers;

import jakarta.servlet.http.HttpServletRequest;

// To be removed after we drop support for Spring Boot 2
public class JakartaHttpServletRequestWrapper implements HttpServletRequestWrapper {

  private final HttpServletRequest delegate;

  public JakartaHttpServletRequestWrapper(HttpServletRequest delegate) {
    this.delegate = delegate;
  }

  @Override
  public void setAttribute(String key, Object value) {
    delegate.setAttribute(key, value);
  }

  @Override
  public String getRequestUri() {
    return delegate.getRequestURI();
  }

  @Override
  public String getRemoteAddr() {
    return delegate.getRemoteAddr();
  }

  @Override
  public Object getAttribute(String name) {
    return delegate.getAttribute(name);
  }

  @Override
  public void removeAttribute(String name) {
    delegate.removeAttribute(name);
  }
}
