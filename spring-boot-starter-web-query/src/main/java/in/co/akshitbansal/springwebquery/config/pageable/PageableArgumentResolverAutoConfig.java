package in.co.akshitbansal.springwebquery.config.pageable;

import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryDTOAwarePageableArgumentResolver;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryEntityAwarePageableArgumentResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

/**
 * Creates pageable argument resolvers that delegate base pagination parsing to Spring Data.
 */
@AutoConfiguration
public class PageableArgumentResolverAutoConfig {

    @Bean
    public WebQueryEntityAwarePageableArgumentResolver entityAwarePageableArgumentResolver(PageableHandlerMethodArgumentResolver delegate) {
        return new WebQueryEntityAwarePageableArgumentResolver(delegate);
    }

    @Bean
    public WebQueryDTOAwarePageableArgumentResolver dtoAwarePageableArgumentResolver(PageableHandlerMethodArgumentResolver delegate) {
        return new WebQueryDTOAwarePageableArgumentResolver(delegate);
    }
}
