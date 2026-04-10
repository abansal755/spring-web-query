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

	/**
	 * Entity-aware specification resolver registered with Spring MVC.
	 */
	private final WebQueryEntityAwareSpecificationArgumentResolver entityAwareSpecResolver;

	/**
	 * DTO-aware specification resolver registered with Spring MVC.
	 */
	private final WebQueryDTOAwareSpecificationArgumentResolver dtoAwareSpecResolver;

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(entityAwareSpecResolver);
		resolvers.add(dtoAwareSpecResolver);
		log.info("Registered {} for handling @WebQuery Specification parameters", WebQueryEntityAwareSpecificationArgumentResolver.class.getSimpleName());
		log.info("Registered {} for handling @WebQuery Specification parameters", WebQueryDTOAwareSpecificationArgumentResolver.class.getSimpleName());
	}
}
