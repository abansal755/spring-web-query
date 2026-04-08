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

package in.co.akshitbansal.springwebquery.config.pageable;

import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryDTOAwarePageableArgumentResolver;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryEntityAwarePageableArgumentResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Registers WebQuery pageable resolvers ahead of Spring Data's default resolver.
 */
@AutoConfiguration
@RequiredArgsConstructor
@Slf4j
public class PageableArgumentResolverRegistrationAutoConfig implements WebMvcConfigurer {

	private final WebQueryEntityAwarePageableArgumentResolver entityAwarePageableArgumentResolver;
	private final WebQueryDTOAwarePageableArgumentResolver dtoAwarePageableArgumentResolver;

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		// Ensure these resolvers are checked before Spring Data's default Pageable resolver.
		resolvers.add(0, entityAwarePageableArgumentResolver);
		resolvers.add(1, dtoAwarePageableArgumentResolver);
		log.info("Registered {} for handling @WebQuery Pageable parameters", WebQueryEntityAwarePageableArgumentResolver.class.getSimpleName());
		log.info("Registered {} for handling @WebQuery Pageable parameters", WebQueryDTOAwarePageableArgumentResolver.class.getSimpleName());
	}
}
