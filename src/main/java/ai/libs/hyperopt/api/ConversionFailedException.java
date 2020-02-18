package ai.libs.hyperopt.api;

public class ConversionFailedException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = -808347403461363324L;

	public ConversionFailedException() {
		super();
	}

	public ConversionFailedException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ConversionFailedException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public ConversionFailedException(final String message) {
		super(message);
	}

	public ConversionFailedException(final Throwable cause) {
		super(cause);
	}

}
