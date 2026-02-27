package in.co.akshitbansal.springwebquery.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@AutoConfiguration
@Slf4j
public class PaginationCustomizationAutoConfig {

    @Value("${api.pagination.max-page-size:100}")
    private int MAX_PAGE_SIZE;

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer maxPageSizeCustomizer() {
        log.info("{} registered to set max page size to {}",
                PageableHandlerMethodArgumentResolverCustomizer.class.getSimpleName(), MAX_PAGE_SIZE);
        return resolver -> resolver.setMaxPageSize(MAX_PAGE_SIZE);
    }
}
