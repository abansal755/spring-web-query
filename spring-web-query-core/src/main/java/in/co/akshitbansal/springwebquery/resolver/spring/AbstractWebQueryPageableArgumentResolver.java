package in.co.akshitbansal.springwebquery.resolver.spring;

import in.co.akshitbansal.springwebquery.annotation.FieldMapping;
import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.exception.QueryException;
import in.co.akshitbansal.springwebquery.validator.SortableFieldValidator;
import in.co.akshitbansal.springwebquery.validator.Validator;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Base resolver for {@link Pageable} parameters participating in
 * {@link WebQuery}-aware sorting.
 *
 * <p>This class delegates standard page/size parsing to Spring's
 * {@link PageableHandlerMethodArgumentResolver} and lets subclasses validate
 * and remap sort properties against either entity or DTO metadata.</p>
 */
public abstract class AbstractWebQueryPageableArgumentResolver extends AbstractWebQueryResolver {

	/**
	 * Delegate used to parse raw pageable parameters from the request.
	 */
	protected final PageableHandlerMethodArgumentResolver delegate;

	/**
	 * Validator used to enforce {@code @Sortable} constraints on resolved sort fields.
	 */
	protected final Validator<SortableFieldValidator.SortableField> sortableFieldValidator;

	/**
	 * Creates a pageable resolver base that delegates raw pagination parsing to
	 * Spring and applies shared sort-field validation support.
	 *
	 * @param delegate Spring's pageable resolver used for base pagination parsing
	 */
	protected AbstractWebQueryPageableArgumentResolver(PageableHandlerMethodArgumentResolver delegate) {
		this.delegate = delegate;
		this.sortableFieldValidator = new SortableFieldValidator();
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return Pageable.class.isAssignableFrom(parameter.getParameterType())
				&& super.supportsParameter(parameter);
	}

	@Override
	public Pageable resolveArgument(
			MethodParameter parameter,
			@Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest,
			@Nullable WebDataBinderFactory binderFactory
	) {
		try {
			// Delegate parsing of page, size and sort parameters to Spring
			Pageable pageable = delegate.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
			// Resolve effective endpoint settings from the current method parameter
			QueryConfiguration queryConfig = getQueryConfiguration(parameter);
			// Perform pageable resolution and validation based on the extracted configuration
			return resolvePageable(pageable, queryConfig);
		}
		catch (QueryException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new QueryConfigurationException("Failed to resolve pageable argument", ex);
		}
	}

	/**
	 * Validates and remaps pageable sorting according to the effective query configuration.
	 *
	 * @param pageable pageable parsed from the request
	 * @param queryConfig effective query configuration derived from {@link WebQuery}
	 *
	 * @return pageable with validated and possibly remapped sort orders
	 */
	protected abstract Pageable resolvePageable(Pageable pageable, QueryConfiguration queryConfig);

	/**
	 * Extracts pageable-specific query metadata directly from the
	 * {@link WebQuery} annotation declared on the supplied controller method.
	 *
	 * <p>Unlike specification resolution, pageable handling does not consume
	 * operator policies or AST settings, so this configuration contains only
	 * the entity type, optional DTO type, and the declared field mappings
	 * retained for entity-aware sort validation and remapping.</p>
	 *
	 * @param parameter supported method parameter whose declaring method carries
	 * {@link WebQuery}
	 *
	 * @return effective configuration used by pageable resolvers for sort validation
	 */
	protected QueryConfiguration getQueryConfiguration(MethodParameter parameter) {
		// Only runs successfully if supportsParameter has already returned true
		// so we can safely assume the presence of a valid @WebQuery annotation here, thus no exception handling is necessary
		WebQuery webQueryAnnotation = getWebQueryAnnotation(parameter);
		return QueryConfiguration
				.builder()
				.entityClass(webQueryAnnotation.entityClass())
				.dtoClass(webQueryAnnotation.dtoClass())
				.fieldMappings(Collections.unmodifiableList(Arrays.asList(webQueryAnnotation.fieldMappings())))
				.build();
	}

	/**
	 * Effective pageable-specific query metadata extracted from a supported
	 * {@link WebQuery}-annotated controller method.
	 */
	@Getter
	@Builder
	@EqualsAndHashCode
	@ToString
	protected static class QueryConfiguration {

		/**
		 * Entity type that ultimately receives validated sort paths.
		 */
		private final Class<?> entityClass;

		/**
		 * Optional DTO type used to validate API-facing sort selectors.
		 */
		private final Class<?> dtoClass;

		/**
		 * Explicit field aliases declared on {@link WebQuery}, used only by
		 * entity-aware pageable resolution.
		 */
		private final List<FieldMapping> fieldMappings;
	}
}
