package com.transferwise.common.gracefulshutdown.strategies;

import com.transferwise.common.baseutils.ExceptionUtils;
import com.transferwise.common.gracefulshutdown.GracefulShutdownStrategy;
import io.grpc.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class GRPCGracefulShutdownStrategy implements GracefulShutdownStrategy {
	@Autowired
	private Server server;

	@Override
	public void applicationStarted() {
		ExceptionUtils.doUnchecked(() -> {
			log.info("Starting GRPC server");
			server.start();
			log.info("Started GRPC Server on port {}", server.getPort());
		});
	}

	@Override
	public void prepareForShutdown() {
		if (!server.isShutdown() && !server.isTerminated()) {
			log.info("Shutting Down GRPC Server");
			server.shutdown();
		}
	}

	@Override
	public boolean canShutdown() {
		return server.isTerminated();
	}

	@Override
	public void applicationTerminating() {
		if (!server.isShutdown() && !server.isTerminated()) {
			log.warn("Terminating GRPC Server ");
			server.shutdownNow();
		}
	}
}
