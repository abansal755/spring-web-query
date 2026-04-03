package in.co.akshitbansal.springwebquery.config;

import in.co.akshitbansal.springwebquery.RSQLCustomOperatorsConfigurer;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryDTOAwarePageableArgumentResolver;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryDTOAwareSpecificationArgumentResolver;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryEntityAwarePageableArgumentResolver;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryEntityAwareSpecificationArgumentResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {
        PaginationCustomizationAutoConfig.class,
        WebQueryPageableArgumentResolverAutoConfig.class,
        WebQueryBeanAutoConfig.class,
        WebQuerySpecificationArgumentResolverAutoConfig.class
})
@TestPropertySource(properties = "spring-web-query.pagination.max-page-size=50")
class AutoConfigIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private PaginationCustomizationAutoConfig paginationConfig;

    @Autowired
    private WebQueryPageableArgumentResolverAutoConfig pageableResolverConfig;

    @Autowired
    private WebQuerySpecificationArgumentResolverAutoConfig specificationResolverConfig;

    @Test
    void beansAreRegistered() {
        assertNotNull(context.getBean(PageableHandlerMethodArgumentResolverCustomizer.class));
        assertNotNull(context.getBean(RSQLCustomOperatorsConfigurer.class));
        assertNotNull(context.getBean(WebQueryEntityAwarePageableArgumentResolver.class));
        assertNotNull(context.getBean(WebQueryDTOAwarePageableArgumentResolver.class));
        assertNotNull(context.getBean(WebQueryEntityAwareSpecificationArgumentResolver.class));
        assertNotNull(context.getBean(WebQueryDTOAwareSpecificationArgumentResolver.class));
    }

    @Test
    void defaultCustomOperatorsConfigurerReturnsEmptySet() {
        RSQLCustomOperatorsConfigurer configurer = context.getBean(RSQLCustomOperatorsConfigurer.class);
        assertTrue(configurer.getCustomOperators().isEmpty());
    }

    @Test
    void pageableConfigAddsEntityThenDtoResolvers() {
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
        pageableResolverConfig.addArgumentResolvers(resolvers);

        assertEquals(2, resolvers.size());
        assertInstanceOf(WebQueryEntityAwarePageableArgumentResolver.class, resolvers.get(0));
        assertInstanceOf(WebQueryDTOAwarePageableArgumentResolver.class, resolvers.get(1));
    }

    @Test
    void specificationConfigAddsEntityThenDtoResolvers() {
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
        specificationResolverConfig.addArgumentResolvers(resolvers);

        assertEquals(2, resolvers.size());
        assertInstanceOf(WebQueryEntityAwareSpecificationArgumentResolver.class, resolvers.get(0));
        assertInstanceOf(WebQueryDTOAwareSpecificationArgumentResolver.class, resolvers.get(1));
    }

    @Test
    void maxPageSizeCustomizerCapsResolvedPageSize() throws Exception {
        PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
        paginationConfig.maxPageSizeCustomizer().customize(resolver);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("size", "500");
        Method method = TestController.class.getDeclaredMethod("search", Pageable.class);

        Pageable pageable = resolver.resolveArgument(
                new MethodParameter(method, 0),
                null,
                new ServletWebRequest(request),
                null
        );

        assertEquals(50, pageable.getPageSize());
    }

    @Test
    void paginationConfigRejectsNonPositiveMaxPageSize() {
        assertThrows(QueryConfigurationException.class, () -> new PaginationCustomizationAutoConfig(0));
        assertThrows(QueryConfigurationException.class, () -> new PaginationCustomizationAutoConfig(-1));
    }

    @Test
    void webQueryBeanConfigRejectsNegativeMaxAstDepth() {
        assertThrows(QueryConfigurationException.class, () -> new WebQueryBeanAutoConfig("filter", false, true, -1));
    }

    private static class TestController {
        @SuppressWarnings("unused")
        void search(Pageable pageable) {
        }
    }
}
