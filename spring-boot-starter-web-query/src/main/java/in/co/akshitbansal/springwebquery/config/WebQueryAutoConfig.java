package in.co.akshitbansal.springwebquery.config;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.validator.FieldMappingsValidator;
import in.co.akshitbansal.springwebquery.validator.QueryParamNameValidator;
import in.co.akshitbansal.springwebquery.validator.SortableFieldValidator;
import in.co.akshitbansal.springwebquery.validator.Validator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Set;

@AutoConfiguration
public class WebQueryAutoConfig {

	@Bean
	public RSQLParser rsqlParser(Set<ComparisonOperator> allowedOperatorSet) {
		return new RSQLParser(allowedOperatorSet);
	}

	@Bean
	public Validator<String> queryParamNameValidator() {
		return new QueryParamNameValidator();
	}

	@Bean
	public Validator<SortableFieldValidator.SortableField> sortableFieldValidator() {
		return new SortableFieldValidator();
	}

	@Bean
	public Validator<List<FieldMapping>> fieldMappingsValidator() {
		return new FieldMappingsValidator();
	}
}
