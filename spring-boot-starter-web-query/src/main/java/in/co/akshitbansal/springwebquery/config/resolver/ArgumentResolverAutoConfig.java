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

import cz.jirutka.rsql.parser.RSQLParser;
import in.co.akshitbansal.springwebquery.SpringWebQueryProperties;
import in.co.akshitbansal.springwebquery.ast.ValidationRSQLVisitorFactory;
import in.co.akshitbansal.springwebquery.resolver.field.FieldResolverFactory;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQueryPageableArgumentResolver;
import in.co.akshitbansal.springwebquery.resolver.spring.WebQuerySpecificationArgumentResolver;
import in.co.akshitbansal.springwebquery.validator.FieldMappingsValidator;
import in.co.akshitbansal.springwebquery.validator.QueryParamNameValidator;
import in.co.akshitbansal.springwebquery.validator.SortableFieldValidator;
import io.github.perplexhub.rsql.RSQLCustomPredicate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

import java.util.List;

@AutoConfiguration
public class ArgumentResolverAutoConfig {

	@Bean
	@ConditionalOnMissingBean
	public WebQuerySpecificationArgumentResolver webQuerySpecificationArgumentResolver(
			SpringWebQueryProperties properties,
			RSQLParser rsqlParser,
			List<RSQLCustomPredicate<?>> customPredicates,
			QueryParamNameValidator queryParamNameValidator,
			ValidationRSQLVisitorFactory validationRSQLVisitorFactory,
			FieldMappingsValidator fieldMappingsValidator
	) {
		return new WebQuerySpecificationArgumentResolver(
				properties.getGlobalFilterParamName(),
				properties.isGlobalAllowAndOperation(),
				properties.isGlobalAllowOrOperation(),
				properties.getGlobalMaxASTDepth(),
				rsqlParser,
				customPredicates,
				queryParamNameValidator,
				validationRSQLVisitorFactory,
				fieldMappingsValidator
		);
	}

	@Bean
	@ConditionalOnMissingBean
	public WebQueryPageableArgumentResolver webQueryPageableArgumentResolver(
			PageableHandlerMethodArgumentResolver delegate,
			SortableFieldValidator sortableFieldValidator,
			FieldResolverFactory fieldResolverFactory,
			FieldMappingsValidator fieldMappingsValidator
	) {
		return new WebQueryPageableArgumentResolver(
				delegate,
				sortableFieldValidator,
				fieldResolverFactory,
				fieldMappingsValidator
		);
	}
}
