package com.transferwise.common.gracefulshutdown.test;

import javax.annotation.Priority;
import org.springframework.stereotype.Component;

@Component
@Priority(2)
@jakarta.annotation.Priority(2)
public class StrategyA extends BaseOrderStrategy {

}
