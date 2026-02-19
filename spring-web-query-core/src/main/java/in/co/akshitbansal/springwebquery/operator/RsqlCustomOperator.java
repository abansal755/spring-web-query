package in.co.akshitbansal.springwebquery.operator;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import io.github.perplexhub.rsql.RSQLCustomPredicateInput;
import jakarta.persistence.criteria.Predicate;

public interface RsqlCustomOperator<T extends Comparable<?>> {

    ComparisonOperator getComparisonOperator();
    Class<T> getType();
    Predicate toPredicate(RSQLCustomPredicateInput input);
}
