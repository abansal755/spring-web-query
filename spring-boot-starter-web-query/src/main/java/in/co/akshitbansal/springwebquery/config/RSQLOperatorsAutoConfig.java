package in.co.akshitbansal.springwebquery.config;

import in.co.akshitbansal.springwebquery.RSQLCustomOperatorsConfigurer;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.text.MessageFormat;
import java.util.*;

/**
 * Registers validated default and custom RSQL operator sets for starter consumers.
 */
@AutoConfiguration
@Slf4j
public class RSQLOperatorsAutoConfig {

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
}
