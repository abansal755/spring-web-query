package in.co.akshitbansal.springwebquery.config;

import in.co.akshitbansal.springwebquery.resolver.WebQueryDtoAwarePageableArgumentResolver;
import in.co.akshitbansal.springwebquery.resolver.WebQueryEntityAwarePageableArgumentResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@AutoConfiguration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@RequiredArgsConstructor
@Slf4j
public class WebQueryPageableArgumentResolverAutoConfig implements WebMvcConfigurer {

    private final WebQueryEntityAwarePageableArgumentResolver entityAwarePageableArgumentResolver;
    private final WebQueryDtoAwarePageableArgumentResolver dtoAwarePageableArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // Ensure these resolvers are checked before Spring Data's default Pageable resolver.
        resolvers.addFirst(dtoAwarePageableArgumentResolver);
        resolvers.addFirst(entityAwarePageableArgumentResolver);
        log.info("Registered {} for handling @RestrictedPageable parameters", WebQueryEntityAwarePageableArgumentResolver.class.getSimpleName());
        log.info("Registered {} for handling @RestrictedPageable parameters", WebQueryDtoAwarePageableArgumentResolver.class.getSimpleName());
    }
}
