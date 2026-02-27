package in.co.akshitbansal.springwebquery.config;

import in.co.akshitbansal.springwebquery.RsqlCustomOperatorsConfigurer;
import in.co.akshitbansal.springwebquery.resolver.DtoAwareRestrictedPageableArgumentResolver;
import in.co.akshitbansal.springwebquery.resolver.DtoAwareRsqlSpecArgumentResolver;
import in.co.akshitbansal.springwebquery.resolver.EntityAwareRestrictedPageableArgumentResolver;
import in.co.akshitbansal.springwebquery.resolver.EntityAwareRsqlSpecArgumentResolver;
import in.co.akshitbansal.springwebquery.util.AnnotationUtil;
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

import static org.junit.jupiter.api.Assertions.*;

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
    void beansAreRegistered() {
        assertNotNull(context.getBean(PageableHandlerMethodArgumentResolverCustomizer.class));
        assertNotNull(context.getBean(RsqlCustomOperatorsConfigurer.class));
        assertNotNull(context.getBean(AnnotationUtil.class));
        assertNotNull(context.getBean(EntityAwareRestrictedPageableArgumentResolver.class));
        assertNotNull(context.getBean(DtoAwareRestrictedPageableArgumentResolver.class));
        assertNotNull(context.getBean(EntityAwareRsqlSpecArgumentResolver.class));
        assertNotNull(context.getBean(DtoAwareRsqlSpecArgumentResolver.class));
    }

    @Test
    void defaultCustomOperatorsConfigurerReturnsEmptySet() {
        RsqlCustomOperatorsConfigurer configurer = context.getBean(RsqlCustomOperatorsConfigurer.class);
        assertTrue(configurer.getCustomOperators().isEmpty());
    }

    @Test
    void restrictedPageableConfigAddsEntityThenDtoResolvers() {
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
        restrictedPageableConfig.addArgumentResolvers(resolvers);

        assertEquals(2, resolvers.size());
        assertInstanceOf(EntityAwareRestrictedPageableArgumentResolver.class, resolvers.get(0));
        assertInstanceOf(DtoAwareRestrictedPageableArgumentResolver.class, resolvers.get(1));
    }

    @Test
    void rsqlSpecConfigAddsEntityThenDtoResolvers() {
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
        rsqlSpecResolverConfig.addArgumentResolvers(resolvers);

        assertEquals(2, resolvers.size());
        assertInstanceOf(EntityAwareRsqlSpecArgumentResolver.class, resolvers.get(0));
        assertInstanceOf(DtoAwareRsqlSpecArgumentResolver.class, resolvers.get(1));
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

    private static class TestController {
        @SuppressWarnings("unused")
        void search(Pageable pageable) {
        }
    }
}
