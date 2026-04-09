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

package in.co.akshitbansal.springwebquery.config;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.Set;

/**
 * Publishes the shared {@link RSQLParser} used by specification resolvers.
 */
@AutoConfiguration
public class RSQLParserAutoConfig {

	/**
	 * Creates the shared RSQL parser configured with all allowed comparison operators.
	 *
	 * @param allowedOperatorSet comparison operators accepted by the parser
	 *
	 * @return configured RSQL parser
	 */
	@Bean
	public RSQLParser rsqlParser(Set<ComparisonOperator> allowedOperatorSet) {
		return new RSQLParser(allowedOperatorSet);
	}
}
