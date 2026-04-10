package in.co.akshitbansal.springwebquery.config;

import in.co.akshitbansal.springwebquery.SpringWebQueryProperties;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.validator.QueryParamNameValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.text.MessageFormat;

@AutoConfiguration
@Slf4j
public class SpringWebQueryPropertiesAutoConfig {

	@Bean
	public SpringWebQueryProperties springWebQueryProperties(
			@Value("${spring-web-query.filtering.filter-param-name:filter}") String globalFilterParamName,
			@Value("${spring-web-query.filtering.allow-and-operation:true}") boolean globalAllowAndOperation,
			@Value("${spring-web-query.filtering.allow-or-operation:false}") boolean globalAllowOrOperation,
			@Value("${spring-web-query.filtering.max-ast-depth:1}") int globalMaxASTDepth,
			QueryParamNameValidator queryParamNameValidator
	) {
		// Validating globalFilterParamName
		queryParamNameValidator.validate(globalFilterParamName);

		// Validating globalMaxASTDepth
		if (globalMaxASTDepth < 0) {
			throw new QueryConfigurationException(MessageFormat.format(
					"Value for spring-web-query.filtering.max-ast-depth must be non-negative. Provided value: {0}",
					globalMaxASTDepth
			));
		}

		SpringWebQueryProperties properties = new SpringWebQueryProperties(
				globalFilterParamName,
				globalAllowAndOperation,
				globalAllowOrOperation,
				globalMaxASTDepth
		);
		log.info("Global spring-web-query configuration: {}", properties);
		return properties;
	}
}
