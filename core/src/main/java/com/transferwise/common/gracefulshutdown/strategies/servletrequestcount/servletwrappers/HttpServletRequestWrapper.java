package com.transferwise.common.gracefulshutdown.strategies.servletrequestcount.servletwrappers;

// To be removed after we drop support for Spring Boot 2
public interface HttpServletRequestWrapper {

  void setAttribute(String key, Object value);

  String getRequestUri();

  String getRemoteAddr();

  Object getAttribute(String name);

  void removeAttribute(String name);
}
