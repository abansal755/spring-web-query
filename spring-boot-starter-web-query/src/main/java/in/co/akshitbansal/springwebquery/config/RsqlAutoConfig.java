package in.co.akshitbansal.springwebquery.config;

import in.co.akshitbansal.springwebquery.RsqlCustomOperatorsConfigurer;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.operator.RsqlCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RsqlOperator;
import in.co.akshitbansal.springwebquery.resolver.DtoAwareRestrictedPageableArgumentResolver;
import in.co.akshitbansal.springwebquery.resolver.DtoAwareRsqlSpecArgumentResolver;
import in.co.akshitbansal.springwebquery.resolver.EntityAwareRestrictedPageableArgumentResolver;
import in.co.akshitbansal.springwebquery.resolver.EntityAwareRsqlSpecArgumentResolver;
import in.co.akshitbansal.springwebquery.util.AnnotationUtil;
import io.github.perplexhub.rsql.RSQLJPAAutoConfiguration;
import io.github.perplexhub.rsql.RSQLJPASupport;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;

@AutoConfiguration
@ConditionalOnClass(RSQLJPAAutoConfiguration.class)
@RequiredArgsConstructor
@Slf4j
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
        log.info("Registered default RSQL operators: {}", defaultOperators
                .stream()
                .map(RsqlOperator::getOperator)
                .toList()
        );
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
        log.info("Registered custom RSQL operators: {}", customOperators
                .stream()
                .map(RsqlCustomOperator::getComparisonOperator)
                .toList()
        );
        return customOperators;
    }

    @Bean
    public AnnotationUtil annotationUtil(Set<? extends RsqlCustomOperator<?>> customOperatorSet) {
        return new AnnotationUtil(customOperatorSet);
    }

    @Bean
    public EntityAwareRsqlSpecArgumentResolver entityAwareRsqlSpecArgumentResolver(
            Set<RsqlOperator> defaultOperatorSet,
            Set<? extends RsqlCustomOperator<?>> customOperatorSet,
            AnnotationUtil annotationUtil
    ) {
        return new EntityAwareRsqlSpecArgumentResolver(defaultOperatorSet, customOperatorSet, annotationUtil);
    }

    @Bean
    public DtoAwareRsqlSpecArgumentResolver dtoAwareRsqlSpecArgumentResolver(
            Set<RsqlOperator> defaultOperatorSet,
            Set<? extends RsqlCustomOperator<?>> customOperatorSet,
            AnnotationUtil annotationUtil
    ) {
        return new DtoAwareRsqlSpecArgumentResolver(defaultOperatorSet, customOperatorSet, annotationUtil);
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
    public EntityAwareRestrictedPageableArgumentResolver entityAwareRestrictedPageableArgumentResolver(
            PageableHandlerMethodArgumentResolver delegate,
            AnnotationUtil annotationUtil
    ) {
        return new EntityAwareRestrictedPageableArgumentResolver(delegate, annotationUtil);
    }

    @Bean
    public DtoAwareRestrictedPageableArgumentResolver dtoRestrictedPageableArgumentResolver(
            PageableHandlerMethodArgumentResolver delegate,
            AnnotationUtil annotationUtil
    ) {
        return new DtoAwareRestrictedPageableArgumentResolver(delegate, annotationUtil);
    }
}
