package in.co.akshitbansal.springwebquery.config;

import in.co.akshitbansal.springwebquery.resolver.DtoRestrictedPageableArgumentResolver;
import in.co.akshitbansal.springwebquery.resolver.EntityRestrictedPageableArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@AutoConfiguration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@RequiredArgsConstructor
public class RestrictedPageableAutoConfig implements WebMvcConfigurer {

    private final EntityRestrictedPageableArgumentResolver entityRestrictedPageableArgumentResolver;
    private final DtoRestrictedPageableArgumentResolver dtoRestrictedPageableArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // Ensure these resolvers are checked before Spring Data's default Pageable resolver.
        resolvers.addFirst(dtoRestrictedPageableArgumentResolver);
        resolvers.addFirst(entityRestrictedPageableArgumentResolver);
    }
}
