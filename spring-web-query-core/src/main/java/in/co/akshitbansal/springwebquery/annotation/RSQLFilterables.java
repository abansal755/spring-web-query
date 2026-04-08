package in.co.akshitbansal.springwebquery.annotation;

import java.lang.annotation.*;

/**
 * Container annotation for repeatable {@link RSQLFilterable} declarations on a field.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RSQLFilterables {

	/**
	 * Repeated {@link RSQLFilterable} entries declared on the same field.
	 *
	 * @return filterability declarations for the field
	 */
	RSQLFilterable[] value();
}
