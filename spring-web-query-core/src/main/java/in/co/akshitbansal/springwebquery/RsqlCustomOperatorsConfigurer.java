package in.co.akshitbansal.springwebquery;

import in.co.akshitbansal.springwebquery.operator.RsqlCustomOperator;

import java.util.Set;

/**
 * Configurer interface for providing custom RSQL operators.
 * <p>
 * Applications can implement this interface and register it as a bean to
 * contribute custom operators to the RSQL query resolution process.
 * </p>
 */
@FunctionalInterface
public interface RsqlCustomOperatorsConfigurer {

    /**
     * Returns a set of custom RSQL operators to be registered.
     * <p>
     * If multiple {@code RsqlCustomOperatorsConfigurer} beans are present in the
     * Spring context, their results will be combined into a single set of
     * operators available for query resolution.
     * </p>
     *
     * @return a set of custom operators, or an empty set if none
     */
    Set<? extends RsqlCustomOperator<?>> getCustomOperators();
}
