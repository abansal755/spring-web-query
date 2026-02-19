package in.co.akshitbansal.springwebquery.config;

import in.co.akshitbansal.springwebquery.RsqlCustomOperatorsConfigurer;
import in.co.akshitbansal.springwebquery.RsqlSpecificationArgumentResolver;
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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@AutoConfiguration
@ConditionalOnClass(RSQLJPAAutoConfiguration.class)
@RequiredArgsConstructor
public class RsqlAutoConfig {

    @Bean
    @ConditionalOnMissingBean(RsqlCustomOperatorsConfigurer.class)
    public RsqlCustomOperatorsConfigurer rsqlCustomOperatorsConfigurer() {
        return new RsqlCustomOperatorsConfigurer() {};
    }

    @Bean
    public RsqlSpecificationArgumentResolver rsqlSpecificationArgumentResolver(List<RsqlCustomOperatorsConfigurer> rsqlCustomOperatorsConfigurers) {
        Set<RsqlOperator> defaultOperators = Arrays
                .stream(RsqlOperator.values())
                .collect(Collectors.toSet());
        Set<? extends RsqlCustomOperator<?>> customOperators = rsqlCustomOperatorsConfigurers
                .stream()
                .flatMap(configurer -> configurer.getCustomOperators().stream())
                .collect(Collectors.toSet());
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
