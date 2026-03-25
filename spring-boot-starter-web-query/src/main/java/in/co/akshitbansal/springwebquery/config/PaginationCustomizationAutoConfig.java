package in.co.akshitbansal.springwebquery.config;

import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

import java.text.MessageFormat;

@AutoConfiguration
@Slf4j
public class PaginationCustomizationAutoConfig {

    private final int MAX_PAGE_SIZE;

    public PaginationCustomizationAutoConfig(@Value("${spring-web-query.pagination.max-page-size:100}") int MAX_PAGE_SIZE) {
        this.MAX_PAGE_SIZE = MAX_PAGE_SIZE;
        if(MAX_PAGE_SIZE <= 0) throw new QueryConfigurationException(MessageFormat.format(
                "Value for spring-web-query.pagination.max-page-size must be greater than 0. Provided value: {0}",
                MAX_PAGE_SIZE
        ));
    }

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer maxPageSizeCustomizer() {
        log.info("{} registered to set max page size to {}",
                PageableHandlerMethodArgumentResolverCustomizer.class.getSimpleName(), MAX_PAGE_SIZE);
        return resolver -> resolver.setMaxPageSize(MAX_PAGE_SIZE);
    }
}
