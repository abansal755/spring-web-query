package in.co.akshitbansal.springwebquery.config;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import in.co.akshitbansal.springwebquery.RestrictedPageableArgumentResolver;
import in.co.akshitbansal.springwebquery.RsqlCustomOperatorsConfigurer;
import in.co.akshitbansal.springwebquery.RsqlSpecificationArgumentResolver;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.operator.RsqlCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RsqlOperator;
import in.co.akshitbansal.springwebquery.util.AnnotationUtil;
import io.github.perplexhub.rsql.RSQLJPAAutoConfiguration;
import io.github.perplexhub.rsql.RSQLJPASupport;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;

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
    public Set<RsqlOperator> defaultOperatorSet() {
        // Set for checking duplicates
        Set<String> symbolSet = new HashSet<>();

        // Default operators gathered from the RsqlOperator enum
        Set<RsqlOperator> defaultOperators = new HashSet<>();
        for(RsqlOperator operator: RsqlOperator.values()) {
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
        return defaultOperators;
    }

    @Bean
    public Set<? extends RsqlCustomOperator<?>> customOperatorSet(
            List<RsqlCustomOperatorsConfigurer> rsqlCustomOperatorsConfigurers,
            Set<RsqlOperator> defaultOperatorSet
    ) {
        // Set for checking duplicates
        Set<String> symbolSet = new HashSet<>();
        for(RsqlOperator operator: defaultOperatorSet)
            symbolSet.addAll(Arrays.asList(operator.getOperator().getSymbols()));

        // Custom operators gathered from all the configurers
        Set<RsqlCustomOperator<?>> customOperators = new HashSet<>();
        for(RsqlCustomOperatorsConfigurer configurer : rsqlCustomOperatorsConfigurers) {
            for(RsqlCustomOperator<?> operator : configurer.getCustomOperators()) {
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
        return customOperators;
    }

    @Bean
    public AnnotationUtil annotationUtil(Set<? extends RsqlCustomOperator<?>> customOperatorSet) {
        return new AnnotationUtil(customOperatorSet);
    }

    @Bean
    public RsqlSpecificationArgumentResolver rsqlSpecificationArgumentResolver(
            Set<RsqlOperator> defaultOperatorSet,
            Set<? extends RsqlCustomOperator<?>> customOperatorSet,
            AnnotationUtil annotationUtil
    ) {
        return new RsqlSpecificationArgumentResolver(defaultOperatorSet, customOperatorSet, annotationUtil);
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
    public RestrictedPageableArgumentResolver restrictedPageableArgumentResolver(
            List<PageableHandlerMethodArgumentResolverCustomizer> customizers,
            AnnotationUtil annotationUtil
    ) {
        PageableHandlerMethodArgumentResolver delegate = new PageableHandlerMethodArgumentResolver();
        for (PageableHandlerMethodArgumentResolverCustomizer customizer : customizers)
            customizer.customize(delegate);
        return new RestrictedPageableArgumentResolver(delegate, annotationUtil);
    }
}
