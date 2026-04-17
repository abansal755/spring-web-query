package in.co.akshitbansal.springwebquery.resolver.field;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.lang.reflect.Field;

/**
 * Value object returned by {@link FieldResolver} implementations after path
 * resolution.
 *
 * <p>It carries the resolved entity-backed field path to use in downstream
 * query construction together with the reflected terminal field that callers
 * may validate for filtering or sorting rules.</p>
 */
@RequiredArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class ResolutionResult {

	/**
	 * Resolved field path understood by downstream entity-backed query code.
	 */
	private final String fieldName;

	/**
	 * Terminal field reached while resolving the request selector path.
	 */
	private final Field terminalField;
}
