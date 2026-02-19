package in.co.akshitbansal.springwebquery.config;

import in.co.akshitbansal.springwebquery.RestrictedPageableArgumentResolver;
import in.co.akshitbansal.springwebquery.RsqlCustomOperatorsConfigurer;
import in.co.akshitbansal.springwebquery.RsqlSpecificationArgumentResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {
        PaginationCustomizationAutoConfig.class,
        RestrictedPageableAutoConfig.class,
        RsqlAutoConfig.class,
        RsqlSpecResolverAutoConfig.class
})
@TestPropertySource(properties = "api.pagination.max-page-size=50")
class AutoConfigIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private PaginationCustomizationAutoConfig paginationConfig;

    @Autowired
    private RestrictedPageableAutoConfig restrictedPageableConfig;

    @Autowired
    private RsqlSpecResolverAutoConfig rsqlSpecResolverConfig;

    @Test
    void testBeansAreRegistered() {
        assertNotNull(context.getBean(PageableHandlerMethodArgumentResolverCustomizer.class));
        assertNotNull(context.getBean(RestrictedPageableArgumentResolver.class));
        assertNotNull(context.getBean(RsqlSpecificationArgumentResolver.class));
        assertNotNull(context.getBean(RsqlCustomOperatorsConfigurer.class));
    }

    @Test
    void testRestrictedPageableConfigAddsResolver() {
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
        restrictedPageableConfig.addArgumentResolvers(resolvers);
        assertTrue(resolvers.stream().anyMatch(r -> r instanceof RestrictedPageableArgumentResolver));
    }

    @Test
    void testRsqlConfigAddsResolver() {
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
        rsqlSpecResolverConfig.addArgumentResolvers(resolvers);
        assertTrue(resolvers.stream().anyMatch(r -> r instanceof RsqlSpecificationArgumentResolver));
    }
}
