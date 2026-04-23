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

package in.co.akshitbansal.springwebquery.pathmapper;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.Striped;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import lombok.NonNull;
import org.jspecify.annotations.Nullable;

import java.text.MessageFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;

import static in.co.akshitbansal.springwebquery.pathmapper.CachedDTOToEntityPathMapper.CacheKey;
import static in.co.akshitbansal.springwebquery.pathmapper.DTOToEntityPathMapper.MappingResult;

public class DTOToEntityPathMapperFactory {

	@Nullable
	private final ConcurrentMap<CacheKey, MappingResult> successfulResolutions;

	@Nullable
	private final Cache<CacheKey, RuntimeException> failedResolutions;

	@Nullable
	private final Striped<Lock> stripedLock;

	private final boolean cachingEnabled;

	public DTOToEntityPathMapperFactory(int failedResolutionsMaxCapacity, int lockStripeCount) {
		try {
			this.successfulResolutions = new ConcurrentHashMap<>();
			this.failedResolutions = Caffeine
					.newBuilder()
					.maximumSize(failedResolutionsMaxCapacity)
					.build();
			this.stripedLock = Striped.lock(lockStripeCount);
			this.cachingEnabled = true;
		}
		catch (Exception ex) {
			throw new QueryConfigurationException(
					MessageFormat.format(
							"Failed to initialize DTO-aware field resolution cache: {0}", ex.getMessage()
					), ex
			);
		}
	}

	public DTOToEntityPathMapperFactory() {
		this.successfulResolutions = null;
		this.failedResolutions = null;
		this.stripedLock = null;
		this.cachingEnabled = false;
	}

	@SuppressWarnings({"DataFlowIssue", "NullAway"})
	public DTOToEntityPathMapper newMapper(@NonNull Class<?> entityClass, @NonNull Class<?> dtoClass) {
		if (cachingEnabled) {
			return new CachedDTOToEntityPathMapper(entityClass, dtoClass, successfulResolutions, failedResolutions, stripedLock);
		}
		return new DTOToEntityPathMapper(entityClass, dtoClass);
	}
}
