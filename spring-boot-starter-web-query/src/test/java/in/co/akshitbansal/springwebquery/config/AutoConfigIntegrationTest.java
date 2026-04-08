package in.co.akshitbansal.springwebquery.config;

import in.co.akshitbansal.springwebquery.config.pageable.PageableArgumentResolverAutoConfig;
import in.co.akshitbansal.springwebquery.config.pageable.PageableArgumentResolverRegistrationAutoConfig;
import in.co.akshitbansal.springwebquery.config.pageable.PaginationCustomizationAutoConfig;
import in.co.akshitbansal.springwebquery.config.specification.SpecificationArgumentResolverAutoConfig;
import in.co.akshitbansal.springwebquery.config.specification.SpecificationArgumentResolverRegistrationAutoConfig;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.operator.RSQLCustomOperator;
import in.co.akshitbansal.springwebquery.operator.RSQLDefaultOperator;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {
        PaginationCustomizationAutoConfig.class,
        PageableArgumentResolverAutoConfig.class,
        PageableArgumentResolverRegistrationAutoConfig.class,
        RSQLOperatorsAutoConfig.class,
        RSQLJPAAutoConfig.class,
        SpecificationArgumentResolverAutoConfig.class,
        SpecificationArgumentResolverRegistrationAutoConfig.class
})
@TestPropertySource(properties = "spring-web-query.pagination.max-page-size=50")
class AutoConfigIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer;

    @Autowired
    private PageableArgumentResolverRegistrationAutoConfig pageableResolverConfig;

    @Autowired
    private SpecificationArgumentResolverRegistrationAutoConfig specificationResolverConfig;

    @Test
    void beansAreRegistered() {
        assertNotNull(pageableCustomizer);
        assertNotNull(context.getBean(WebQueryEntityAwarePageableArgumentResolver.class));
        assertNotNull(context.getBean(WebQueryDTOAwarePageableArgumentResolver.class));
        assertNotNull(context.getBean(WebQueryEntityAwareSpecificationArgumentResolver.class));
        assertNotNull(context.getBean(WebQueryDTOAwareSpecificationArgumentResolver.class));
        assertNotNull(context.getBean("defaultOperatorSet"));
        assertNotNull(context.getBean("customOperatorSet"));
    }

    @Test
    void customOperatorSetDefaultsToEmpty() {
        @SuppressWarnings("unchecked")
        Set<RSQLCustomOperator<?>> customOperators =
                (Set<RSQLCustomOperator<?>>) context.getBean("customOperatorSet");

        assertTrue(customOperators.isEmpty());
    }

    @Test
    void defaultOperatorSetIsRegistered() {
        @SuppressWarnings("unchecked")
        Set<RSQLDefaultOperator> defaultOperators =
                (Set<RSQLDefaultOperator>) context.getBean("defaultOperatorSet");

        assertTrue(defaultOperators.contains(RSQLDefaultOperator.EQUAL));
        assertTrue(defaultOperators.contains(RSQLDefaultOperator.IN));
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
    void paginationCustomizerCapsResolvedPageSize() throws Exception {
        PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
        pageableCustomizer.customize(resolver);

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
    void paginationCustomizerUsesConfiguredFallbackPageSize() throws Exception {
        PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
        pageableCustomizer.customize(resolver);

        MockHttpServletRequest request = new MockHttpServletRequest();
        Method method = TestController.class.getDeclaredMethod("search", Pageable.class);

        Pageable pageable = resolver.resolveArgument(
                new MethodParameter(method, 0),
                null,
                new ServletWebRequest(request),
                null
        );

        assertEquals(20, pageable.getPageSize());
    }

    @Test
    void paginationConfigRejectsNonPositiveMaxPageSize() {
        PaginationCustomizationAutoConfig config = new PaginationCustomizationAutoConfig();

        assertThrows(QueryConfigurationException.class, () -> config.maxPageSizeCustomizer(0, 20));
        assertThrows(QueryConfigurationException.class, () -> config.maxPageSizeCustomizer(-1, 20));
    }

    @Test
    void paginationConfigRejectsInvalidDefaultPageSize() {
        PaginationCustomizationAutoConfig config = new PaginationCustomizationAutoConfig();

        assertThrows(QueryConfigurationException.class, () -> config.maxPageSizeCustomizer(50, 0));
        assertThrows(QueryConfigurationException.class, () -> config.maxPageSizeCustomizer(50, -1));
        assertThrows(QueryConfigurationException.class, () -> config.maxPageSizeCustomizer(50, 51));
    }

    @Test
    void specificationConfigRejectsNegativeMaxAstDepth() {
        assertThrows(QueryConfigurationException.class,
                () -> new SpecificationArgumentResolverAutoConfig("filter", false, true, -1));
    }

    private static class TestController {
        @SuppressWarnings("unused")
        void search(Pageable pageable) {
        }
    }
}
