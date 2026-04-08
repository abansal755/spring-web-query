/*
 * Copyright 2026-present Akshit Bansal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package in.co.akshitbansal.springwebquery.resolver.spring;

import in.co.akshitbansal.springwebquery.annotation.WebQuery;
import org.springframework.core.MethodParameter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.lang.reflect.Method;

/**
 * Common base for {@link org.springframework.web.method.support.HandlerMethodArgumentResolver}
 * implementations that participate in {@link WebQuery}-driven request parsing.
 *
 * <p>This class centralizes detection of controller method parameters whose
 * declaring method carries {@link WebQuery} and exposes a shared helper for
 * retrieving that annotation once support has been established.</p>
 */
public abstract class AbstractWebQueryResolver implements HandlerMethodArgumentResolver {

	/**
	 * Determines whether the supplied method parameter belongs to a controller
	 * method annotated with {@link WebQuery}.
	 *
	 * <p>This base implementation uses {@link #getWebQueryAnnotation(MethodParameter)}
	 * as the single source of truth for annotation lookup. Missing methods or
	 * missing annotations are treated as "not supported" rather than exceptional
	 * conditions so Spring MVC can continue evaluating other resolvers.</p>
	 *
	 * @param parameter method parameter under inspection
	 *
	 * @return {@code true} when the declaring method has {@link WebQuery},
	 * otherwise {@code false}
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		try {
			getWebQueryAnnotation(parameter);
			return true;
		}
		catch (RuntimeException e) {
			// Exceptions are expected when the annotation is missing or the parameter is malformed
			// so we catch and return false rather than propagating
			return false;
		}
	}

	/**
	 * Returns the {@link WebQuery} annotation declared on the supplied method parameter's
	 * controller method.
	 *
	 * <p>This helper centralizes annotation lookup for both support detection and
	 * downstream configuration resolution. Callers that need boolean support checks
	 * should use {@link #supportsParameter(MethodParameter)}; this method is for
	 * subclasses that expect a supported parameter and therefore treat missing
	 * method metadata as a programming error.</p>
	 *
	 * @param parameter method parameter whose declaring method is expected to carry
	 * {@link WebQuery}
	 *
	 * @return resolved {@link WebQuery} annotation
	 *
	 * @throws IllegalStateException if the parameter has no declaring method or the
	 * declaring method is not annotated with {@link WebQuery}
	 */
	protected WebQuery getWebQueryAnnotation(MethodParameter parameter) {
		Method method = parameter.getMethod();
		if (method == null) throw new IllegalStateException("MethodParameter does not have an associated method");
		WebQuery webQueryAnnotation = method.getAnnotation(WebQuery.class);
		if (webQueryAnnotation == null) throw new IllegalStateException("Method is not annotated with @WebQuery");
		return webQueryAnnotation;
	}
}
