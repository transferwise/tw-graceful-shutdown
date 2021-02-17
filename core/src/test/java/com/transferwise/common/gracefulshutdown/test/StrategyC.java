package com.transferwise.common.gracefulshutdown.test;

import javax.annotation.Priority;
import org.springframework.stereotype.Component;

@Component
@Priority(3)
public class StrategyC extends BaseOrderStrategy {

}