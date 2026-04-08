package in.co.akshitbansal.springwebquery.config;

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

    private final int MAX_PAGE_SIZE;
    private final int DEFAULT_PAGE_SIZE;

    public PaginationCustomizationAutoConfig(
            @Value("${spring-web-query.pagination.max-page-size:100}") int MAX_PAGE_SIZE,
            @Value("${spring-web-query.pagination.default-page-size:20}") int DEFAULT_PAGE_SIZE
    ) {
        if(MAX_PAGE_SIZE <= 0) throw new QueryConfigurationException(MessageFormat.format(
                "Value for spring-web-query.pagination.max-page-size must be greater than 0. Provided value: {0}",
                MAX_PAGE_SIZE
        ));
        this.MAX_PAGE_SIZE = MAX_PAGE_SIZE;

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
        this.DEFAULT_PAGE_SIZE = DEFAULT_PAGE_SIZE;
    }

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer defaultPageSizeCustomizer() {
        log.info("{} registered to set default page size to {}",
                PageableHandlerMethodArgumentResolverCustomizer.class.getSimpleName(), DEFAULT_PAGE_SIZE);
        return resolver -> resolver.setFallbackPageable(Pageable.ofSize(DEFAULT_PAGE_SIZE));
    }

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer maxPageSizeCustomizer() {
        log.info("{} registered to set max page size to {}",
                PageableHandlerMethodArgumentResolverCustomizer.class.getSimpleName(), MAX_PAGE_SIZE);
        return resolver -> resolver.setMaxPageSize(MAX_PAGE_SIZE);
    }
}
