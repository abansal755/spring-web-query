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

package in.co.akshitbansal.springwebquery.config.resolver;

import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryPageableArgumentResolver;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQuerySpecificationArgumentResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Registers the merged WebQuery argument resolvers with Spring MVC.
 */
@AutoConfiguration
@Slf4j
@RequiredArgsConstructor
public class ArgumentResolverRegistrationAutoConfig implements WebMvcConfigurer {

	/**
	 * Resolver for {@code Specification} parameters.
	 */
	private final WebQuerySpecificationArgumentResolver webQuerySpecificationArgumentResolver;

	/**
	 * Resolver for {@code Pageable} parameters.
	 */
	private final WebQueryPageableArgumentResolver webQueryPageableArgumentResolver;

	/**
	 * Adds the WebQuery resolvers ahead of Spring MVC's default pageable
	 * resolver so merged query handling takes precedence.
	 *
	 * @param resolvers registered handler method argument resolvers
	 */
	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(webQuerySpecificationArgumentResolver);
		log.info("Registered {} for handling @WebQuery Specification parameters", WebQuerySpecificationArgumentResolver.class.getSimpleName());
		// Ensure this resolver is checked in before Spring Data's default Pageable resolver.
		resolvers.add(0, webQueryPageableArgumentResolver);
		log.info("Registered {} for handling @WebQuery Pageable parameters", WebQueryPageableArgumentResolver.class.getSimpleName());
	}
}
