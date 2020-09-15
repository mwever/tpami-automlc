package benchmark.core.api;

/**
 * Converts a source object of type S into an object of the target type T.
 * @author mwever
 *
 * @param <S> The type of the source object.
 * @param <T> The type of the target object.
 */
public interface IConverter<S, T> {

	/**
	 * Converts a source object into an object of the target type.
	 * @param source The source object.
	 * @return The object in the target type representation.
	 */
	public T convert(S source) throws ConversionFailedException;

}
