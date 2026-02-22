package in.co.akshitbansal.springwebquery.config;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.RsqlCustomOperatorsConfigurer;
import in.co.akshitbansal.springwebquery.RsqlSpecificationArgumentResolver;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.operator.RsqlCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RsqlOperator;
import io.github.perplexhub.rsql.RSQLJPAAutoConfiguration;
import io.github.perplexhub.rsql.RSQLJPASupport;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@AutoConfiguration
@ConditionalOnClass(RSQLJPAAutoConfiguration.class)
@RequiredArgsConstructor
public class RsqlAutoConfig {

    @Bean
    @ConditionalOnMissingBean(RsqlCustomOperatorsConfigurer.class)
    public RsqlCustomOperatorsConfigurer rsqlCustomOperatorsConfigurer() {
        return Collections::emptySet;
    }

    @Bean
    public RsqlSpecificationArgumentResolver rsqlSpecificationArgumentResolver(List<RsqlCustomOperatorsConfigurer> rsqlCustomOperatorsConfigurers) {
        // Set for checking duplicates
        Set<ComparisonOperator> operatorSet = new HashSet<>();

        // Default operators gathered from the RsqlOperator enum
        Set<RsqlOperator> defaultOperators = new HashSet<>();
        for(RsqlOperator operator: RsqlOperator.values()) {
            // If already an operator is present with the same symbol, throw exception
            if(!operatorSet.add(operator.getOperator())) {
                throw new QueryConfigurationException(MessageFormat.format(
                        "Duplicate operator ''{0}'' found in default RSQL operators. Each operator must be unique.", operator.getOperator()
                ));
            }
            defaultOperators.add(operator);
        }

        // Custom operators gathered from all the configurers
        Set<RsqlCustomOperator<?>> customOperators = new HashSet<>();
        for(RsqlCustomOperatorsConfigurer configurer : rsqlCustomOperatorsConfigurers) {
            for(RsqlCustomOperator<?> operator : configurer.getCustomOperators()) {
                // If already an operator is present with the same symbol, throw exception
                if(!operatorSet.add(operator.getComparisonOperator())) {
                    throw new QueryConfigurationException(MessageFormat.format(
                            "Duplicate operator ''{0}'' found in custom RSQL operators. Each operator must be unique and not overlap with default operators.",
                            operator.getComparisonOperator()
                    ));
                }
                customOperators.add(operator);
            }
        }
        return new RsqlSpecificationArgumentResolver(defaultOperators, customOperators);
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
}
