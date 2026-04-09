package in.co.akshitbansal.springwebquery.config;

import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.validator.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Map;

@AutoConfiguration
public class ValidatorAutoConfig {

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

	@Bean
	public Validator<FilterableFieldValidator.FilterableField> filterableFieldValidator(
			Map<Class<?>, RSQLCustomOperator<?>> customOperatorMap
	) {
		return new FilterableFieldValidator(customOperatorMap);
	}
}
