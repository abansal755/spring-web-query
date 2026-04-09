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

import io.github.perplexhub.rsql.RSQLJPAAutoConfiguration;
import io.github.perplexhub.rsql.RSQLJPASupport;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.AutoConfiguration;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * Registers RSQL JPA converters required by the starter.
 */
@AutoConfiguration(after = RSQLJPAAutoConfiguration.class)
public class RSQLJPAConverterRegistrationAutoConfig {

	// Allows RSQL to parse ISO-8601 Timestamp fields
	// eg. 2025-12-08T00:00:00+00:00 or 2025-12-08T00:00:00Z
	// query eg. to query by createTimestamp on 2025-12-08 in UTC
	// use createTimestamp>=2025-12-08T00:00:00%2B00:00;createTimestamp<2025-12-09T00:00:00%2B00:00
	// %2B is URL encoding for +
	@PostConstruct
	public void init() {
		RSQLJPASupport.addConverter(
				Timestamp.class, value -> {
					OffsetDateTime odt = OffsetDateTime.parse(value);
					Instant instant = odt.toInstant();
					return Timestamp.from(instant);
				}
		);
	}
}
