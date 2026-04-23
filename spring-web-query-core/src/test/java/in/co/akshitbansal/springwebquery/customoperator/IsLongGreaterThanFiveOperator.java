package in.co.akshitbansal.springwebquery.customoperator;

import cz.jirutka.rsql.parser.ast.Arity;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import io.github.perplexhub.rsql.RSQLCustomPredicateInput;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class IsLongGreaterThanFiveOperator implements RSQLCustomOperator<Long> {

	private final ComparisonOperator comparisonOperator = new ComparisonOperator("=isGtFive=", Arity.nary(0));
	private final Class<Long> type = Long.class;

	@Override
	public ComparisonOperator getComparisonOperator() {
		return comparisonOperator;
	}

	@Override
	public Class<Long> getType() {
		return type;
	}

	@Override
	public Predicate toPredicate(RSQLCustomPredicateInput input) {
		CriteriaBuilder cb = input.getCriteriaBuilder();
		return cb.equal(input.getPath(), 5);
	}
}
