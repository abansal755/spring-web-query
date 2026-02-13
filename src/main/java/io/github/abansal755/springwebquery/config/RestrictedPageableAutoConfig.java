package io.github.abansal755.springwebquery.config;

import io.github.abansal755.springwebquery.RestrictedPageableArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@AutoConfiguration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@RequiredArgsConstructor
public class RestrictedPageableAutoConfig implements WebMvcConfigurer {

    private final List<PageableHandlerMethodArgumentResolverCustomizer> customizers;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(restrictedPageableArgumentResolver());
    }

    @Bean
    public RestrictedPageableArgumentResolver restrictedPageableArgumentResolver() {
        PageableHandlerMethodArgumentResolver delegate = new PageableHandlerMethodArgumentResolver();
        for (PageableHandlerMethodArgumentResolverCustomizer customizer : customizers)
            customizer.customize(delegate);
        return new RestrictedPageableArgumentResolver(delegate);
    }
}
