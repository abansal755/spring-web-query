package io.github.abansal755.springwebquery.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@AutoConfiguration
public class PaginationCustomizationAutoConfig {

    @Value("${api.pagination.max-page-size:100}")
    private int MAX_PAGE_SIZE;

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer maxPageSizeCustomizer() {
        return resolver -> resolver.setMaxPageSize(MAX_PAGE_SIZE);
    }
}
