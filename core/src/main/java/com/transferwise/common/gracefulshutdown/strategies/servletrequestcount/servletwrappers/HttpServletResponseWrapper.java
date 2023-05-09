package com.transferwise.common.gracefulshutdown.strategies.servletrequestcount.servletwrappers;

import java.io.IOException;

// To be removed after we drop support for Spring Boot 2
public interface HttpServletResponseWrapper {

  void sendError(int sc) throws IOException;
}
