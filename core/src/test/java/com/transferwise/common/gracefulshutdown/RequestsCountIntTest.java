package com.transferwise.common.gracefulshutdown;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.transferwise.common.gracefulshutdown.test.BaseTestEnvironment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

@BaseTestEnvironment
class RequestsCountIntTest {

  @Autowired
  private WebTestClient webTestClient;

  @Test
  void testThatRequestCountFilterIsApplied() {
    var currentRequestsCount = webTestClient.get().uri("/test").exchange()
        .expectStatus().is2xxSuccessful().returnResult(Long.class).getResponseBody().blockFirst();

    assertThat(currentRequestsCount, equalTo(1L));
  }
}
