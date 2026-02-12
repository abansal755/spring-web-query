package io.github.abansal755.spring_web_query.config;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import io.github.abansal755.spring_web_query.RsqlSpecificationArgumentResolver;
import io.github.abansal755.spring_web_query.enums.RsqlOperator;
import io.github.perplexhub.rsql.RSQLJPASupport;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@AutoConfiguration
public class RsqlAutoConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(rsqlSpecificationArgumentResolver());
    }

    @Bean
    public RsqlSpecificationArgumentResolver rsqlSpecificationArgumentResolver() {
        Set<ComparisonOperator> allowedOperators = Arrays
                .stream(RsqlOperator.values())
                .map(RsqlOperator::getOperator)
                .collect(Collectors.toSet());
        RSQLParser rsqlParser = new RSQLParser(allowedOperators);
        return new RsqlSpecificationArgumentResolver(rsqlParser);
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
