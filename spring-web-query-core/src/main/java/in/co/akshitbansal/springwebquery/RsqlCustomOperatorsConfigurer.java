package in.co.akshitbansal.springwebquery;

import in.co.akshitbansal.springwebquery.operator.RsqlCustomOperator;

import java.util.Collections;
import java.util.Set;

/**
 * Configurer interface for providing custom RSQL operators.
 * <p>
 * Applications can implement this interface and register it as a bean to
 * contribute custom operators to the RSQL query resolution process.
 * </p>
 */
public interface RsqlCustomOperatorsConfigurer {

    /**
     * Returns a set of custom RSQL operators to be registered.
     *
     * @return a set of custom operators, or an empty set if none
     */
    default Set<? extends RsqlCustomOperator<?>> getCustomOperators() {
        return Collections.emptySet();
    }
}
