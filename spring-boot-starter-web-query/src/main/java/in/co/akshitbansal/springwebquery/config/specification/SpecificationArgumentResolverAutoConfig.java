package in.co.akshitbansal.springwebquery.config.specification;

import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryDTOAwareSpecificationArgumentResolver;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryEntityAwareSpecificationArgumentResolver;
import in.co.akshitbansal.springwebquery.validator.QueryParamNameValidator;
import in.co.akshitbansal.springwebquery.validator.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.text.MessageFormat;
import java.util.Set;

@AutoConfiguration
public class SpecificationArgumentResolverAutoConfig {

    private final String GLOBAL_FILTER_PARAM_NAME;
    private final boolean GLOBAL_ALLOW_OR_OPERATION;
    private final boolean GLOBAL_ALLOW_AND_OPERATION;
    private final int GLOBAL_MAX_AST_DEPTH;

    public SpecificationArgumentResolverAutoConfig(
            @Value("${spring-web-query.filtering.filter-param-name:filter}") String GLOBAL_FILTER_PARAM_NAME,
            @Value("${spring-web-query.filtering.allow-or-operation:false}") boolean GLOBAL_ALLOW_OR_OPERATION,
            @Value("${spring-web-query.filtering.allow-and-operation:true}") boolean GLOBAL_ALLOW_AND_OPERATION,
            @Value("${spring-web-query.filtering.max-ast-depth:1}") int GLOBAL_MAX_AST_DEPTH
    ) {
        // Validating GLOBAL_FILTER_PARAM_NAME
        Validator<String> queryParamNameValidator = new QueryParamNameValidator();
        queryParamNameValidator.validate(GLOBAL_FILTER_PARAM_NAME);
        this.GLOBAL_FILTER_PARAM_NAME = GLOBAL_FILTER_PARAM_NAME;

        this.GLOBAL_ALLOW_OR_OPERATION = GLOBAL_ALLOW_OR_OPERATION;
        this.GLOBAL_ALLOW_AND_OPERATION = GLOBAL_ALLOW_AND_OPERATION;

        // Validating GLOBAL_MAX_AST_DEPTH
        if(GLOBAL_MAX_AST_DEPTH < 0) {
            throw new QueryConfigurationException(MessageFormat.format(
                    "Value for spring-web-query.filtering.max-ast-depth must be non-negative. Provided value: {0}",
                    GLOBAL_MAX_AST_DEPTH
            ));
        }
        this.GLOBAL_MAX_AST_DEPTH = GLOBAL_MAX_AST_DEPTH;
    }

    @Bean
    public WebQueryEntityAwareSpecificationArgumentResolver entityAwareSpecArgumentResolver(
            Set<RSQLDefaultOperator> defaultOperatorSet,
            Set<? extends RSQLCustomOperator<?>> customOperatorSet
    ) {
        return new WebQueryEntityAwareSpecificationArgumentResolver(
                GLOBAL_FILTER_PARAM_NAME,
                GLOBAL_ALLOW_AND_OPERATION,
                GLOBAL_ALLOW_OR_OPERATION,
                GLOBAL_MAX_AST_DEPTH,
                defaultOperatorSet,
                customOperatorSet
        );
    }

    @Bean
    public WebQueryDTOAwareSpecificationArgumentResolver dtoAwareSpecArgumentResolver(
            Set<RSQLDefaultOperator> defaultOperatorSet,
            Set<? extends RSQLCustomOperator<?>> customOperatorSet
    ) {
        return new WebQueryDTOAwareSpecificationArgumentResolver(
                GLOBAL_FILTER_PARAM_NAME,
                GLOBAL_ALLOW_AND_OPERATION,
                GLOBAL_ALLOW_OR_OPERATION,
                GLOBAL_MAX_AST_DEPTH,
                defaultOperatorSet,
                customOperatorSet
        );
    }
}
