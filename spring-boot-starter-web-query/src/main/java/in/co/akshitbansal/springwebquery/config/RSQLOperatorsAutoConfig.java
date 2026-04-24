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

package in.co.akshitbansal.springwebquery.config;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import io.github.perplexhub.rsql.RSQLCustomPredicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Registers validated default operators, custom operators, and the derived
 * parser/predicate helper collections used by the starter.
 */
@AutoConfiguration
@Slf4j
public class RSQLOperatorsAutoConfig {

	/**
	 * Collects the built-in RSQL operators supported by the library and
	 * validates that their symbols are unique.
	 *
	 * @return immutable set of default operators
	 */
	@Bean
	public Set<RSQLDefaultOperator> defaultOperatorSet() {
		// Set for checking duplicates
		Set<String> symbolSet = new HashSet<>();

		// Default operators gathered from the RsqlOperator enum
		Set<RSQLDefaultOperator> defaultOperators = new HashSet<>();
		for (RSQLDefaultOperator operator: RSQLDefaultOperator.values()) {
			for (String symbol: operator.getOperator().getSymbols()) {
				// If already an operator is present with the same symbol, throw exception
				if (!symbolSet.add(symbol)) {
					throw new QueryConfigurationException(MessageFormat.format(
							"Duplicate operator symbol ''{0}'' found in default RSQL operators. Each operator must be unique.",
							symbol
					));
				}
			}
			defaultOperators.add(operator);
		}
		log.info(
				"Registered default RSQL operators: {}", defaultOperators
						.stream()
						.map(RSQLDefaultOperator::getOperator)
						.toList()
		);
		return Collections.unmodifiableSet(defaultOperators);
	}

	/**
	 * Collects user-defined custom operators and validates that none of their
	 * symbols overlap with each other or the built-in set.
	 *
	 * @param customOperatorBeans custom operator beans discovered by Spring
	 * @param defaultOperatorSet validated default operators
	 *
	 * @return immutable set of validated custom operators
	 */
	@Bean
	public Set<? extends RSQLCustomOperator<?>> customOperatorSet(
			List<RSQLCustomOperator<?>> customOperatorBeans,
			Set<RSQLDefaultOperator> defaultOperatorSet
	) {
		// Set for checking duplicates
		Set<String> symbolSet = new HashSet<>();
		for (RSQLDefaultOperator operator: defaultOperatorSet)
			symbolSet.addAll(Arrays.asList(operator.getOperator().getSymbols()));

		Set<RSQLCustomOperator<?>> customOperators = new HashSet<>();
		for (RSQLCustomOperator<?> operator: customOperatorBeans) {
			for (String symbol: operator.getComparisonOperator().getSymbols()) {
				// If already an operator is present with the same symbol, throw exception
				if (!symbolSet.add(symbol)) {
					throw new QueryConfigurationException(MessageFormat.format(
							"Duplicate operator symbol ''{0}'' found in custom RSQL operators. Each operator must be unique and not overlap with default operators.",
							symbol
					));
				}
			}
			customOperators.add(operator);
		}
		log.info(
				"Registered custom RSQL operators: {}", customOperators
						.stream()
						.map(RSQLCustomOperator::getComparisonOperator)
						.toList()
		);
		return Collections.unmodifiableSet(customOperators);
	}

	/**
	 * Builds the complete set of comparison operators accepted by the shared RSQL parser.
	 *
	 * @param defaultOperatorSet validated default operators
	 * @param customOperatorSet validated custom operators
	 *
	 * @return immutable set of allowed comparison operators
	 */
	@Bean
	public Set<ComparisonOperator> allowedOperatorSet(
			Set<RSQLDefaultOperator> defaultOperatorSet,
			Set<? extends RSQLCustomOperator<?>> customOperatorSet
	) {
		Stream<ComparisonOperator> defaultOperatorsStream = defaultOperatorSet
				.stream()
				.map(RSQLDefaultOperator::getOperator);
		Stream<ComparisonOperator> customOperatorsStream = customOperatorSet
				.stream()
				.map(RSQLCustomOperator::getComparisonOperator);
		return Collections.unmodifiableSet((Set<ComparisonOperator>) Stream
				.concat(defaultOperatorsStream, customOperatorsStream)
				.collect(Collectors.toCollection(HashSet::new)));
	}

	/**
	 * Adapts registered custom operators into the predicate format expected by
	 * the underlying {@code rsql-jpa} integration.
	 *
	 * @param customOperatorSet validated custom operators
	 *
	 * @return immutable list of custom predicates
	 */
	@Bean
	public List<RSQLCustomPredicate<?>> customPredicates(Set<? extends RSQLCustomOperator<?>> customOperatorSet) {
		List<RSQLCustomPredicate<?>> customPredicates = customOperatorSet
				.stream()
				.map(operator -> new RSQLCustomPredicate<>(
						operator.getComparisonOperator(),
						operator.getType(),
						operator::toPredicate
				))
				.collect(Collectors.toCollection(ArrayList::new));
		return Collections.unmodifiableList(customPredicates);
	}

	/**
	 * Registers custom operators by implementation class for downstream
	 * validator lookups.
	 *
	 * @param customOperatorSet validated custom operators
	 *
	 * @return immutable custom operator registry
	 */
	@Bean
	public Map<Class<?>, RSQLCustomOperator<?>> customOperatorMap(Set<? extends RSQLCustomOperator<?>> customOperatorSet) {
		Map<Class<?>, RSQLCustomOperator<?>> customOperatorMap = customOperatorSet
				.stream()
				.collect(Collectors.toMap(
						RSQLCustomOperator::getClass,
						operator -> operator,
						// Duplicates won't be present since validation is already done above in customOperatorSet method
						(existing, duplicate) -> existing,
						HashMap::new
				));
		return Collections.unmodifiableMap(customOperatorMap);
	}
}
