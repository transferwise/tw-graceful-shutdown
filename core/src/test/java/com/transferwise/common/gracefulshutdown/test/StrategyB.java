package com.transferwise.common.gracefulshutdown.test;

import javax.annotation.Priority;
import org.springframework.stereotype.Component;

@Component
@Priority(1)
public class StrategyB extends BaseOrderStrategy {

}