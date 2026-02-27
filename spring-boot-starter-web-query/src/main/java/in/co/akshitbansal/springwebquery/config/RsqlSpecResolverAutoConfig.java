package in.co.akshitbansal.springwebquery.config;

import in.co.akshitbansal.springwebquery.resolver.DtoAwareRsqlSpecArgumentResolver;
import in.co.akshitbansal.springwebquery.resolver.EntityAwareRsqlSpecArgumentResolver;
import io.github.perplexhub.rsql.RSQLJPAAutoConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@AutoConfiguration
@RequiredArgsConstructor
@ConditionalOnClass(RSQLJPAAutoConfiguration.class)
@Slf4j
public class RsqlSpecResolverAutoConfig implements WebMvcConfigurer {

    private final EntityAwareRsqlSpecArgumentResolver entityAwareRsqlSpecArgumentResolver;
    private final DtoAwareRsqlSpecArgumentResolver dtoAwareRsqlSpecArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(entityAwareRsqlSpecArgumentResolver);
        resolvers.add(dtoAwareRsqlSpecArgumentResolver);
        log.info("Registered {} for handling @RsqlSpec parameters", EntityAwareRsqlSpecArgumentResolver.class.getSimpleName());
        log.info("Registered {} for handling @RsqlSpec parameters", DtoAwareRsqlSpecArgumentResolver.class.getSimpleName());
    }
}
