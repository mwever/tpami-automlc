package ai.libs.hyperopt.impl;

import java.io.IOException;

import org.api4.java.common.attributedobjects.IObjectEvaluator;

import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.pcsbasedoptimization.proto.PCSBasedOptimizerService;
import ai.libs.hyperopt.api.IOptimizationTask;
import ai.libs.hyperopt.impl.pcs.IPCSBasedOptimizerConfig;
import io.grpc.Server;
import io.grpc.ServerBuilder;

/**
 * For starting a gRPC server with the implementation of
 * {@link PCSBasedOptimizerService}
 *
 * @author kadirayk
 *
 */
public class PCSBasedOptimizerGrpcServer<M> {

	private static final IPCSBasedOptimizerConfig CONFIG = IPCSBasedOptimizerConfig.get(GlobalConfig.OPTIMIZER_CONFIG_PATH);

	/**
	 * Starts the server on given port
	 *
	 * @param evaluator an implementation of {@link IObjectEvaluator} with
	 *                  {@link ComponentInstance} and Double
	 * @param input     {@link PCSBasedOptimizerInput}
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static <M> void start(final IOptimizationTask<M> input) throws IOException, InterruptedException {

		Integer port = CONFIG.getPort();
		Server server = ServerBuilder.forPort(port).addService(new PCSBasedOptimizerServiceImpl<M>(input)).build();

		server.start();
		server.awaitTermination();

	}

}
