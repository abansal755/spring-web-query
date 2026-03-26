package in.co.akshitbansal.springwebquery.config;

import in.co.akshitbansal.springwebquery.RSQLCustomOperatorsConfigurer;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryDTOAwarePageableArgumentResolver;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryDTOAwareSpecificationArgumentResolver;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryEntityAwarePageableArgumentResolver;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryEntityAwareSpecificationArgumentResolver;
import io.github.perplexhub.rsql.RSQLJPASupport;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;

@AutoConfiguration
@Slf4j
public class WebQueryBeanAutoConfig {

    private final boolean GLOBAL_ALLOW_OR_OPERATION;
    private final boolean GLOBAL_ALLOW_AND_OPERATION;
    private final int GLOBAL_MAX_AST_DEPTH;

    public WebQueryBeanAutoConfig(
            @Value("${spring-web-query.filtering.allow-or-operation:false}") boolean GLOBAL_ALLOW_OR_OPERATION,
            @Value("${spring-web-query.filtering.allow-and-operation:true}") boolean GLOBAL_ALLOW_AND_OPERATION,
            @Value("${spring-web-query.filtering.max-ast-depth:1}") int GLOBAL_MAX_AST_DEPTH
    ) {
        this.GLOBAL_ALLOW_OR_OPERATION = GLOBAL_ALLOW_OR_OPERATION;
        this.GLOBAL_ALLOW_AND_OPERATION = GLOBAL_ALLOW_AND_OPERATION;
        this.GLOBAL_MAX_AST_DEPTH = GLOBAL_MAX_AST_DEPTH;
        if(GLOBAL_MAX_AST_DEPTH < 0) {
            throw new QueryConfigurationException(MessageFormat.format(
                    "Value for spring-web-query.filtering.max-ast-depth must be non-negative. Provided value: {0}",
                    GLOBAL_MAX_AST_DEPTH
            ));
        }
    }

    @Bean
    @ConditionalOnMissingBean(RSQLCustomOperatorsConfigurer.class)
    public RSQLCustomOperatorsConfigurer rsqlCustomOperatorsConfigurer() {
        return Collections::emptySet;
    }

    @Bean
    public Set<RSQLDefaultOperator> defaultOperatorSet() {
        // Set for checking duplicates
        Set<String> symbolSet = new HashSet<>();

        // Default operators gathered from the RsqlOperator enum
        Set<RSQLDefaultOperator> defaultOperators = new HashSet<>();
        for(RSQLDefaultOperator operator: RSQLDefaultOperator.values()) {
            for(String symbol:operator.getOperator().getSymbols()) {
                // If already an operator is present with the same symbol, throw exception
                if(!symbolSet.add(symbol)) {
                    throw new QueryConfigurationException(MessageFormat.format(
                            "Duplicate operator symbol ''{0}'' found in default RSQL operators. Each operator must be unique.",
                            symbol
                    ));
                }
            }
            defaultOperators.add(operator);
        }
        log.info("Registered default RSQL operators: {}", defaultOperators
                .stream()
                .map(RSQLDefaultOperator::getOperator)
                .toList()
        );
        return Collections.unmodifiableSet(defaultOperators);
    }

    @Bean
    public Set<? extends RSQLCustomOperator<?>> customOperatorSet(
            List<RSQLCustomOperatorsConfigurer> rsqlCustomOperatorsConfigurers,
            Set<RSQLDefaultOperator> defaultOperatorSet
    ) {
        // Set for checking duplicates
        Set<String> symbolSet = new HashSet<>();
        for(RSQLDefaultOperator operator: defaultOperatorSet)
            symbolSet.addAll(Arrays.asList(operator.getOperator().getSymbols()));

        // Custom operators gathered from all the configurers
        Set<RSQLCustomOperator<?>> customOperators = new HashSet<>();
        for(RSQLCustomOperatorsConfigurer configurer : rsqlCustomOperatorsConfigurers) {
            for(RSQLCustomOperator<?> operator : configurer.getCustomOperators()) {
                for(String symbol:operator.getComparisonOperator().getSymbols()) {
                    // If already an operator is present with the same symbol, throw exception
                    if(!symbolSet.add(symbol)) {
                        throw new QueryConfigurationException(MessageFormat.format(
                                "Duplicate operator symbol ''{0}'' found in custom RSQL operators. Each operator must be unique and not overlap with default operators.",
                                symbol
                        ));
                    }
                }
                customOperators.add(operator);
            }
        }
        log.info("Registered custom RSQL operators: {}", customOperators
                .stream()
                .map(RSQLCustomOperator::getComparisonOperator)
                .toList()
        );
        return Collections.unmodifiableSet(customOperators);
    }

    @Bean
    public WebQueryEntityAwareSpecificationArgumentResolver entityAwareSpecArgumentResolver(
            Set<RSQLDefaultOperator> defaultOperatorSet,
            Set<? extends RSQLCustomOperator<?>> customOperatorSet
    ) {
        return new WebQueryEntityAwareSpecificationArgumentResolver(
                defaultOperatorSet,
                customOperatorSet,
                GLOBAL_ALLOW_AND_OPERATION,
                GLOBAL_ALLOW_OR_OPERATION,
                GLOBAL_MAX_AST_DEPTH
        );
    }

    @Bean
    public WebQueryDTOAwareSpecificationArgumentResolver dtoAwareSpecArgumentResolver(
            Set<RSQLDefaultOperator> defaultOperatorSet,
            Set<? extends RSQLCustomOperator<?>> customOperatorSet
    ) {
        return new WebQueryDTOAwareSpecificationArgumentResolver(
                defaultOperatorSet,
                customOperatorSet,
                GLOBAL_ALLOW_AND_OPERATION,
                GLOBAL_ALLOW_OR_OPERATION,
                GLOBAL_MAX_AST_DEPTH
        );
    }

    // Allows RSQL to parse ISO-8601 Timestamp fields
    // eg. 2025-12-08T00:00:00+00:00 or 2025-12-08T00:00:00Z
    // query eg. to query by createTimestamp on 2025-12-08 in UTC
    // use createTimestamp>=2025-12-08T00:00:00%2B00:00;createTimestamp<2025-12-09T00:00:00%2B00:00
    // %2B is URL encoding for +
    @PostConstruct
    public void init() {
        RSQLJPASupport.addConverter(Timestamp.class, value -> {
            OffsetDateTime odt = OffsetDateTime.parse(value);
            Instant instant = odt.toInstant();
            return Timestamp.from(instant);
        });
    }

    @Bean
    public WebQueryEntityAwarePageableArgumentResolver entityAwarePageableArgumentResolver(PageableHandlerMethodArgumentResolver delegate) {
        return new WebQueryEntityAwarePageableArgumentResolver(
                delegate,
                GLOBAL_ALLOW_AND_OPERATION,
                GLOBAL_ALLOW_OR_OPERATION,
                GLOBAL_MAX_AST_DEPTH
        );
    }

    @Bean
    public WebQueryDTOAwarePageableArgumentResolver dtoAwarePageableArgumentResolver(PageableHandlerMethodArgumentResolver delegate) {
        return new WebQueryDTOAwarePageableArgumentResolver(
                delegate,
                GLOBAL_ALLOW_AND_OPERATION,
                GLOBAL_ALLOW_OR_OPERATION,
                GLOBAL_MAX_AST_DEPTH
        );
    }
}
