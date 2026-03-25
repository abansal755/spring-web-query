package in.co.akshitbansal.springwebquery.config;

import in.co.akshitbansal.springwebquery.resolver.WebQueryDTOAwarePageableArgumentResolver;
import in.co.akshitbansal.springwebquery.resolver.WebQueryEntityAwarePageableArgumentResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@AutoConfiguration
@EnableSpringDataWebSupport
@RequiredArgsConstructor
@Slf4j
public class WebQueryPageableArgumentResolverAutoConfig implements WebMvcConfigurer {

    private final WebQueryEntityAwarePageableArgumentResolver entityAwarePageableArgumentResolver;
    private final WebQueryDTOAwarePageableArgumentResolver dtoAwarePageableArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // Ensure these resolvers are checked before Spring Data's default Pageable resolver.
        // resolvers.addFirst(dtoAwarePageableArgumentResolver);
        // resolvers.addFirst(entityAwarePageableArgumentResolver);
        // addFirst() was added in Java 21, using add(0, ...) for compatibility with earlier versions.
        resolvers.add(0, dtoAwarePageableArgumentResolver);
        resolvers.add(0, entityAwarePageableArgumentResolver);
        log.info("Registered {} for handling @RestrictedPageable parameters", WebQueryEntityAwarePageableArgumentResolver.class.getSimpleName());
        log.info("Registered {} for handling @RestrictedPageable parameters", WebQueryDTOAwarePageableArgumentResolver.class.getSimpleName());
    }
}
