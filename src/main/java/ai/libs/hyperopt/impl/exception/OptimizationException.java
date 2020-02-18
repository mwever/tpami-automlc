package ai.libs.hyperopt.impl.exception;

import ai.libs.jaicore.ml.core.exception.CheckedJaicoreMLException;

/**
 *
 * @author kadirayk
 *
 */
public class OptimizationException extends CheckedJaicoreMLException {

	public OptimizationException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public OptimizationException(final String message) {
		super(message);
	}

	public OptimizationException(final Throwable cause) {
		super(cause);
	}

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

}
