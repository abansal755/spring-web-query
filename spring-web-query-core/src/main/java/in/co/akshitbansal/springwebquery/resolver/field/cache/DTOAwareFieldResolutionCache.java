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

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import in.co.akshitbansal.springwebquery.resolver.field.ResolutionResult;
import lombok.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Shared cache for DTO-aware field-resolution outcomes.
 *
 * <p>Successful resolutions are retained without eviction, while failed
 * resolutions are stored in a bounded LRU map to avoid unbounded memory growth
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
	private final ConcurrentMap<CacheKey, RuntimeException> failedResolutions;

	/**
	 * Per-key locks used to serialize cache population for the same selector.
	 */
	private final ConcurrentMap<CacheKey, Lock> keyLocks;

	/**
	 * Creates the shared cache used by cached DTO-aware field resolvers.
	 *
	 * @param failedResolutionsMaxCapacity maximum number of failed resolutions to
	 * retain
	 */
	public DTOAwareFieldResolutionCache(int failedResolutionsMaxCapacity) {
		if (failedResolutionsMaxCapacity <= 0)
			throw new IllegalArgumentException("Failed resolutions max capacity must be a positive integer");

		this.successfulResolutions = new ConcurrentHashMap<>();
		this.failedResolutions = new ConcurrentLinkedHashMap.Builder<CacheKey, RuntimeException>()
				.maximumWeightedCapacity(failedResolutionsMaxCapacity)
				.build();
		this.keyLocks = new ConcurrentHashMap<>();
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
		RuntimeException ex = failedResolutions.get(cacheKey);
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
	 * Returns the lock used to coordinate cache population for the supplied key.
	 *
	 * @param cacheKey composite key identifying the query contract and DTO path
	 *
	 * @return per-key lock for cache population
	 */
	public Lock getLock(@NonNull CacheKey cacheKey) {
		return keyLocks.computeIfAbsent(cacheKey, ignored -> new ReentrantLock());
	}

	/**
	 * Removes the coordination lock for the supplied key after population has
	 * completed.
	 *
	 * @param cacheKey composite key identifying the query contract and DTO path
	 */
	public void removeLock(@NonNull CacheKey cacheKey) {
		keyLocks.remove(cacheKey);
	}
}
