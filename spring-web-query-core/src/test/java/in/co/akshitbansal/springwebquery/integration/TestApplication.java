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

package in.co.akshitbansal.springwebquery.integration;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.ast.ValidationRSQLVisitorFactory;
import in.co.akshitbansal.springwebquery.customoperator.IsLongGreaterThanFiveOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import in.co.akshitbansal.springwebquery.pathmapper.DTOToEntityPathMapperFactory;
import in.co.akshitbansal.springwebquery.tupleconverter.PreferredConstructorDiscovererFactory;
import in.co.akshitbansal.springwebquery.tupleconverter.TupleConverterFactory;
import in.co.akshitbansal.springwebquery.validator.FilterableFieldValidator;
import in.co.akshitbansal.springwebquery.validator.SortableFieldValidator;
import io.github.perplexhub.rsql.RSQLJPAAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootApplication
@EntityScan(basePackages = "in.co.akshitbansal.springwebquery.common.entity")
@EnableJpaRepositories(basePackages = {
		"in.co.akshitbansal.springwebquery.integration.repository",
		"in.co.akshitbansal.springwebquery.repository"
})
@Import(RSQLJPAAutoConfiguration.class)
public class TestApplication {

	// Operators configuration
	@Bean
	public IsLongGreaterThanFiveOperator isLongGreaterThanFiveOperator() {
		return new IsLongGreaterThanFiveOperator();
	}

	@Bean
	public Map<Class<?>, RSQLCustomOperator<?>> customOperatorMap(IsLongGreaterThanFiveOperator isLongGreaterThanFiveOperator) {
		return Map.of(IsLongGreaterThanFiveOperator.class, isLongGreaterThanFiveOperator);
	}

	@Bean
	public Set<ComparisonOperator> allowedOperatorSet(IsLongGreaterThanFiveOperator isLongGreaterThanFiveOperator) {
		Stream<ComparisonOperator> defaultOperators = Arrays
				.stream(RSQLDefaultOperator.values())
				.map(RSQLDefaultOperator::getOperator);
		Stream<ComparisonOperator> customOperators = Stream
				.of(isLongGreaterThanFiveOperator)
				.map(RSQLCustomOperator::getComparisonOperator);
		return Stream
				.concat(defaultOperators, customOperators)
				.collect(Collectors.toSet());
	}

	// RSQL Parser configuration
	@Bean
	public RSQLParser rsqlParser(Set<ComparisonOperator> allowedOperatorSet) {
		return new RSQLParser(allowedOperatorSet);
	}

	// Validators configuration
	@Bean
	public SortableFieldValidator sortableFieldValidator() {
		return new SortableFieldValidator();
	}

	@Bean
	public FilterableFieldValidator filterableFieldValidator(Map<Class<?>, RSQLCustomOperator<?>> customOperatorMap) {
		return new FilterableFieldValidator(customOperatorMap);
	}

	// Factories configuration
	@Bean
	public DTOToEntityPathMapperFactory dtoToEntityPathMapperFactory() {
		return new DTOToEntityPathMapperFactory();
	}

	@Bean
	public PreferredConstructorDiscovererFactory preferredConstructorDiscovererFactory() {
		return new PreferredConstructorDiscovererFactory(false);
	}

	@Bean
	public TupleConverterFactory tupleConverterFactory(PreferredConstructorDiscovererFactory preferredConstructorDiscovererFactory) {
		return new TupleConverterFactory(preferredConstructorDiscovererFactory);
	}

	@Bean
	public ValidationRSQLVisitorFactory validationRSQLVisitorFactory(
			DTOToEntityPathMapperFactory dtoToEntityPathMapperFactory,
			FilterableFieldValidator filterableFieldValidator
	) {
		return new ValidationRSQLVisitorFactory(dtoToEntityPathMapperFactory, filterableFieldValidator);
	}
}
