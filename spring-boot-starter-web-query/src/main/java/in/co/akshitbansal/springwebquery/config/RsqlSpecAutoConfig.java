package in.co.akshitbansal.springwebquery.config;

import in.co.akshitbansal.springwebquery.RsqlSpecificationArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@AutoConfiguration
@RequiredArgsConstructor
@ConditionalOnClass(RsqlAutoConfig.class)
public class RsqlSpecAutoConfig implements WebMvcConfigurer {

    private final RsqlSpecificationArgumentResolver rsqlSpecificationArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(rsqlSpecificationArgumentResolver);
    }
}
