/*
 * Copyright 2026-present Akshit Bansal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package in.co.akshitbansal.springwebquery.config.customoperator;

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
		return cb.greaterThan(input.getPath().as(Long.class), 5L);
	}
}
