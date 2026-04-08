package in.co.akshitbansal.springwebquery.config.pageable;

import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

import java.text.MessageFormat;

@AutoConfiguration
@Slf4j
public class PaginationCustomizationAutoConfig {

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer maxPageSizeCustomizer(
            @Value("${spring-web-query.pagination.max-page-size:100}") int MAX_PAGE_SIZE,
            @Value("${spring-web-query.pagination.default-page-size:20}") int DEFAULT_PAGE_SIZE
    ) {
        // Validating MAX_PAGE_SIZE
        if(MAX_PAGE_SIZE <= 0) throw new QueryConfigurationException(MessageFormat.format(
                "Value for spring-web-query.pagination.max-page-size must be greater than 0. Provided value: {0}",
                MAX_PAGE_SIZE
        ));

        // Validating DEFAULT_PAGE_SIZE
        if(DEFAULT_PAGE_SIZE <= 0) {
            throw new QueryConfigurationException(MessageFormat.format(
                    "Value for spring-web-query.pagination.default-page-size must be greater than 0. Provided value: {0}",
                    DEFAULT_PAGE_SIZE
            ));
        }
        if(DEFAULT_PAGE_SIZE > MAX_PAGE_SIZE) {
            throw new QueryConfigurationException(MessageFormat.format(
                    "Value for spring-web-query.pagination.default-page-size must be less than or equal to spring-web-query.pagination.max-page-size. Provided values: {0} and {1}",
                    DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE
            ));
        }

        log.info("{} registered to set max page size to {} and default page size to {}",
                PageableHandlerMethodArgumentResolverCustomizer.class.getSimpleName(), MAX_PAGE_SIZE, DEFAULT_PAGE_SIZE);
        return resolver -> {
            resolver.setMaxPageSize(MAX_PAGE_SIZE);
            resolver.setFallbackPageable(Pageable.ofSize(MAX_PAGE_SIZE));
        };
    }
}
