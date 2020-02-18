package ai.libs.hyperopt.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.api4.java.common.attributedobjects.ObjectEvaluationFailedException;
import org.jfree.util.Log;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.model.Parameter;
import ai.libs.hasco.pcsbasedoptimization.proto.PCSBasedComponentProto;
import ai.libs.hasco.pcsbasedoptimization.proto.PCSBasedEvaluationResponseProto;
import ai.libs.hasco.pcsbasedoptimization.proto.PCSBasedOptimizerServiceGrpc.PCSBasedOptimizerServiceImplBase;
import ai.libs.hasco.pcsbasedoptimization.proto.PCSBasedParameterProto;
import ai.libs.hyperopt.api.IOptimizationTask;
import io.grpc.stub.StreamObserver;

/**
 * gRPC service implementation for PCSBasedOptimizers
 *
 * @author kadirayk
 *
 */
public class PCSBasedOptimizerServiceImpl<M> extends PCSBasedOptimizerServiceImplBase {

	private IOptimizationTask<M> input;
	private Map<String, String> parameterMapping;

	public PCSBasedOptimizerServiceImpl(final IOptimizationTask<M> input) {
		this.input = input;
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
		ComponentInstance componentInstance = this.resolveSatisfyingInterfaces(this.input.getComponents(), request.getName(), request.getParametersList());

		Double score = 0.0;
		try {
			System.out.println(componentInstance);
			score = this.input.getDirectEvaluator().evaluate(componentInstance);
		} catch (InterruptedException | ObjectEvaluationFailedException e) {
			e.printStackTrace();
		}

		PCSBasedEvaluationResponseProto response = PCSBasedEvaluationResponseProto.newBuilder().setResult(score).build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
		System.out.println("response completed");
	}

	/**
	 * Creates a ComponentInstance based on the given componentName, components, and
	 * a list of component parameters. Recursively resolves satisfying interfaces.
	 *
	 * @param components
	 * @param componentName
	 * @param params
	 * @return
	 */
	private ComponentInstance resolveSatisfyingInterfaces(final Collection<Component> components, final String componentName, final List<PCSBasedParameterProto> params) {
		Optional<Component> opt = components.stream().filter(c -> c.getName().contains(componentName)).findFirst();
		Component cmp = null;
		if (opt.isPresent()) {
			cmp = opt.get();
		} else {
			Log.error("component does not exist:" + componentName);
		}

		Map<String, String> requiredInterfaces = new HashMap<>(); // namespacedName,key
		for (Map.Entry<String, String> e : cmp.getRequiredInterfaces().entrySet()) {
			requiredInterfaces.put(HASCOToPCSConverter.nameSpaceInterface(cmp, e.getValue()), e.getKey());
		}

		Set<Parameter> hascoParams = cmp.getParameters();

		Map<String, String> componentParameters = new HashMap<>();

		for (Parameter hp : hascoParams) {
			Optional<PCSBasedParameterProto> op = params.stream().filter(p -> this.parameterMapping.getOrDefault(p.getKey(), p.getKey()).contains(componentName + "." + hp)).findFirst();
			if (op.isPresent()) {
				PCSBasedParameterProto param = op.get();
				int indexOfLastDot = this.parameterMapping.getOrDefault(param.getKey(), param.getKey()).lastIndexOf(".");
				String key = this.parameterMapping.getOrDefault(param.getKey(), param.getKey()).substring(indexOfLastDot + 1);
				componentParameters.put(key, param.getValue());
			}
		}

		if (requiredInterfaces == null || requiredInterfaces.isEmpty()) {
			Map<String, ComponentInstance> satisfyingInterfaces = new HashMap<>();
			return new ComponentInstance(cmp, componentParameters, satisfyingInterfaces);
		}

		Map<String, ComponentInstance> satisfyingInterfaces = new HashMap<>();
		for (String requiredInterface : requiredInterfaces.keySet()) {
			Optional<PCSBasedParameterProto> optionalParam = params.stream().filter(p -> this.parameterMapping.getOrDefault(p.getKey(), p.getKey()).equals(requiredInterface)).findFirst();
			if (!optionalParam.isPresent()) {
				continue;
			}
			PCSBasedParameterProto param = optionalParam.get();
			String satisfyingClassName = param.getValue();
			ComponentInstance compInstance = this.resolveSatisfyingInterfaces(components, satisfyingClassName, params);
			satisfyingInterfaces.put(requiredInterfaces.get(requiredInterface), compInstance);
		}
		return new ComponentInstance(cmp, componentParameters, satisfyingInterfaces);
	}

}
