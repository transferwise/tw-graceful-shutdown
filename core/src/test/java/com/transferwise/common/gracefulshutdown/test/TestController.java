package com.transferwise.common.gracefulshutdown.test;

import com.transferwise.common.gracefulshutdown.strategies.servletrequestcount.BaseRequestCountGracefulShutdownStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class TestController {

  @Autowired
  BaseRequestCountGracefulShutdownStrategy baseRequestCountGracefulShutdownStrategy;

  @GetMapping(value = "/test", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public Long test() {
    return baseRequestCountGracefulShutdownStrategy.getCurrentRequestsCount();
  }
}
