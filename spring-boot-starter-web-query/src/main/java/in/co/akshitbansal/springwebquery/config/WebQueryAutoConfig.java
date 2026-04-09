package in.co.akshitbansal.springwebquery.config;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.ast.ValidationRSQLVisitorFactory;
import in.co.akshitbansal.springwebquery.resolver.FieldResolverFactory;
import in.co.akshitbansal.springwebquery.validator.FilterableFieldValidator;
import in.co.akshitbansal.springwebquery.validator.Validator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.Set;

@AutoConfiguration
public class WebQueryAutoConfig {

	@Bean
	public RSQLParser rsqlParser(Set<ComparisonOperator> allowedOperatorSet) {
		return new RSQLParser(allowedOperatorSet);
	}

	@Bean
	public FieldResolverFactory fieldResolverFactory() {
		return new FieldResolverFactory();
	}

	@Bean
	public ValidationRSQLVisitorFactory validationRSQLVisitorFactory(
			FieldResolverFactory fieldResolverFactory,
			Validator<FilterableFieldValidator.FilterableField> filterableFieldValidator
	) {
		return new ValidationRSQLVisitorFactory(fieldResolverFactory, filterableFieldValidator);
	}
}
