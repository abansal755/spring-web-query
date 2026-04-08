package in.co.akshitbansal.springwebquery.config.specification;

import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryDTOAwareSpecificationArgumentResolver;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryEntityAwareSpecificationArgumentResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Registers WebQuery specification resolvers with Spring MVC.
 */
@AutoConfiguration
@RequiredArgsConstructor
@Slf4j
public class SpecificationArgumentResolverRegistrationAutoConfig implements WebMvcConfigurer {

	private final WebQueryEntityAwareSpecificationArgumentResolver entityAwareSpecResolver;
	private final WebQueryDTOAwareSpecificationArgumentResolver dtoAwareSpecResolver;

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(entityAwareSpecResolver);
		resolvers.add(dtoAwareSpecResolver);
		log.info("Registered {} for handling @WebQuery Specification parameters", WebQueryEntityAwareSpecificationArgumentResolver.class.getSimpleName());
		log.info("Registered {} for handling @WebQuery Specification parameters", WebQueryDTOAwareSpecificationArgumentResolver.class.getSimpleName());
	}
}
