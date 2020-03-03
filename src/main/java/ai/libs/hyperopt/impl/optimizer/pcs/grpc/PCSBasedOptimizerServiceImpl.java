package ai.libs.hyperopt.impl.optimizer.pcs.grpc;

import java.util.HashMap;
import java.util.Map;

import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.pcsbasedoptimization.proto.PCSBasedComponentProto;
import ai.libs.hasco.pcsbasedoptimization.proto.PCSBasedEvaluationResponseProto;
import ai.libs.hasco.pcsbasedoptimization.proto.PCSBasedOptimizerServiceGrpc.PCSBasedOptimizerServiceImplBase;
import ai.libs.hyperopt.api.input.IOptimizationTask;
import ai.libs.hyperopt.impl.optimizer.pcs.HASCOToPCSConverter;
import io.grpc.stub.StreamObserver;

/**
 * gRPC service implementation for PCSBasedOptimizers
 *
 * @author kadirayk
 *
 */
public class PCSBasedOptimizerServiceImpl<M> extends PCSBasedOptimizerServiceImplBase {

	private final IOptimizationTask<M> input;
	private final HASCOToPCSConverter searchspaceConverter;
	
	public PCSBasedOptimizerServiceImpl(final IOptimizationTask<M> input, final HASCOToPCSConverter searchspaceConverter) {
		this.input = input;
		this.searchspaceConverter = searchspaceConverter;
	}

	/**
	 * Optimizer scripts call this method with a component name and a list of
	 * parameters for that component. The corresponding component will be
	 * instantiated with the parameters as a componentInstance.
	 *
	 * ComponentInstance will be evaluated with the given evaluator. A response
	 * containing an evalutation score will return to the caller script
	 */
	@Override
	public void evaluate(final PCSBasedComponentProto request, final StreamObserver<PCSBasedEvaluationResponseProto> responseObserver) {
		System.out.println("Evaluate!");
		// copy all parameters and their values into a map and reconstruct the component instance represented by this
		Map<String, String> parameterMap = new HashMap<>();
		request.getParametersList().stream().forEach(x -> parameterMap.put(x.getKey(), x.getValue()));
		System.out.println("Build component instance from map: " + parameterMap);
		ComponentInstance componentInstance = this.searchspaceConverter.getComponentInstanceFromMap(parameterMap);

		// Evaluate the score of the component instance.
		Double score = 0.0;
//		try {
//			System.out.println(componentInstance);
//			score = this.input.getDirectEvaluator(this.getClass().getSimpleName()).evaluate(componentInstance);
//		} catch (InterruptedException | ObjectEvaluationFailedException e) {
//			e.printStackTrace();
//		}

		PCSBasedEvaluationResponseProto response = PCSBasedEvaluationResponseProto.newBuilder().setResult(score).build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
		System.out.println("response completed");
	}

}
