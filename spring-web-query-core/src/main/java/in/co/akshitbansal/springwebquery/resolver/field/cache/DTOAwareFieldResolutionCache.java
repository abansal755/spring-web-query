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

package in.co.akshitbansal.springwebquery.resolver.field.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.Striped;
import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.resolver.field.ResolutionResult;
import lombok.NonNull;
import org.jspecify.annotations.Nullable;

import java.text.MessageFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;

/**
 * Shared cache for DTO-aware field-resolution outcomes.
 *
 * <p>Successful resolutions are retained without eviction, while failed
 * resolutions are stored in a bounded cache to avoid unbounded memory growth
 * from repeated invalid selector lookups.</p>
 */
public class DTOAwareFieldResolutionCache {

	/**
	 * Cache of successfully resolved DTO selectors keyed by query contract.
	 */
	private final ConcurrentMap<CacheKey, ResolutionResult> successfulResolutions;

	/**
	 * Bounded cache of failed resolution attempts keyed by query contract.
	 */
	private final Cache<CacheKey, RuntimeException> failedResolutions;

	/**
	 * Striped lock registry used to coordinate concurrent cache population.
	 */
	private final Striped<Lock> stripedLock;


	/**
	 * Creates the shared cache used by cached DTO-aware field resolvers.
	 *
	 * @param failedResolutionsMaxCapacity maximum number of failed resolutions to
	 * retain
	 * @param lockStripeCount number of lock stripes used to coordinate cache
	 * population for selector keys
	 *
	 * @throws IllegalArgumentException if the failed-resolution cache capacity
	 * or lock stripe count is non-positive
	 */
	public DTOAwareFieldResolutionCache(int failedResolutionsMaxCapacity, int lockStripeCount) {
		try {
			this.successfulResolutions = new ConcurrentHashMap<>();
			this.failedResolutions = Caffeine
					.newBuilder()
					.maximumSize(failedResolutionsMaxCapacity)
					.build();
			this.stripedLock = Striped.lock(lockStripeCount);
		}
		catch (Exception ex) {
			throw new QueryConfigurationException(MessageFormat.format(
					"Failed to initialize DTO-aware field resolution cache: {0}", ex.getMessage()
			), ex);
		}
	}

	/**
	 * Returns a cached successful resolution result when present or rethrows a
	 * cached failure for the same key.
	 *
	 * @param cacheKey composite key identifying the query contract and DTO path
	 *
	 * @return cached successful resolution, or {@code null} when the key is not
	 * present
	 */
	@Nullable
	public ResolutionResult resolveFromCache(@NonNull CacheKey cacheKey) {
		ResolutionResult result = successfulResolutions.get(cacheKey);
		if (result != null) return result;
		RuntimeException ex = failedResolutions.getIfPresent(cacheKey);
		if (ex != null) throw ex;
		return null;
	}

	/**
	 * Stores a successful DTO-aware path-resolution result.
	 *
	 * @param cacheKey composite key identifying the query contract and DTO path
	 * @param result successful resolution result to cache
	 */
	public void putSuccessfulResolution(@NonNull CacheKey cacheKey, @NonNull ResolutionResult result) {
		successfulResolutions.put(cacheKey, result);
	}

	/**
	 * Stores a failed DTO-aware path-resolution attempt.
	 *
	 * @param cacheKey composite key identifying the query contract and DTO path
	 * @param ex exception raised while resolving the path
	 */
	public void putFailedResolution(@NonNull CacheKey cacheKey, @NonNull RuntimeException ex) {
		failedResolutions.put(cacheKey, ex);
	}

	/**
	 * Returns the striped lock associated with the supplied cache key.
	 *
	 * <p>The returned lock is selected by hashing the key into the configured
	 * stripe set so callers can coordinate cache population without allocating a
	 * dedicated lock per selector.</p>
	 *
	 * @param cacheKey composite key identifying the query contract and DTO path
	 *
	 * @return lock guarding cache population for the key's stripe
	 */
	public Lock getKeyLock(@NonNull CacheKey cacheKey) {
		return stripedLock.get(cacheKey);
	}
}
