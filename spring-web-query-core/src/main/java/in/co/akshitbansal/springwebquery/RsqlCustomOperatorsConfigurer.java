package in.co.akshitbansal.springwebquery;

import in.co.akshitbansal.springwebquery.operator.RsqlCustomOperator;

import java.util.Collections;
import java.util.Set;

public interface RsqlCustomOperatorsConfigurer {

    default Set<? extends RsqlCustomOperator<?>> getCustomOperators() {
        return Collections.emptySet();
    }
}
